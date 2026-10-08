# Detailed Implementation Plan: Microservices Architecture

This document outlines the step-by-step roadmap to build, secure, containerize, and deploy your complete microservices architecture, including Docker, Kubernetes, Redis, and CI/CD pipelines.

---

## Phase 1: Basic Microservices (Easiest)
*The most familiar part. We build standard Spring Boot REST APIs without worrying about complex networking yet.*

### 1.1 Employee Service (Port: 8081)
*   **Goal:** Manage employee data.
*   **Tasks:** Finish the CRUD API using standard Spring Boot. Keep it simple and running locally on port 8081.

### 1.2 Address Service (Port: 8082)
*   **Goal:** Manage address data.
*   **Tasks:** Create a brand new Spring Boot project. Implement simple Entity, Repository, and Controller for Addresses. Run on port 8082.

---

## Phase 2: Data & Caching (Easy-Medium)
*Connecting the services to real databases and caching mechanisms.*

### 2.1 MySQL Integration
*   **Goal:** Move away from in-memory (H2) databases.
*   **Tasks:** Install MySQL locally, update `application.properties` in both services to connect to real database schemas.

### 2.2 Redis Integration
*   **Goal:** Speed up read requests using caching.
*   **Tasks:** Install Redis locally. Add `spring-boot-starter-data-redis` to the services. Use `@EnableCaching` and `@Cacheable` on the GET endpoints.

---

## Phase 3: Service Discovery (Medium)
*Now we make the services aware of each other so they can communicate dynamically.*

### 3.1 Eureka Server (Port: 8761)
*   **Goal:** Create the central registry.
*   **Tasks:** Create a new Spring Boot app with `@EnableEurekaServer`.
### 3.2 Registering Clients
*   **Goal:** Connect Employee and Address services to Eureka.
*   **Tasks:** Add Eureka Client dependency to both services. Add `@EnableDiscoveryClient` and configure them to point to `localhost:8761`.

### 3.3 Inter-Service Communication & Load Balancing
*   **Goal:** Allow Employee Service to dynamically fetch data from the Address Service.
*   **Tasks:** Create a `@LoadBalanced RestTemplate`. Create `AddressResponseDto` and `EmployeeWithAddressDto` to map JSON data. Update `EmployeeService` to call `http://ADDRESS/v1/address/{id}` to demonstrate Eureka name resolution and round-robin load balancing.

---

## Phase 4: API Gateway & Security (Hard)
*Securing the network and creating a single entry point. Spring Security can be tricky, making this harder.*

### 4.1 Auth Service & JWT (Port: 8083)
*   **Goal:** Handle logins and issue JSON Web Tokens.
*   **Tasks:** Create Auth Service. Configure Spring Security. Create `/login` endpoint that generates JWTs. Register with Eureka.

### 4.2 API Gateway (Port: 9090)
*   **Goal:** Route traffic and protect routes.
*   **Tasks:** Create Gateway service. Configure routing in `application.yml`. Implement a global filter that intercepts incoming requests, reads the JWT, and validates it with the Auth Service before letting traffic through to Employee/Address.

---

## Phase 5: Containerization (Harder)
*Moving away from running apps on your local machine to running them in isolated Docker containers.*

### 5.1 Docker & Docker Compose
*   **Goal:** Run the entire architecture with one command.
*   **Tasks:** Write a `Dockerfile` for all 5 Spring Boot apps. Write a `docker-compose.yml` that defines networks, environment variables, and brings up MySQL, Redis, Eureka, Gateway, and the Services together.

---

## Phase 6: CI/CD Pipeline (Advanced)
*Automating the boring stuff.*

### 6.1 GitHub Actions
*   **Goal:** Automate testing and Docker image creation.
*   **Tasks:** Write a YAML workflow that automatically triggers on `git push`. It should run `mvn test`, build the jars, build the Docker images, and push them to DockerHub automatically.

---

## Phase 7: Kubernetes Orchestration (Hardest)
*Enterprise-grade scaling and networking. The steepest learning curve.*

### 7.1 KIND / Minikube
*   **Goal:** Deploy the containers into a Kubernetes cluster and manage advanced networking.
*   **Tasks:** Install KIND. Write Kubernetes `.yaml` manifests (Deployments, Services, ConfigMaps, Secrets, Ingress). Apply them to the cluster. Configure native Kubernetes Load Balancing (ClusterIP/Ingress) to distribute traffic across pods, and manage Horizontal Pod Autoscaling (HPA) to set dynamic server limits.

---

## Phase 8: Advanced Spring Cloud Concepts (Optional)
*Refining the architecture with industry-standard resilience and cleaner code.*

### 8.1 Declarative REST Clients (OpenFeign)
*   **Why we use it:** `RestTemplate` requires writing bulky, repetitive HTTP boilerplate code. OpenFeign allows us to define HTTP calls using simple, clean Java interfaces.
*   **Goal:** Replace the manual `RestTemplate` logic with cleaner interfaces.
*   **Tasks:** Add `spring-cloud-starter-openfeign`. Create an interface annotated with `@FeignClient` to automatically handle HTTP calls.
*   **⚠️ K8s Collision & Resolution:** OpenFeign natively relies on Eureka to resolve IPs. Since we disabled Eureka in our Cloud-Native mode, we will use `@FeignClient(name="address", url="${address.service.url}")` and inject the native K8s DNS URL via our `02-configmap.yaml`.

### 8.2 Resilience & Fault Tolerance (Circuit Breakers)
*   **Why we use it:** If the Address Service goes down, the Employee Service will hang while waiting for a response, eventually crashing itself. Circuit breakers "trip" the connection and provide fallback data instantly to save the system.
*   **Goal:** Prevent cascading failures when a microservice is down or slow.
*   **Tasks:** Implement `Resilience4j`. Wrap the inter-service calls with `@CircuitBreaker` and `@Retry` to provide default "fallback" data instead of hanging requests.
*   *(No K8s collision here; this runs purely inside the JVM).*

### 8.3 Asynchronous Processing & Multithreading
*   **Why we use it:** Processing heavy background tasks synchronously blocks the main HTTP thread, slowing down user response times. Async processing offloads this to background threads.
*   **Goal:** Improve application performance by running non-blocking background tasks.
*   **Tasks:** Enable `@EnableAsync` in Spring Boot. Implement `@Async` methods. Configure a `ThreadPoolTaskExecutor`.
*   **⚠️ K8s Collision & Resolution:** Creating too many async background threads consumes extra RAM. This might cause the Pod to exceed its Kubernetes `limits: memory: "384Mi"`, resulting in a fatal `OOMKilled` crash. We will need to monitor thread creation and potentially increase the memory limit in `employee-service.yaml`.

---

## Phase 9: The Spring Ecosystem Horizon (Future Exploration)
*Massive scale, asynchronous processing, and enterprise security.*

### 9.1 Event-Driven Architecture (Kafka / RabbitMQ)
*   **Why we use it:** Synchronous REST calls tightly couple services together. Kafka allows Service A to shout "Employee Created!" into a message queue, and Service B can process it later whenever it has free time.
*   **Goal:** Move from synchronous REST calls to asynchronous message queues.
*   **Tasks:** Install Apache Kafka. Implement Producers and Consumers to publish and subscribe to events.
*   **⚠️ K8s Collision & Resolution:** Kafka is a stateful application. We will need to write advanced K8s `StatefulSet` and `Service` manifests to deploy Kafka and Zookeeper inside our cluster before the Java apps can connect to it.

### 9.2 Advanced Spring Security (OAuth2 / OIDC)
*   **Goal:** Implement enterprise-grade Single Sign-On (SSO).
*   **Tasks:** Replace the custom JWT login with an OAuth2 Provider (like Keycloak or Okta) to support "Login with Google" and complex Role-Based Access Control (RBAC).

### 9.3 Reactive Programming (Spring WebFlux)
*   **Goal:** Maximize server throughput using non-blocking I/O.
*   **Tasks:** Rewrite a service using Spring WebFlux and Project Reactor to handle thousands of concurrent connections without exhausting standard Tomcat threads.

### 9.4 Spring Batch
*   **Goal:** Process massive amounts of data in the background.
*   **Tasks:** Write a scheduled batch job to process or export a million employee records offline using automated chunks, readers, and writers.

### 9.5 API Documentation (Swagger / OpenAPI)
*   **Goal:** Automatically generate interactive API documentation.
*   **Tasks:** Integrate `springdoc-openapi-starter-webmvc-ui` to automatically generate Swagger UI for all microservices, allowing easy testing and exploration of the REST endpoints without Postman.
