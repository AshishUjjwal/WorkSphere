<h1 align="center">
  🏢 WorkSphere
</h1>

<p align="center">
  <em>A robust, scalable, and secure microservices-based application built with Spring Boot and Spring Cloud.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.4-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Cloud-2023.0.1-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
</p>

---

## 📖 Overview

**WorkSphere** is a distributed enterprise application designed to manage employee and address records efficiently. It employs a modern **Microservices Architecture** utilizing Spring Cloud Netflix Eureka for service discovery, a centralized API Gateway for routing and security, and JWT for secure authentication.

## 🏗️ Architecture

### 1. Spring Cloud Local Architecture
![Microservices Architecture](./Flow/Microservices%20Arch.png)

### 2. Kubernetes Cloud-Native Architecture
![Kubernetes Architecture](./Flow/Kubernetes%20Microservices%20Lifecycle%20and%20Runtime%20Flow.png)
The system consists of five distinct microservices communicating seamlessly:

1. **API Gateway** (Port: `9090`): The single entry point for all client requests. It handles intelligent routing, rate limiting, IP logging, and global JWT authentication.
2. **Eureka Server** (Port: `8761`): The service registry. All microservices register themselves here so the Gateway can dynamically locate them.
3. **Auth Service** (Port: `8083`): Handles user registration, login, JWT token generation, and token validation.
4. **Employee Service** (Port: `8081`): Manages the core business logic for employee data.
5. **Address Service** (Port: `8082`): Manages the business logic for employee addresses.

## 🛠️ Tech Stack

*   **Backend Framework:** Spring Boot
*   **Microservices/Routing:** Spring Cloud Gateway, Netflix Eureka
*   **Security:** Spring Security, JWT (JSON Web Tokens)
*   **Database:** MySQL, Spring Data JPA / Hibernate
*   **Java Version:** Java 17
*   **Build Tool:** Maven

## 🛡️ Security & API Gateway Features

The API Gateway is equipped with custom security filters to protect the backend microservices:
*   **RateLimitFilter:** Prevents spam and abuse by limiting requests per IP address (handles NGINX `X-Forwarded-For` headers safely).
*   **LoggingFilter:** Logs the true client IP address and requested paths for security audits.
*   **AuthenticationFilter:** Intercepts secure routes and internally validates the JWT token with the Auth Service before routing traffic.

## 🚀 Getting Started

### Prerequisites
*   JDK 17 or higher
*   Maven 3.6+
*   MySQL Server running on `localhost:3306`

### Database Setup
The application relies on three separate databases. Create them in your MySQL server:
```sql
CREATE DATABASE employee_db;
CREATE DATABASE address_db;
CREATE DATABASE auth_db;
```
*(Note: Hibernate `ddl-auto=update` is enabled, so tables will be created automatically on startup).*

### Running the Application

To ensure proper service discovery, start the microservices in the following order:

1.  **Start Eureka Server**
    ```bash
    cd EurekaServer
    mvnw spring-boot:run
    ```
2.  **Start Auth Service, Employee Service, and Address Service** (Order doesn't matter)
    ```bash
    cd AuthService && mvnw spring-boot:run
    cd Employee && mvnw spring-boot:run
    cd Address && mvnw spring-boot:run
    ```
3.  **Start API Gateway** (Start this last so it can register with Eureka)
    ```bash
    cd ApiGateway
    mvnw spring-boot:run
    ```

Check the Eureka Dashboard at `http://localhost:8761` to verify all services are registered (API-GATEWAY, AUTH-SERVICE, EMPLOYEE, ADDRESS).

## 📡 API Endpoints

All requests should be routed through the **API Gateway** on port `9090`.

### Public Endpoints (No Token Required)
*   **Register User:** `POST http://localhost:9090/auth/register`
*   **Login (Get Token):** `POST http://localhost:9090/auth/token`

### Protected Endpoints (Requires Bearer Token)
*   **Employee Endpoints:** `http://localhost:9090/employee/**`
*   **Address Endpoints:** `http://localhost:9090/address/**`

**Header Requirement:**
```http
Authorization: Bearer <your_jwt_token_here>
```

---
<div align="center">
  <i>Developed with ❤️ using Spring Boot Microservices</i>
</div>
