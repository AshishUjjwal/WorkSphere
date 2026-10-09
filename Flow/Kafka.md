# 🚀 Apache Kafka & Event-Driven Architecture (V2 Flow)

## 1. Why Kafka? (Monolithic vs. Event-Driven)
In our `V1` flow, if the Employee Service wants to send an email, it calls the `NotificationService` directly (using a direct method call or OpenFeign).
* **The Problem (Tight Coupling):** What if the Notification Service is dead? What if it is overwhelmed? What if we later want to send an SMS too? We would have to modify the Employee Service code every time.
* **The Kafka Solution (Decoupling):** The Employee Service (Producer) just shouts into a microphone: *"An employee was created!"* It doesn't care who is listening. The Notification Service (Consumer) listens to that microphone. If the Consumer dies, the Producer is completely unaffected.

---

## 2. Core Kafka Architecture
* **Kafka Broker:** A single Kafka server (in our case, the Docker container running on port `9092`). Think of a **Real Estate Broker**: they don't buy or sell the house themselves. They just take the house from the Seller (Producer), hold onto it, and hand it to the Buyer (Consumer). The Kafka Broker simply takes the message, saves it safely to its hard drive, and waits for a Consumer to ask for it.
:- It acts like a giant, high-performance hard drive that stores messages (events) in log files.
* **Topic:** A category or folder where messages are stored. We created the `employee-created-events` topic.
* **Zookeeper & KRaft (The Boss):** Zookeeper is the "Manager" of the Kafka Brokers. It tracks which brokers are alive and manages the cluster metadata. 
  * **Crucial Architecture Detail:** Your Java Spring Boot applications **do not talk to Zookeeper**! Zookeeper is only found in our `docker-compose-kafka.yml` because it is strictly an infrastructure concern. Your Java apps (Producer/Consumer) only talk to the Kafka Broker on port `9092`.
  * **KRaft (The Modern Replacement):** In modern Kafka (v3.3+), Zookeeper is being deleted entirely in favor of **KRaft** (Kafka Raft). KRaft moves the "Boss" logic directly *inside* the Kafka Broker itself, meaning DevOps teams only have to run one application instead of two! *(Note: We intentionally used Zookeeper in this project because 90% of large enterprise companies still use it in their legacy production systems today).*
* **Partitions:** Topics are split into partitions to allow multiple consumers to read them at the exact same time in parallel.

---

## 3. The Producer (Employee Microservice)
Our Employee service acts as a **Producer**. 
When `saveEmployeeV2` is called:
1. We save the Employee to MySQL.
2. We use Spring's `KafkaTemplate` to take the `EmployeeDto` Java object, automatically convert it into a JSON String (`JsonSerializer`), and publish it to the `employee-created-events` topic.
3. It immediately returns `201 Created` to the user. It does not wait for the email to be sent! (Fire and Forget).

---

## 4. The Consumer (Notification Microservice)
Our Notification service acts as a **Consumer**.
It contains a `@KafkaListener` that is constantly polling the `employee-created-events` topic.
* **Consumer Groups:** We assigned it to `groupId = "notification-group"`. If we ran 5 instances of the Notification Microservice, they would all join this group. Kafka guarantees that only **ONE** instance in the group will process a specific message (preventing the user from getting 5 duplicate emails).

---

## 5. 🛑 The Famous Deserialization Crash (`__TypeId__` Bug)
During our testing, we encountered the famous `RecordDeserializationException`. This is a highly sought-after interview scenario!

**What happened?**
1. The Producer (`Employee`) converted `EmployeeDto` to JSON. Spring secretly added a metadata header to the Kafka message: `__TypeId__: com.microservice.Employee.dto.EmployeeDto`.
2. The Consumer (`Notification`) received the JSON. It looked at the `__TypeId__` header and tried to find that exact package/class in its own codebase.
3. Because the Notification service stores its DTO in `com.microservice.Notification.dto.EmployeeDto`, it threw a `ClassNotFoundException` and crashed!

**How we fixed it:**
We told the Consumer to ignore the Producer's package headers and forcefully map the JSON into its own local class.
```properties
# application.properties (Notification Service)
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.microservice.Notification.dto.EmployeeDto
```

---

## 6. Guaranteed Delivery (The Magic Moment)
When the Notification service was crashing due to the Deserialization bug, we continued to hit "Send" in Postman. 
Normally, in a REST API (OpenFeign), those messages would be lost forever. 
But with Kafka, the messages were safely stored in the Broker's hard drive. The exact second we fixed the code and restarted the Notification Consumer, it instantly reconnected, read the backlog, and successfully processed all the missed emails! No data was lost.

---

## 7. Interview FAQ: Docker Configurations & Real-World Scaling

**Q: In our `docker-compose.yml`, what is `KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092`?**
**A:** This is how Kafka listens *inside its own house*. `0.0.0.0` tells the Kafka server to open its doors and accept connections from anywhere inside the Docker network on port 9092.

**Q: What is `KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092`?**
**A:** This is what Kafka tells your Java Apps to connect to! Without this, Kafka would reply with its internal Docker IP (like `172.17.0.3`), and your Java app on Windows would crash because it can't reach it. This forces Kafka to say, *"Hey Java app, my official address is localhost:9092."*

**Q: Why do we only put `KAFKA_CREATE_TOPICS: "employee-events:1:1"` on ONE Broker in a multi-broker setup?**
**A:** If you put it on all 3 brokers, they would all boot up and try to create the exact same topic simultaneously, causing collision errors. You only give the command to `Broker-1`. Zookeeper manages the rest and tells the other brokers it exists.

```yaml
# Example of a 3-Broker Architecture
version: '3'
services:
  zookeeper:
    image: wurstmeister/zookeeper
    ports:
      - "2181:2181"

  kafka-1:
    image: wurstmeister/kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      # Only kafka-1 gets the command to create the topic for the whole cluster!
      KAFKA_CREATE_TOPICS: "employee-created-events:3:3" #(topicName:no.OfPartitions:no.OfReplicas)

  kafka-2:
    image: wurstmeister/kafka
    ports:
      - "9093:9093"
    environment:
      KAFKA_BROKER_ID: 2
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9093
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9093
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181

  kafka-3:
    image: wurstmeister/kafka
    ports:
      - "9094:9094"
    environment:
      KAFKA_BROKER_ID: 3
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9094
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9094
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
```

**Q: What is the difference between Partitions and Replicas?**
**A:** **Partitions = Parallel Speed. Replicas = Safety (Backups).**

**Q: Why do we need Multiple Brokers (A Kafka Cluster)?**
**A:** Running multiple brokers solves two massive enterprise problems:
1. **High Availability (Surviving a Server Crash):** If you have 1 Broker and the server loses power, Kafka is dead. If you have 3 Brokers, Zookeeper instantly redirects traffic to the surviving Brokers. Because of *Replicas*, no data is lost and your customers never notice the crash.
2. **Handling Huge Data:** If you process trillions of messages like Netflix, a single server's hard drive and CPU will max out. Adding more Brokers allows you to distribute the Partitions across multiple physical computers, giving you infinite storage and compute power.

**Q: In a 3-Broker cluster, do 2 of the Brokers just sit idle waiting for the 1st one to die?**
**A:** **No! Kafka is Active-Active.** Zookeeper acts as a fair manager and assigns "Leadership" of partitions across all brokers.
* **Broker-1** is the *Leader* of Partition 1 (Handles all reads/writes for P1).
* **Broker-2** is the *Leader* of Partition 2 (Handles all reads/writes for P2).
* **Broker-3** is the *Leader* of Partition 3 (Handles all reads/writes for P3).
All 3 Brokers are doing active work simultaneously! In the background, they silently copy each other's data to maintain Replicas. If Broker-1 dies, Zookeeper promotes Broker-2 to become the new Leader of Partition 1.

**Q: Can you give a real-world example of Partitions vs Replicas?**
**A:** Imagine Swiggy/Zomato on New Year’s Eve getting 10,000 orders in a single second.
* **Scenario 1 (1 Partition, 1 Replica):** With 1 partition, Kafka forces the Consumer to read orders 1 by 1. The 10,000th order waits hours. If that single server catches on fire (1 Replica), all 10,000 orders are permanently deleted.
* **Scenario 2 (50 Partitions, 3 Replicas):** With 50 Partitions, Swiggy can spin up 50 instances of the Notification Service. All 50 read Kafka at the exact same time, clearing the queue in seconds. With 3 Replicas, if Broker 1 catches on fire, Zookeeper instantly routes Consumers to Broker 2 which has the exact same data. The system doesn't flinch, and no data is lost!

**Q: What is a Kafka Offset and a Consumer Group?**
**A:** Think of a Kafka Topic as a giant book, and every message is a page (Offset 0, Offset 1, Offset 2). As a Consumer reads, it tells Kafka *"I finished page 2"*, and Kafka saves a bookmark (Committed Offset). 
A **Consumer Group** (`spring.kafka.consumer.group-id`) acts like a team. If you run 5 instances of the Notification service with the exact same Group ID, Kafka ensures they share the work and no two instances read the exact same page.

**Q: What happens if Kafka loses the bookmark (Amnesia)?**
**A:** If a brand new Consumer Group connects, or the server was offline for so long that Kafka deleted the old bookmark, Kafka asks: *"Where do you want to start?"*
* `auto-offset-reset=earliest`: Start from Page 0. Process all old messages still sitting on the hard drive. (Great for critical data like Emails or Payments).
* `auto-offset-reset=latest`: Skip the old pages and only read brand new messages from this exact moment forward. (Great for live Dashboards or Stock Tickers).

**Q: Wait, if we use `earliest` (or if we crash before saving the bookmark), won't we send Duplicate Emails?**
**A:** **YES!** Kafka guarantees **"At-Least-Once"** delivery, meaning a message might be processed 2 or 3 times. This is why in distributed systems, Consumers must be designed to be **Idempotent** (safe to retry). You must code a check in your own database before taking action: *"Have I already emailed Employee ID #5? If yes, skip it."*
In a real enterprise system, you must design your Consumers to be Idempotent (a fancy word that means "safe to retry").
```java
// Inside your @KafkaListener
if (notificationDatabase.hasAlreadyEmailed(employeeDto.getEmail())) {
    System.out.println("Wait, we already emailed this guy! Skipping message.");
    return;
}

emailService.send(employeeDto.getEmail());
notificationDatabase.save("Emailed: " + employeeDto.getEmail());
```

**Q: Does Kafka send every message to all Consumer Instances?**
**A:** No! If it did, 3 instances would send the exact same email 3 times. Instead, Kafka routes messages based on **Partitions**.
Think of Partitions as lanes on a highway. The Producer drops 5 emails in Kafka, and Kafka puts them in different lanes (Round-Robin). 
The Kafka Boss (Group Coordinator) assigns exactly 1 Lane to 1 Instance. 
* Instance A gets Lane 1 (Reads Email 1, 4)
* Instance B gets Lane 2 (Reads Email 2, 5)
* Instance C gets Lane 3 (Reads Email 3)
Because the instances only read their assigned lanes, no emails are processed twice, and they all work in parallel!

**Q: How does Kubernetes attach the Group ID to the instances?**
**A:** Kubernetes actually has no idea what a Group ID is! The Group ID is permanently hardcoded inside your Java Spring Boot code (`@KafkaListener(groupId="...")` or `application.properties`).
When you deploy to Kubernetes:
1. Kubernetes spins up 5 identical Pods (clones of your app).
2. All 5 Java instances boot up and connect to the Kafka Broker over the network, saying *"Hi, I want to read messages. My name is `notification-group`"*.
3. Kafka's Boss (Group Coordinator) sees 5 connections all claiming the exact same name. It realizes they are on the same team, automatically registers them as a single Consumer Group, and splits the Partitions among them. 
Kubernetes simply clones the app, and Kafka mathematically handles grouping the clones!

**Q: What is the Golden Rule of Kafka Scaling?**
**A:** **`Number of Partitions >= Number of Consumer Instances`**
* **Partitions == Instances (3:3):** Perfect. Every instance gets exactly 1 partition. 100% efficiency.
* **Partitions > Instances (10:3):** Totally fine. Instances will just monitor a few extra partitions each.
* **Partitions < Instances (3:5):** **BAD ARCHITECTURE.** 3 instances will get 1 partition each. The remaining 2 instances will be locked out and sit completely idle doing zero work! (This is why Netflix creates topics with 50-100 partitions from Day 1).

---

## 8. Spring Boot Consumer Properties Explained

**`spring.kafka.bootstrap-servers=localhost:9092`**
Tells the Spring Boot application where the Kafka Broker is located so it can connect.

**`spring.kafka.consumer.group-id=notification-group`**
Assigns this consumer to a specific "team". If multiple instances run with the same group ID, Kafka automatically load-balances the messages between them.

**`spring.kafka.consumer.auto-offset-reset=earliest`**
If the consumer loses its bookmark (offset), this tells it to start reading from the very beginning of the topic (`earliest`) so no messages are missed.

**`spring.kafka.consumer.key-deserializer` & `value-deserializer`**
Kafka stores raw bytes. These tell Spring to translate the incoming byte array back into a Java `String` (for the key) and a `JSON` object (for the value).

**`spring.kafka.consumer.properties.spring.json.trusted.packages=*`**
A security measure. Spring Boot normally blocks incoming JSON to prevent hackers from executing malicious classes. `*` tells Spring we trust all incoming JSON packages.

**`spring.kafka.consumer.properties.spring.json.use.type.headers=false`**
**`spring.kafka.consumer.properties.spring.json.value.default.type=com.microservice.Notification.dto.EmployeeDto`**
This fixes the `__TypeId__` Deserialization crash! It tells Spring to ignore the package name sent by the Producer and forcefully map the raw JSON into our local Consumer's DTO class.

---

## 9. Top 20 Kafka Interview Questions & Answers

1. **What is Apache Kafka and why use it over REST API?**
   Kafka is a distributed event-streaming platform. Unlike REST (synchronous, tight-coupling), Kafka is asynchronous and decoupled. If the receiving service goes down, Kafka stores the messages safely until it comes back online.
2. **What are the core components of Kafka?**
   Producer (sends data), Broker (server storing data), Topic (category), Partition (lanes within a topic), Consumer (reads data), and Zookeeper/KRaft (manager).
3. **What is Zookeeper, and what is replacing it?**
   Zookeeper is the manager of the Kafka cluster that tracks broker health and topic metadata. In Kafka 3.3+, it is being completely replaced by **KRaft** (Kafka Raft), which moves the management logic directly inside the Kafka Broker to simplify architecture.
4. **Do Spring Boot applications communicate directly with Zookeeper?**
   No. Producers and Consumers only communicate with the Kafka Brokers on port `9092`. Zookeeper is strictly a backend infrastructure tool used by the Brokers to communicate with each other.
5. **What is a Partition?**
   A Partition is a division of a Topic. It allows multiple consumers to read from the exact same topic at the exact same time in parallel, providing massive scalability.
6. **What is a Replica?**
   A Replica is a backup copy of a Partition stored on a different Broker. It provides High Availability and fault tolerance if a physical server crashes.
7. **What is the Golden Rule of Kafka Scaling?**
   The number of Partitions must be `>=` the number of Consumer Instances. If you have 3 partitions and 5 consumers, 2 consumers will sit completely idle.
8. **What is a Consumer Group?**
   A logical grouping of consumers (e.g., `notification-group`). Kafka guarantees that a single message is processed by only ONE consumer within a specific group.
9. **How does Kafka assign Partitions to Consumers?**
   The Group Coordinator assigns exactly 1 Partition to 1 Consumer within a group. A partition cannot be shared by two consumers in the same group.
10. **What is a Kafka Offset?**
    An offset is a unique, sequential ID number assigned to each message in a partition (e.g., 0, 1, 2). It acts as a bookmark so the consumer knows where it left off.
11. **What happens if a Consumer crashes and loses its Offset (Amnesia)?**
    Kafka looks at the `auto-offset-reset` property. If set to `earliest`, it replays all old messages from Offset 0. If set to `latest`, it skips the past and only reads new messages.
12. **Does Kafka guarantee exactly-once delivery?**
    By default, Kafka guarantees "At-Least-Once" delivery. This means if a consumer crashes before committing its offset, it will process the same message again when it restarts.
13. **What is Idempotency in Consumers?**
    Because Kafka might deliver the same message twice, your Consumer code must be Idempotent (safe to retry). You should check your database (`have I processed this Employee ID?`) before executing the side effect.
14. **How does Kafka maintain High Availability (Active-Active)?**
    In a multi-broker setup, Zookeeper assigns "Leadership" for different partitions to different brokers. All brokers actively read/write data simultaneously while silently maintaining backup replicas of each other's partitions.
15. **What causes the `RecordDeserializationException` in Spring Kafka?**
    When Spring produces a JSON object, it automatically attaches a `__TypeId__` header with the fully qualified class name (e.g., `com.microservice.Employee.dto...`). If the consumer does not have that exact package path, it crashes.
16. **How do you fix the `__TypeId__` Deserialization crash?**
    Set `spring.json.use.type.headers=false` and provide a `value.default.type` property in the Consumer to force Spring to map the raw JSON into the local DTO.
17. **What is `KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092`?**
    It tells the Kafka Docker container to accept internal connections from any IP inside the Docker network.
18. **What is `KAFKA_ADVERTISED_LISTENERS`?**
    It is the address Kafka broadcasts to external Java applications. Without this, Kafka would give the Java app its internal Docker IP, and the Java app would crash trying to connect to it.
19. **How does Kafka guarantee message ordering?**
    Kafka ONLY guarantees ordering *within a single partition*. If you need strict ordering for a specific user, you must use a "Message Key" (like the User ID) so that all events for that user are mathematically routed to the exact same partition.
20. **Why would you use Kafka over RabbitMQ?**
    RabbitMQ deletes messages as soon as they are consumed (Smart Broker, Dumb Consumer). Kafka stores messages permanently on disk for a configured retention period, allowing you to replay history and "time travel" (Dumb Broker, Smart Consumer).

---

## 10. Kafka vs REST API (When to use which?)

### Use REST API (OpenFeign / Synchronous) when:
* **You need an immediate response:** The user is waiting on the screen for data (e.g., "Give me the Employee's Profile details").
* **Strict Dependency:** Service A *cannot* continue until Service B replies (e.g., Service A asks the Payment Service to process a credit card before saving the order).
* **Data Retrieval:** You are fetching data (GET requests).

### Use Apache Kafka (Asynchronous / Event-Driven) when:
* **Fire and Forget:** Service A wants to announce something happened, but doesn't care who listens or how long it takes (e.g., "Employee Created!").
* **High Traffic / Scalability:** You have millions of requests and need to put them in a queue so downstream services don't crash from the sudden load.
* **Fault Tolerance:** If Service B goes down, you don't want Service A to fail. Kafka will hold the message safely until Service B wakes back up.
* **Multiple Listeners:** One event needs to trigger 5 different services (e.g., Send Email, Update Search DB, Send SMS, Generate Invoice). Instead of 5 slow REST calls, you send 1 message to Kafka, and all 5 services read it in parallel.