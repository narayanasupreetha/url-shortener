# 🔗 URL Shortener

A production-ready **URL Shortener** REST API built with **Spring Boot 3**. It lets authenticated users shorten long
URLs, manage their links, track click analytics, and resolve short codes with automatic redirect — all secured with *
*JWT authentication** and backed by **PostgreSQL**.

---

## 📑 Table of Contents

- [Project Overview](#-project-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture Overview](#-architecture-overview)
- [Prerequisites](#-prerequisites)
- [Setup & Installation](#-setup--installation)
- [Configuration](#-configuration)
- [Running the Application](#-running-the-application)
- [API Documentation](#-api-documentation)
- [API Endpoints & Examples](#-api-endpoints--examples)
    - [Authentication](#authentication)
    - [URL Management](#url-management)
    - [Redirect](#redirect)
    - [Admin](#admin)
- [Database Schema](#-database-schema)
- [Caching](#-caching)
- [Async Processing](#-async-processing)
- [Security](#-security)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)
- [Contact & Support](#-contact--support)

---

## 📖 Project Overview

The **URL Shortener** service converts any long URL into a compact 6-character alphanumeric short code (e.g.,
`http://localhost:8080/api/v1/aB3xYz`). Users register an account, log in to obtain a JWT token, then use the API to:

- Create short URLs (with optional expiry dates)
- List and manage their own short URLs (paginated)
- Deactivate links they no longer want active
- Resolve short codes to their original destinations (public, no auth required)

All redirect clicks are recorded **asynchronously** (IP address, user agent, referrer, timestamp), providing a
foundation for analytics. An admin role allows privileged account management.

---

## ✨ Features

| Feature                 | Description                                                                   |
|-------------------------|-------------------------------------------------------------------------------|
| 🔐 JWT Authentication   | Stateless login via `email + password`; token returned on login               |
| 🔗 URL Shortening       | Generates unique 6-character alphanumeric short codes using `SecureRandom`    |
| 📋 URL Management       | List (paginated), deactivate, and view your own short URLs                    |
| ⏰ Expiry Support        | Optional expiry date/time per short URL; expired links return HTTP `410 Gone` |
| ↩️ Smart Redirect       | HTTP `302` redirect; validates active/expiry status before redirecting        |
| 📊 Click Analytics      | Async recording of IP address, user agent, referrer, and timestamp per click  |
| ⚡ In-Memory Caching     | Caffeine cache for hot-path redirects (up to 10,000 entries, 5-min TTL)       |
| 🛡️ Role-Based Access   | `ROLE_USER` for regular users, `ROLE_ADMIN` for admin operations              |
| 🏷️ Correlation IDs     | Every request tagged with `X-Correlation-Id` for distributed tracing          |
| 📄 OpenAPI / Swagger UI | Interactive API docs available at `/swagger-ui.html`                          |
| 🔄 Database Migrations  | Liquibase manages all schema changes — versioned and repeatable               |
| 🧪 Test Coverage        | Unit tests for all controllers and services                                   |

---

## 🛠️ Tech Stack

| Layer       | Technology                             |
|-------------|----------------------------------------|
| Language    | Java 17                                |
| Framework   | Spring Boot 3.5.10                     |
| Security    | Spring Security + JWT (jjwt 0.11.5)    |
| Persistence | Spring Data JPA / Hibernate            |
| Database    | PostgreSQL                             |
| Migrations  | Liquibase                              |
| Caching     | Spring Cache + Caffeine                |
| API Docs    | SpringDoc OpenAPI 2.8.5 (Swagger UI)   |
| Validation  | Jakarta Bean Validation                |
| Boilerplate | Lombok                                 |
| Build Tool  | Apache Maven (Maven Wrapper included)  |
| Testing     | JUnit 5, Mockito, Spring Security Test |

---

## 🏗️ Architecture Overview

```
┌────────────────────────────────────────────────────────┐
│                   HTTP Clients / Browsers               │
└───────────────────────────┬────────────────────────────┘
                            │
              ┌─────────────▼─────────────┐
              │   CorrelationIdFilter      │  (adds X-Correlation-Id)
              │   JwtFilter                │  (validates Bearer token)
              └─────────────┬─────────────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
   ┌─────▼──────┐   ┌───────▼──────┐  ┌───────▼──────┐
   │AuthController│  │UrlController │  │RedirectCtrl  │
   │AdminController│ │              │  │              │
   └─────┬──────┘   └───────┬──────┘  └───────┬──────┘
         │                  │                  │
   ┌─────▼──────┐   ┌───────▼──────┐  ┌───────▼──────────────┐
   │AuthService │   │UrlService    │  │RedirectService        │
   │AdminService│   │              │  │(Cacheable shortUrls)  │
   └─────┬──────┘   └───────┬──────┘  └───────┬──────────────┘
         │                  │                  │
         │                  │          ┌───────▼──────────────┐
         │                  │          │ClickEventService      │
         │                  │          │(@Async - thread pool) │
         │                  │          └───────┬──────────────┘
         │                  │                  │
   ┌─────▼──────────────────▼──────────────────▼──────┐
   │               PostgreSQL Database                  │
   │  users │ short_urls │ url_click_events │ audit_logs│
   └───────────────────────────────────────────────────┘
```

---

## ✅ Prerequisites

Before running the project, ensure the following are installed:

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.8+** — or use the included `./mvnw` wrapper
- **PostgreSQL 13+** — [Download](https://www.postgresql.org/download/)
- **Git** — [Download](https://git-scm.com/)

---

## 🚀 Setup & Installation

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/url-shortener.git
cd url-shortener
```

### 2. Create the PostgreSQL Database

Connect to PostgreSQL and run:

```sql
CREATE
DATABASE url_shortener;
```

> The default credentials in `application.properties` are `postgres` / `root`. Update them to match your local setup (
> see [Configuration](#-configuration)).

### 3. Configure the Application

Copy and edit the properties file (or set environment variables — see [Configuration](#-configuration)):

```bash
cp src/main/resources/application.properties src/main/resources/application-local.properties
```

At minimum, update the following in `application.properties` (or the new `application-local.properties`):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener
spring.datasource.username=your_pg_username
spring.datasource.password=your_pg_password
jwt.secret=your_base64_encoded_secret_min_32_bytes
app.base-url=http://localhost:8080
```

### 4. Build the Project

```bash
./mvnw clean package -DskipTests
```

---

## ⚙️ Configuration

All configuration properties live in `src/main/resources/application.properties`:

| Property                                          | Default                                          | Description                                           |
|---------------------------------------------------|--------------------------------------------------|-------------------------------------------------------|
| `server.port`                                     | `8080`                                           | HTTP port the server listens on                       |
| `spring.datasource.url`                           | `jdbc:postgresql://localhost:5432/url_shortener` | JDBC URL                                              |
| `spring.datasource.username`                      | `postgres`                                       | Database username                                     |
| `spring.datasource.password`                      | `root`                                           | Database password                                     |
| `spring.datasource.hikari.maximum-pool-size`      | `10`                                             | HikariCP max connections                              |
| `spring.liquibase.enabled`                        | `true`                                           | Enable/disable Liquibase migrations                   |
| `jwt.secret`                                      | _(base64 encoded)_                               | Base64-encoded HS256 signing key (≥ 32 bytes decoded) |
| `jwt.expirationMs`                                | `3600000`                                        | JWT lifetime in milliseconds (default: 1 hour)        |
| `app.base-url`                                    | `http://localhost:8080`                          | Base URL prepended to short codes in responses        |
| `app.cache.short-urls.max-size`                   | `10000`                                          | Maximum entries in the `shortUrls` Caffeine cache     |
| `app.cache.short-urls.expire-after-write-minutes` | `5`                                              | Cache TTL in minutes                                  |
| `springdoc.swagger-ui.path`                       | `/swagger-ui.html`                               | Swagger UI path                                       |
| `springdoc.api-docs.path`                         | `/v3/api-docs`                                   | OpenAPI JSON path                                     |

> **Security Note:** Never commit production credentials or JWT secrets to version control. Use environment variables or
> a secrets manager in production.

### Generating a JWT Secret

```bash
openssl rand -base64 32
```

Paste the output as the value for `jwt.secret` in your properties file.

---

## ▶️ Running the Application

### Option A — Using Maven Wrapper

```bash
./mvnw spring-boot:run
```

### Option B — Running the JAR directly

```bash
./mvnw clean package -DskipTests
java -jar target/url-shortener-0.0.1-SNAPSHOT.jar
```

### Option C — Overriding properties via environment variables

Spring Boot maps `SPRING_DATASOURCE_URL` → `spring.datasource.url`, etc.:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/url_shortener
export SPRING_DATASOURCE_USERNAME=myuser
export SPRING_DATASOURCE_PASSWORD=mypassword
export JWT_SECRET=<your-base64-secret>
export APP_BASE_URL=https://yourdomain.com
./mvnw spring-boot:run
```

Once started, the application is available at:

- **API base:** `http://localhost:8080/api/v1`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

---

## 📚 API Documentation

Interactive Swagger UI is bundled with the application:

```
http://localhost:8080/swagger-ui.html
```

To authenticate in Swagger UI:

1. Call `POST /api/v1/auth/login` and copy the `accessToken` from the response.
2. Click the **Authorize 🔓** button at the top of the page.
3. Paste the token (without the `Bearer ` prefix) and click **Authorize**.

---

## 🔌 API Endpoints & Examples

### Base URL

```
http://localhost:8080/api/v1
```

---

### Authentication

> All auth endpoints are **public** (no JWT required).

#### Register a New User

```http
POST /api/v1/auth/register
Content-Type: application/json
```

**Request Body:**

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "Secret42"
}
```

> Password rules: minimum 8 characters, must contain at least one letter and one number.

**Response `201 Created`:**

```json
{
  "id": 1,
  "email": "jane@example.com"
}
```

**Error Responses:**
| Status | Code | Meaning |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Invalid field values |
| `409` | `USER_ALREADY_EXISTS` | Email already registered |

---

#### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

**Request Body:**

```json
{
  "email": "jane@example.com",
  "password": "Secret42"
}
```

**Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600,
  "email": "jane@example.com",
  "role": "ROLE_USER"
}
```

**Error Responses:**
| Status | Meaning |
|---|---|
| `401` | Invalid credentials |

---

### URL Management

> All URL endpoints require a valid JWT token: `Authorization: Bearer <token>`

#### Create a Short URL

```http
POST /api/v1/urls
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "originalUrl": "https://www.example.com/very/long/path?query=value",
  "expiryDate": "2026-12-31T23:59:59"
}
```

> `expiryDate` is optional. If omitted, the short URL never expires.

**Response `201 Created`:**

```json
{
  "id": 42,
  "shortUrl": "http://localhost:8080/api/v1/aB3xYz",
  "shortCode": "aB3xYz",
  "originalUrl": "https://www.example.com/very/long/path?query=value",
  "createdAt": "2026-03-04T10:00:00",
  "expiryDate": "2026-12-31T23:59:59",
  "active": true
}
```

**Error Responses:**
| Status | Meaning |
|---|---|
| `400` | Invalid URL format or validation error |
| `401` | Unauthorized (missing or invalid JWT) |

---

#### List My Short URLs (Paginated)

```http
GET /api/v1/urls?page=0&size=10
Authorization: Bearer <token>
```

| Parameter | Type | Default | Description              |
|-----------|------|---------|--------------------------|
| `page`    | int  | `0`     | Zero-based page index    |
| `size`    | int  | `10`    | Items per page (max 100) |

**Response `200 OK`:**

```json
{
  "content": [
    {
      "id": 42,
      "shortUrl": "http://localhost:8080/api/v1/aB3xYz",
      "shortCode": "aB3xYz",
      "originalUrl": "https://www.example.com/very/long/path",
      "createdAt": "2026-03-04T10:00:00",
      "expiryDate": null,
      "active": true
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

#### Deactivate a Short URL

```http
PATCH /api/v1/urls/{id}/deactivate
Authorization: Bearer <token>
```

**Response `204 No Content`** on success.

**Error Responses:**
| Status | Meaning |
|---|---|
| `401` | Unauthorized |
| `403` | URL belongs to a different user |
| `404` | URL not found |

---

### Redirect

> This endpoint is **public** — no authentication required.

#### Resolve & Redirect

```http
GET /api/v1/{shortCode}
```

**Example:**

```bash
curl -L http://localhost:8080/api/v1/aB3xYz
```

**Responses:**
| Status | Meaning |
|---|---|
| `302 Found` | Redirects to the original URL via `Location` header |
| `404 Not Found` | Short code does not exist |
| `410 Gone` | Short URL is inactive or has expired |

Each successful redirect is asynchronously logged with: IP address, User-Agent, Referrer, and timestamp.

---

### Admin

> Admin endpoints are accessible only by users with `ROLE_ADMIN`.

#### Create an Admin Account

```http
POST /api/v1/admin/create
Content-Type: application/json
```

**Request Body:**

```json
{
  "name": "Admin User",
  "email": "admin@example.com",
  "password": "AdminPass1"
}
```

**Response `201 Created`:**

```json
{
  "id": 2,
  "email": "admin@example.com"
}
```

---

## 🗃️ Database Schema

Schema is managed by **Liquibase** and automatically applied on startup. Migration files are located in
`src/main/resources/db/changelog/changes/`.

### Tables

#### `users`

| Column          | Type         | Constraints                         |
|-----------------|--------------|-------------------------------------|
| `id`            | BIGINT       | PK, auto-increment                  |
| `name`          | VARCHAR(100) |                                     |
| `email`         | VARCHAR(255) | NOT NULL, UNIQUE                    |
| `password_hash` | VARCHAR(255) | NOT NULL                            |
| `role`          | VARCHAR(20)  | NOT NULL, CHECK (`USER` or `ADMIN`) |
| `created_at`    | TIMESTAMP    |                                     |
| `updated_at`    | TIMESTAMP    |                                     |

#### `short_urls`

| Column         | Type      | Constraints               |
|----------------|-----------|---------------------------|
| `id`           | BIGINT    | PK, auto-increment        |
| `user_id`      | BIGINT    | FK → `users.id`, NOT NULL |
| `short_code`   | VARCHAR   | NOT NULL, UNIQUE          |
| `original_url` | TEXT      | NOT NULL                  |
| `is_active`    | BOOLEAN   |                           |
| `expires_at`   | TIMESTAMP |                           |
| `created_at`   | TIMESTAMP |                           |
| `updated_at`   | TIMESTAMP |                           |

#### `url_click_events`

| Column         | Type      | Description          |
|----------------|-----------|----------------------|
| `id`           | BIGINT    | PK, auto-increment   |
| `short_url_id` | BIGINT    | FK → `short_urls.id` |
| `clicked_at`   | TIMESTAMP |                      |
| `ip_address`   | VARCHAR   | Client IP            |
| `user_agent`   | TEXT      | Browser/device info  |
| `referrer`     | TEXT      | HTTP Referer header  |

#### `audit_logs`

| Column           | Type      | Description                   |
|------------------|-----------|-------------------------------|
| `id`             | BIGINT    | PK, auto-increment            |
| `actor_user_id`  | BIGINT    | User who performed the action |
| `action`         | VARCHAR   | Action type                   |
| `resource_type`  | VARCHAR   | Affected resource type        |
| `resource_id`    | VARCHAR   | Affected resource ID          |
| `status`         | VARCHAR   | Outcome status                |
| `message`        | TEXT      | Human-readable details        |
| `correlation_id` | VARCHAR   | Linked request correlation ID |
| `created_at`     | TIMESTAMP |                               |

### Performance Indexes

Liquibase migration `005-add-performance-indexes.xml` creates:

- `idx_short_urls_user_created` — on `short_urls(user_id, created_at DESC)` for fast paginated user URL listing
- `idx_click_events_short_url_id` — on `url_click_events(short_url_id)` for analytics queries

---

## ⚡ Caching

The application uses a **Caffeine** in-memory cache (`shortUrls`) to speed up the hot redirect path:

- **Cache name:** `shortUrls`
- **Key:** `shortCode` (String)
- **Value:** `ShortUrl` entity
- **Default max size:** 10,000 entries
- **Default TTL:** 5 minutes after write
- **Eviction on deactivation:** The cache entry is explicitly evicted when a URL is deactivated via `UrlService`

Cache settings can be tuned via `application.properties`:

```properties
app.cache.short-urls.max-size=10000
app.cache.short-urls.expire-after-write-minutes=5
```

---

## 🔀 Async Processing

Click events are recorded **asynchronously** using a dedicated `ThreadPoolTaskExecutor` (`clickTrackingExecutor`) to
prevent click tracking from blocking the redirect response:

| Property          | Value                  |
|-------------------|------------------------|
| Core pool size    | 4 threads              |
| Max pool size     | 10 threads             |
| Queue capacity    | 500 tasks              |
| Thread prefix     | `async-click-`         |
| Graceful shutdown | Yes (waits up to 30 s) |

---

## 🔒 Security

### Authentication Flow

```
Client → POST /api/v1/auth/login
       ← { accessToken, expiresIn, email, role }

Client → GET/POST /api/v1/urls
         Authorization: Bearer <accessToken>
       ← 200 / 201 (if token valid)
       ← 401 (if token missing/invalid/expired)
```

### JWT Details

- **Algorithm:** HS256 (HMAC-SHA256)
- **Default expiry:** 1 hour (configurable via `jwt.expirationMs`)
- **Claims:** `sub` (email/username), `iat`, `exp`
- **Storage:** Stateless — no server-side session

### Roles

| Role         | Description                                           |
|--------------|-------------------------------------------------------|
| `ROLE_USER`  | Can create, list, and deactivate their own short URLs |
| `ROLE_ADMIN` | Can create admin accounts; has elevated privileges    |

### Public Endpoints (No JWT Required)

| Endpoint                | Method |
|-------------------------|--------|
| `/api/v1/auth/register` | POST   |
| `/api/v1/auth/login`    | POST   |
| `/api/v1/{shortCode}`   | GET    |
| `/api/v1/admin/**`      | ALL    |
| `/swagger-ui/**`        | GET    |
| `/v3/api-docs/**`       | GET    |

### Passwords

Passwords are hashed with **BCrypt** before storage. Raw passwords are never persisted.

### Correlation IDs

Every HTTP request receives an `X-Correlation-Id` header (taken from the incoming request if present, otherwise
generated as a UUID). The ID appears in all log entries for that request, enabling end-to-end tracing.

---

## 🧪 Testing

The project includes unit tests for all service and controller layers.

### Test Structure

```
src/test/java/com/urlshortener/
├── controller/
│   ├── AdminControllerTest.java
│   ├── AuthControllerTest.java
│   ├── RedirectControllerTest.java
│   └── UrlControllerTest.java
└── service/
    ├── AdminServiceTest.java
    ├── AuthServiceTest.java
    ├── ClickEventServiceTest.java
    ├── CustomUserDetailsServiceTest.java
    ├── RedirectServiceTest.java
    └── UrlServiceTest.java
```

### Running All Tests

```bash
./mvnw test
```

### Running a Specific Test Class

```bash
./mvnw test -Dtest=UrlServiceTest
```

### Running Tests with Coverage (Surefire report)

```bash
./mvnw verify
```

Reports are generated in `target/surefire-reports/`.

---

## 🚢 Deployment

### Running as a Standalone JAR

```bash
./mvnw clean package -DskipTests
java -jar target/url-shortener-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:postgresql://<host>:5432/url_shortener \
  --spring.datasource.username=<user> \
  --spring.datasource.password=<pass> \
  --jwt.secret=<base64-secret> \
  --app.base-url=https://yourdomain.com
```

### Docker (Manual Setup)

Create a `Dockerfile` in the project root:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/url-shortener-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
docker build -t url-shortener:latest .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/url_shortener \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e JWT_SECRET=<base64-secret> \
  -e APP_BASE_URL=http://localhost:8080 \
  url-shortener:latest
```

### Docker Compose (App + PostgreSQL)

Create a `docker-compose.yml` in the project root:

```yaml
version: "3.9"
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: url_shortener
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: root
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build: .
    depends_on:
      - db
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/url_shortener
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: root
      JWT_SECRET: <your-base64-secret>
      APP_BASE_URL: http://localhost:8080

volumes:
  postgres_data:
```

Start everything:

```bash
docker compose up --build
```

### Cloud Deployment Notes

- Set all sensitive values (`jwt.secret`, DB credentials) as **environment variables** or use a secrets manager (AWS
  Secrets Manager, Azure Key Vault, etc.)
- For production, set `spring.jpa.show-sql=false` and `spring.jpa.hibernate.ddl-auto=validate`
- Configure a proper `app.base-url` (e.g., `https://yourdomain.com`) — this is included in every short URL response
- Ensure the Caffeine cache `max-size` and `expire-after-write-minutes` values are tuned to your expected traffic

---

## 🛠️ Troubleshooting

### `Failed to obtain JDBC Connection`

- Ensure PostgreSQL is running and accessible on the configured host/port.
- Verify the database `url_shortener` exists.
- Check username/password in `application.properties`.

### `LiquibaseException: Validation failed`

- Liquibase runs on startup and validates the schema against changesets.
- Do not manually alter the database schema — use Liquibase changesets instead.
- If you need to reset: drop the database, recreate it, and restart the app.

### `401 Unauthorized` on protected endpoints

- Ensure you include the `Authorization: Bearer <token>` header.
- Tokens expire after 1 hour by default. Re-authenticate to get a new token.
- Verify `jwt.secret` has not changed between token generation and validation.

### `410 Gone` on redirect

- The short URL has either been **deactivated** (`PATCH /api/v1/urls/{id}/deactivate`) or its **expiry date** has
  passed.

### Short URL not updating after deactivation (cached)

- The `shortUrls` Caffeine cache has a 5-minute TTL. The cache entry is evicted programmatically on deactivation, so
  this should update immediately. If a stale entry persists, restart the application to clear the in-memory cache.

---

## 🤝 Contributing

Contributions are welcome! To contribute:

1. **Fork** the repository
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes and **add tests** for new functionality
4. Ensure all tests pass:
   ```bash
   ./mvnw test
   ```
5. Commit with a descriptive message:
   ```bash
   git commit -m "feat: add click analytics endpoint"
   ```
6. Push and open a **Pull Request** against `main`

### Code Style Guidelines

- Follow standard Java naming conventions
- Use Lombok annotations where appropriate to reduce boilerplate
- Add Javadoc comments to public service methods
- Add OpenAPI `@Operation` and `@ApiResponse` annotations to new controller endpoints
- All new Liquibase changesets must go in a new numbered file under `src/main/resources/db/changelog/changes/`

---

## 📜 License

This project is licensed under the **MIT License**.

```
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📬 Contact & Support

| Channel        | Details                                                                                                   |
|----------------|-----------------------------------------------------------------------------------------------------------|
| **Issues**     | Open a [GitHub Issue](https://github.com/your-username/url-shortener/issues) for bugs or feature requests |
| **API Team**   | URL Shortener Team (see OpenAPI contact info at `/swagger-ui.html`)                                       |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` — try all endpoints interactively                                 |

---

> Built with ❤️ using Spring Boot 3 · Java 17 · PostgreSQL · Liquibase · JWT · Caffeine

