# Aser Trading Backend System

Multi-Branch Inventory, E-Commerce, and Corporate Website API built with **Spring Boot** (Java 21), **PostgreSQL**, **Redis**, and stateless **JWT Security**.

---

## Getting Started

### 1. Prerequisites
- **Java 21** (JDK 21 LTS)
- **Docker & Docker Compose** (for PostgreSQL and Redis)

---

### 2. Environment Configuration
Copy the provided `.env.example` template to `.env` in the project root:

```bash
# On Linux / macOS
cp .env.example .env

# On Windows (PowerShell)
Copy-Item .env.example .env
```

Open `.env` and fill in your environment-specific credentials:
- `POSTGRES_DB`: Name of the PostgreSQL database
- `POSTGRES_USER`: Database username
- `POSTGRES_PASSWORD`: Secure database password
- `JWT_SECRET`: A secure 256-bit or 512-bit hex key for HMAC-SHA256 token signing
- `PORT`: HTTP server port (defaults to `8085`)

> [!IMPORTANT]
> The `.env` file is excluded from git by `.gitignore` and must **never** be committed to version control.

---

### 3. Start Database and Cache Services
Launch PostgreSQL and Redis using Docker Compose:

```bash
docker compose up -d
```

Verify that the containers are healthy:

```bash
docker compose ps
```

---

### 4. Build and Run the Backend Application
Run the Spring Boot application locally:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

The backend server will start on `http://localhost:8085`.

---

### 5. Running Tests
Run the automated test suite (uses an isolated in-memory H2 database):

```bash
# Linux / macOS
./mvnw test

# Windows (PowerShell)
.\mvnw.cmd test
```

---

### 6. API Documentation
Once the server is running, explore the interactive OpenAPI Swagger UI at:
- `http://localhost:8085/swagger-ui.html`
- `http://localhost:8085/v3/api-docs`

