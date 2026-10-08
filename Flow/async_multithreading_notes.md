# Asynchronous Processing & Multithreading (`@Async`)

In Spring Boot, standard HTTP requests are **Synchronous**. If an API needs to send a welcome email that takes 3 seconds, the user will be staring at a loading screen for 3 seconds. 

By using `@Async`, we offload the email task to a background thread. The API instantly returns a `200 OK` to the user in 0.01 seconds, while the server silently sends the email in the background.

---

## 1. The Proxy Trap: Why a separate `NotificationService`?

**Interview Question:** *"Why did you create a separate `NotificationService` class? Why not just put the `@Async` method directly inside `EmployeeService` where it is being called?"*

* **Answer:** Because of how Spring's AOP (Aspect Oriented Programming) Proxy works! Spring can only intercept method calls that come from **outside** the class. If `saveEmployee()` calls `sendEmail()` inside the exact same class, it bypasses the proxy. The `@Async` annotation is completely ignored, and the method will run synchronously, defeating the entire purpose! It **must** be placed in a separate class.

---

## 2. Tuning the Thread Pool (The Restaurant Kitchen Analogy)

We created an `AsyncConfig.java` to define a `ThreadPoolTaskExecutor`. If we don't do this, Spring creates an infinite number of threads which will quickly consume all server RAM and crash the Kubernetes Pod (OOMKilled).

We configured three specific numbers. They operate in a strict order:

1. **`CorePoolSize (2)` -> The Full-Time Chefs:** 
   This is the baseline number of threads kept alive. If 2 tasks come in, Chef 1 and Chef 2 take them immediately.
2. **`QueueCapacity (50)` -> The Ticket Rail (Waiting Room):**
   If a 3rd task comes in while the Core Chefs are busy, Spring **does not** create a 3rd thread yet. It puts the task in the Queue. The tasks wait here until a core thread frees up.
3. **`MaxPoolSize (5)` -> The Emergency Backup Chefs:**
   If there is a massive traffic spike and **all 50 spots in the queue are full**, Spring panics. It will now start spinning up new threads, up to the maximum limit of 5. Now you have 5 threads aggressively clearing the queue.

---

## 3. Global vs Per-User Thread Pool

**Question:** *"Are these 5 threads created per-user?"*
* **Answer:** **NO. The thread pool is GLOBAL.** It is shared across the entire application for all users. If 1,000 users hit the API at the same time, they all share those same 5 threads and 50 queue slots.
* **Why?** A Java thread takes ~1MB of RAM. If we created 5 threads per user, 1,000 users would consume 5,000 threads (5GB of RAM instantly). By keeping it global, we protect the server from crashing during traffic spikes. The users just have to wait in the queue.

---

## 4. The Absolute Limit of `@Async` vs Apache Kafka

**Question:** *"If the Queue only holds 50, what happens if 5,000 users sign up at the exact same millisecond?"*
* **Answer:** 5 users get assigned to threads. 50 users fill up the Queue. **The 56th user is instantly REJECTED.** Java throws a `TaskRejectedException`, the background task crashes, and the 56th user's email is **lost forever in the void.**

### When to use `@Async` vs Kafka:

1. **Small/Medium Business (Standard Traffic):** 
   If you expect normal traffic (e.g., an internal HR system), `@Async` is perfect. It is lightweight, fast, and costs $0 in extra architecture. Millions of apps run purely on `@Async`.
2. **Massive Enterprise (Uber, Netflix, Black Friday):**
   If you expect thousands of concurrent actions, storing pending tasks in Java RAM (the Queue) is dangerous. If the JVM crashes, all pending tasks in the Queue are deleted. 
   **Solution:** You use **Apache Kafka** (an Event-Driven Architecture). Instead of a Java RAM queue, tasks are sent as messages to a dedicated Kafka server where they are safely saved on a permanent Hard Drive. The Java server slowly pulls messages off the hard drive at a safe speed. If the Java server crashes, 0 tasks are lost!

---

## 5. Thread Starvation & The `@TimeLimiter` Kill Switch

**Question:** *"What if a background task takes too long, or gets stuck in an infinite loop while waiting for an external API?"*

This is the ultimate nightmare scenario called **Thread Starvation**. 
* If you have a `MaxPoolSize` of 5, and 5 background emails get stuck forever trying to reach a broken API, all 5 of your background threads are permanently frozen.
* With 0 threads available, all new tasks pile up in the Queue. Once the Queue hits 50, every single subsequent background task in your entire application will be completely rejected (`TaskRejectedException`) until the server is manually rebooted!

**The Enterprise Solution (`@TimeLimiter`):**
To prevent Thread Starvation, you **must** enforce strict timeouts on background tasks. We implemented this using the **Resilience4j `@TimeLimiter`** combined with a `CompletableFuture`.

By adding this to `application.properties`:
```properties
resilience4j.timelimiter.instances.emailService.timeoutDuration=2s
resilience4j.timelimiter.instances.emailService.cancelRunningFuture=true
...we create a literal "Kill Switch". If the `@Async` method runs for longer than exactly 2 seconds, Resilience4j acts like a sniper. It sends a physical `Interrupt` signal to the stuck thread. This forcefully crashes the hanging process, stops the task, and safely returns the clean thread back to the Pool so it can serve the next user!

**Question:** *"How do I implement this Kill Switch if I am not using Resilience4j?"*
If you aren't using Resilience4j, you have to write it yourself in pure Java. When you trigger a background task, Java returns a `Future` object (a remote control for the thread). You must manually wait for it and call `future.cancel(true)` if it takes too long. *(Resilience4j is highly preferred because it handles this automatically on an invisible background thread without forcing your main code to wait).*

---

## 6. The "Main Thread" Hand-off

**Question:** *"Who is the 'Main Thread'?"*

In Spring Boot, the "Main Thread" is the **Tomcat HTTP Worker Thread**. 
When a user clicks "Submit" on a website, the request hits your server. Tomcat assigns a worker thread to handle that specific user. As long as this thread is busy, the user's browser displays a spinning loading wheel.

**How the Hand-off works:**
1. The Main Thread enters `EmployeeService.saveEmployee()`.
2. It hits the line: `notificationService.sendWelcomeEmail()`.
3. Because this method has `@Async`, the Main Thread does **NOT** execute it. Instead, it throws the data into the Background Queue, yells *"Hey Background Threads, send this email!"*, and instantly walks away.
4. The Main Thread returns `200 OK` to the user. The loading wheel stops. The user thinks the entire process finished in 0.01 seconds.
5. Completely disconnected from the user, a background thread (`AsyncThread-1`) wakes up, pulls the data from the Queue, and spends the next 3 seconds quietly sending the email.

---

## 7. Tuning the Timeout Duration

**Question:** *"If the TimeLimiter is set to 2 seconds, but sending an email normally takes 3 seconds, won't it just fail every time?"*

**Yes!** If you set the timeout too low, the Kill Switch will accidentally assassinate healthy tasks before they can finish. 
In a real production environment, you always set the timeout to be longer than the *expected* duration. 
* If an email normally takes **3 seconds** to send...
* You set your timeout limit to **10 seconds**.

You give the background thread enough time to do its job under normal circumstances. The Kill Switch is only there to save you if the email provider completely breaks and the task gets stuck hanging forever.
