# The Transactional Boundary: `@Transactional` vs `@Async`

One of the most common and difficult Senior Architect interview questions is how Database Transactions behave when combined with Asynchronous Multithreading.

## The Scenario
Look at this code:
```java
@Transactional
public EmployeeDto saveEmployee(EmployeeDto dto) {
    // 1. Save to Database
    Employee savedEntity = repository.save(employeeEntity); 
    
    // 2. Trigger an @Async Background Task
    notificationService.sendWelcomeEmail(savedEntity.getName());
    
    return AppUtils.entityToDto(savedEntity); 
}
```
**Question:** *"If the `@Async` background task crashes and throws a `RuntimeException`, will the `@Transactional` annotation automatically rollback the Database save?"*

---

## The Answer: NO!
The database save will **NOT** be rolled back. The employee remains permanently saved in the database.

### Why? (The ThreadLocal Boundary)
Spring's `@Transactional` is strictly tied to the specific **Thread** that opened the transaction (using a concept called `ThreadLocal`). 

1. The **Main Thread** enters `saveEmployee()` and opens the Database Transaction.
2. The **Main Thread** saves the Employee to the database.
3. The **Main Thread** hits the `@Async` method, drops the data into the Background Queue, and instantly walks away. 
4. The **Main Thread** exits the method and **COMMITS** the transaction to MySQL.

By the time the Background Thread (`AsyncThread-1`) even starts trying to send the email, the Main Thread has already committed the transaction and closed the connection! Because the exception happens on a completely isolated thread, it does not bubble up to the Main Thread. The transaction is completely unaware of the crash, so no rollback occurs.

---

## What if we removed `@Async`?
If you removed the `@Async` annotation, the method would run **Synchronously** on the exact same Main Thread. 

In this case, if the email server crashed and threw an exception, that exception would bubble right back up the stack to `saveEmployee()`. Spring's `@Transactional` proxy on the Main Thread would catch the exception and instantly **ROLLBACK** the database save. The employee would be deleted.

By using `@Async`, we intentionally broke the Transactional Boundary. We are architecturally deciding: *"Saving the core data to the database is the primary mission. Even if a secondary task like an email fails, we still want to keep the primary employee data in the database."*
