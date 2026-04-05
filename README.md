# Shopping Security Service

Backend microservice that provides **authentication and user management** for a shopping-list ecosystem. It exposes a REST API for registration, login, JWT access and refresh tokens, token validation, and user profile operations, backed by a MariaDB database with schema versioning via Flyway.

---

## Features

- **User registration** with validation and unique username constraints  
- **Login** returning access (JWT) and refresh tokens; refresh token also issued as an **HTTP-only, Secure, SameSite=Strict** cookie (`refreshToken`)  
- **Token refresh** with refresh-token rotation and persistence (revocation / expiry in database)  
- **Logout** clearing the refresh cookie  
- **Public token validation** endpoint for other services  
- **Stateless API security** using Spring Security, a custom JWT servlet filter, and BCrypt password hashing  
- **Database migrations** managed by Flyway  

---

## Technology Stack

| Area | Technology |
|------|------------|
| Runtime | Java **21** (virtual threads enabled) |
| Framework | **Spring Boot 3.4.5** |
| Web | Spring Web (REST), embedded **Tomcat** |
| Security | **Spring Security**, custom `JwtAuthFilter`, JJWT **0.11.5** |
| Persistence | **Spring Data JPA**, **Hibernate** 6.5.x |
| Database | **MariaDB** (MySQL connector also on classpath) |
| Migrations | **Flyway** (`flyway-mysql`) |
| Validation | Jakarta Validation (`spring-boot-starter-validation`) |
| Build | **Maven** |
| Other | Lombok, Spring Actuator (endpoints disabled by default), WebFlux & WebSocket starters (dependencies present for stack compatibility; no primary REST usage in the main user API) |

---

## Prerequisites

- **JDK 21**  
- **Maven 3.8+**  
- **MariaDB** (or compatible MySQL) with a database created for the application (default local name in config: `shopping_list_users_db`)  

---

## Configuration

### Profiles

| Profile | Main config | Behavior |
|---------|-------------|----------|
| Default | `application.properties` | Local defaults; imports **no** secrets file unless you add one |
| `dev` | `application-dev.properties` | Imports `application-secret-dev.properties` |
| `prod` | `application-prod.properties` | Imports `application-secret-prod.properties` |

Activate a profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
java -jar target/ShoppingSecService-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Secret properties (required for `dev` / `prod`)

Files `application-secret-dev.properties` and `application-secret-prod.properties` are **gitignored**. Create them under `src/main/resources/` with at least:

| Property | Purpose |
|----------|---------|
| `spring.datasource.username` | Database user |
| `spring.datasource.password` | Database password |
| `spring.flyway.user` | Flyway user (often same as datasource) |
| `spring.flyway.password` | Flyway password |
| `jwt.secret_key.access` | HMAC secret for **access** JWT (sufficient length for HS256) |
| `jwt.secret_key.refresh` | HMAC secret for **refresh** JWT |

**Security:** never commit real secrets; use environment-specific values and rotate keys according to your policy.

### Default application settings (`application.properties`)

- **Port:** `4443`  
- **Datasource URL:** `jdbc:mariadb://localhost:3306/shopping_list_users_db` (override per environment)  
- **JPA:** `ddl-auto` is profile-specific; production uses `none` (schema from Flyway only)  
- **Logging:** `logs/application.log`  
- **Actuator:** endpoints disabled by default (`management.endpoints.enabled-by-default=false`)  

---

## Database & Flyway

- SQL migrations live in `src/main/resources/db/migration/`.  
- On startup, Flyway runs against the same database as the application (see `spring.flyway.*` and datasource URL).  
- If a migration fails, fix the schema and Flyway history according to [Flyway repair / validate](https://documentation.red-gate.com/flyway) documentation.  
- **Foreign keys:** ensure `CHARSET` / `COLLATION` on new tables match the existing `user` table on your server; mismatches cause MariaDB error 150 on `CREATE TABLE` with FKs.  

---

## Build & Run

```bash
mvn clean package -DskipTests
java -jar target/ShoppingSecService-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Development with tests:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## API Overview

Base path: **`/user`** (unless a reverse proxy adds a prefix).

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/user` | Public | Register user (`UserRequestDto`) |
| `POST` | `/user/log` | Public | Login; returns `TokenDto` and sets `refreshToken` cookie |
| `GET` | `/user/logout` | Public | Clears `refreshToken` cookie |
| `GET` | `/user/refresh` | Authenticated (refresh JWT from cookie or header) | Issues new access + refresh tokens; updates cookie |
| `GET` | `/user` | Public | Validate user / token query (`token` request param) → `UserInfoDto` |
| `GET` | `/user/{userName}` | Authenticated | Get user by username |
| `PUT` | `/user/savedTime` | Authenticated | Update user saved time |

**Notes:**

- CSRF is **disabled** (typical for stateless JWT APIs).  
- Access tokens are usually sent as `Authorization: Bearer <accessToken>`.  
- Refresh flow expects a valid refresh token (cookie name `refreshToken` and/or `Authorization` header, depending on client; see `UserController` and `JwtAuthFilter`).  
- Cookie attributes **`Secure`** and **`SameSite=Strict`** require HTTPS and same-site (or careful cross-site) usage in browsers.  

---

## Testing

```bash
mvn test
```

H2 is available in the **test** scope for isolated tests if configured.

---

## Project Layout (high level)

```
src/main/java/pl/kamjer/ShoppingSecService/
├── config/security/     # Security filter chain, JWT filter, user details service
├── controller/          # REST controllers and global exception handling
├── model/               # JPA entities and DTOs
├── repository/          # Spring Data repositories
├── service/             # Business logic (users, JWT)
└── validation/          # Custom constraints

src/main/resources/
├── application*.properties
└── db/migration/        # Flyway SQL
```