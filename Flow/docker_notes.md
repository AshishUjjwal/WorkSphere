# Docker & CI/CD Pipeline Notes

This document summarizes the architecture and functionality of the CI/CD pipeline, `docker-compose.yml`, and `Dockerfile` in the microservices project, along with a detailed Q&A of common Docker/Maven concepts.

## 1. High-Level Overview

*   **`ci-cd.yml` (GitHub Actions):** Automates the building and deployment process. It uses a **Matrix Strategy** to loop through every service folder (e.g., `Address`, `Employee`) and uses the individual **`Dockerfile`** in each folder to build an image. It does **not** use `docker-compose.yml`.
*   **`docker-compose.yml`:** Used entirely for **local development**. It coordinates spinning up the entire stack of microservices simultaneously, configures networks (`employee-net`), and provisions databases (MySQL, Redis).
*   **`Dockerfile`:** The blueprint for creating a container image for a single, specific microservice.

---

## 2. Deep Dive: The Microservice `Dockerfile`

The microservices in this project use a **Multi-Stage Build**. This splits the build into two distinct parts to ensure the final image is extremely small, fast, and secure.

### Stage 1: The Build Stage
The goal of this stage is to compile the Java code into a `.jar` file.

```dockerfile
# Start with a heavy image containing Maven and JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Creates the /app folder and makes it the current directory
WORKDIR /app

# Copies pom.xml into the current directory (/app)
COPY pom.xml .

# Downloads all dependencies. 
# We do this BEFORE copying source code to take advantage of Docker Caching!
RUN mvn dependency:go-offline -B

# Copies the actual Java source code
COPY src ./src

# Compiles code and packages it into a "Fat JAR" in the /app/target/ folder
RUN mvn clean package -DskipTests
```

### Stage 2: The Run Stage
The goal of this stage is to discard the heavy Maven/JDK tools and create a tiny runtime environment.

```dockerfile
# Start a brand new, tiny image using Alpine Linux and only the Java Runtime Environment (JRE)
FROM eclipse-temurin:17-jre-alpine

# Creates and enters /app
WORKDIR /app

# Grabs the compiled .jar file from Stage 1 ("build") and renames it to app.jar
COPY --from=build /app/target/*.jar app.jar

# Defines the command that executes when the container is finally started
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 3. Q&A: Detailed Explanations of Specific Lines

**Q: Where are the directories being created? Is there a hidden `mkdir`?**
No explicit `mkdir` is needed! The command `WORKDIR /app` acts as both `mkdir -p /app` (if it doesn't exist) and `cd /app`. It creates the folder and moves inside it in one step.

**Q: Is there a hidden `.` (dot) in `COPY pom.xml .`?**
Yes, the `.` represents the **current working directory**. Since the previous line was `WORKDIR /app`, the dot tells Docker to copy `pom.xml` from your computer into the `/app` folder inside the container.

**Q: Why are there two `RUN` commands (one for `go-offline`, one for `clean package`)?**
This is a clever trick to use **Docker Layer Caching**. 
- Dependencies in `pom.xml` rarely change, but source code in `src/` changes constantly. 
- By copying `pom.xml` and downloading dependencies first, Docker caches that heavy download step. 
- When you update your Java code, Docker skips downloading dependencies and only re-runs the code compilation step, making builds much faster.

**Q: Does `mvn clean package` put both code and dependencies into the `.jar`?**
Yes. For Spring Boot apps, Maven creates a **"Fat JAR"** (or Uber JAR) that bundles your compiled code *and* all the dependencies (like Tomcat, Spring, DB drivers) into one single, massive `.jar` file. It saves this file in the `/app/target/` folder.

**Q: `RUN mvn clean package` doesn't mention the jar file name. How does Docker know what it is?**
Maven automatically generates the name based on your `pom.xml` file. It combines the `<artifactId>` and `<version>`. For example, it will name the file `Address-0.0.1-SNAPSHOT.jar`. 
Because the name can change, Docker uses a wildcard on line 13: `COPY --from=build /app/target/*.jar app.jar`. This tells Docker to find *any* file ending in `.jar` and rename it to `app.jar`.

**Q: What if there are multiple `.jar` files in the `target/` folder?**
If the wildcard `*.jar` finds more than one file, Docker's `COPY` command acts differently: it creates a **folder** named `app.jar/` and dumps all the files inside it. This would cause your container to crash on startup!
However, this is safe in Spring Boot because Maven renames the original "thin" jar to `Address-0.0.1-SNAPSHOT.jar.original`. Thus, only *one* file actually ends in `.jar`.

Address-0.0.1-SNAPSHOT.jar: This is the "Fat JAR" containing your code and all dependencies. (This is the one we want).

Address-0.0.1-SNAPSHOT.jar.original: This is the "Thin JAR" containing only your code and no dependencies. Notice how Maven automatically added .original to the end of the file name!

**Q: My local `target/` folder has a bunch of other folders in it (like `classes`, `maven-status`). How does Docker handle that?**
The `target` folder is Maven's "workspace" where it stores all intermediate work. The Docker wildcard `*.jar` acts as a strict filter. It completely ignores all those other folders and intermediate files, looking strictly for the single file that ends in `.jar`.

**Q: Why does the second `FROM` command not have an `AS <name>` alias?**
You only need an `AS` alias if you plan to reference that stage later in the file. Since `FROM eclipse-temurin:17-jre-alpine` is the final stage of the Dockerfile, nothing else needs to reference it, so the alias is unnecessary. Docker automatically saves this final stage as the resulting image.

**Q: What does `eclipse-temurin:17-jre-alpine` mean?**
*   `eclipse-temurin:17`: The provider and version of Java (Java 17).
*   `jre`: Java Runtime Environment. It is much smaller than the JDK (Java Development Kit) because it only contains tools to *run* code, not compile it.
*   `alpine`: A highly stripped-down, tiny version of Linux (about 5MB). Combining JRE and Alpine makes your final image incredibly small and secure.

**Q: What is the difference between `ENTRYPOINT` and `RUN`?**
`RUN` executes a command *while* the Docker image is being built (like installing a package). 
`ENTRYPOINT` defines the main command that executes only when the container is finally started (e.g., `docker run`). The container stays alive as long as this process is running.
