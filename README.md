# ShoppingSecService

Spring Boot authentication microservice for the shopping-list ecosystem.
Provides user registration, JWT access and refresh tokens with rotation, token validation for other services, and user profile operations.

## Features

- **User registration** – with validation, unique username constraint, and BCrypt password hashing
- **Login** – returns access (JWT, 15 min) + refresh (JWT, 14 days) tokens; refresh token also issued as an HTTP-only, Secure, SameSite=Strict cookie
- **Refresh token rotation** – each refresh issues a new token pair and revokes the previous one; reuse detection revokes all tokens for the user
- **Logout** – revokes all refresh tokens and clears the cookie
- **Public token validation** – other services can validate access tokens via `GET /user`
- **Saved-time tracking** – optimistic-locking timestamp per user for data synchronization
- **Stateless API security** – Spring Security with custom JWT filter, no HTTP sessions
- **Database migrations** – Flyway-managed schema (MariaDB)

## Architecture

Layered Spring Boot application.

| Layer | Technology |
|-------|-----------|
| Web | Spring Web (REST), embedded Tomcat |
| Security | Spring Security, custom `JwtAuthFilter`, JJWT 0.11.5 (HMAC-SHA256) |
| Persistence | Spring Data JPA, Hibernate 6.5, MariaDB |
| Migrations | Flyway (`flyway-mysql`) |
| Validation | Jakarta Validation (`spring-boot-starter-validation`) |
| Password hashing | BCrypt (`BCryptPasswordEncoder`) |
| Build | Maven |

## Ecosystem (microservices)

This service provides authentication for the entire shopping-list ecosystem. Other services delegate JWT validation to this service via HTTP:

```
                    ┌─────────────────────┐
                    │  ShoppingSecService  │
                    │  (this service)      │
                    │  port 4443           │
                    └──────────┬──────────┘
                               │ REST (JWT validation)
          ┌────────────────────┼────────────────────┐
          │ REST               │ REST                │ WS
          ▼                    ▼                     ▼
 ┌────────────────┐ ┌──────────────────┐ ┌──────────────────┐
 │ShoppingListWeb │ │ShoppingList      │ │ShoppingListService│
 │(Angular 21 SPA)│ │(Android app)     │ │(shopping backend)│
 │                │ │                  │ │port 5443         │
 └────────────────┘ └──────────────────┘ └──────────────────┘
          │                    │
          │                    └────────REST────────┐
          │                                         │
          │                              ┌──────────┴──────────┐
          └────────REST──────────────────│ShoppingListRecipes  │
                                         │Service (port 6443)  │
                                         └─────────────────────┘
```

## Stack

| Area | Technology |
|------|------------|
| Runtime | Java 21, Spring Boot 3.4.5 |
| Web | Spring Web (REST), Tomcat |
| Security | Spring Security, JJWT 0.11.5 (HMAC-SHA256) |
| Data | Spring Data JPA, Hibernate 6.5, MariaDB |
| Migrations | Flyway (`flyway-mysql`) |
| Validation | Jakarta Validation |
| Password hashing | BCrypt |
| Threading | Virtual threads (`spring.threads.virtual.enabled=true`) |
| Build | Maven |

## Requirements

- **JDK 21**
- **Maven 3.8+**
- **MariaDB** with database `shopping_list_users_db`

## Configuration

All configuration is in `src/main/resources/application.properties`. Sensitive values are set via environment variables:

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` | MariaDB username |
| `DB_PASSWORD` | MariaDB password |
| `JWT_ACCESS_SECRET` | HMAC secret for access JWTs (HS256) |
| `JWT_REFRESH_SECRET` | HMAC secret for refresh JWTs (HS256) |

### Profiles

| Profile | Config | Behavior |
|---------|--------|----------|
| Default | `application.properties` | Local defaults |
| `dev` | `application-dev.properties` | JPA `ddl-auto=none` |
| `prod` | `application-prod.properties` | JPA `ddl-auto=none`, Actuator disabled |

### Default settings (`application.properties`)

- **Port:** `4443`
- **Datasource:** `jdbc:mariadb://localhost:3306/shopping_list_users_db`
- **Flyway:** enabled, same database
- **JPA:** `ddl-auto` set per profile (`none` for dev/prod)
- **Actuator:** disabled by default

## Database

Managed by Flyway migrations (`src/main/resources/db/migration/`).

**Table `user`:**

| Column | Type | Notes |
|--------|------|-------|
| `user_name` | `varchar(255)` PK | Logical primary key |
| `password` | `varchar(255)` | BCrypt hash |
| `saved_time` | `datetime(6)` | `@Version` optimistic lock |
| `role` | `varchar(50)` | Default `'USER'` (added by V2) |

**Table `refresh_token`:**

| Column | Type | Notes |
|--------|------|-------|
| `jti` | `varchar(255)` PK | UUID from JWT `jti` claim |
| `user_name` | `varchar(255)` FK → user | `ON DELETE CASCADE` |
| `expiration_time` | `datetime(6)` | Token expiry |
| `is_revoked` | `bit(1)` | Rotation / reuse flag |

## Build and run

You need **JDK 21**, **Maven 3.8+**, and a running **MariaDB** instance with database `shopping_list_users_db`.

```bash
# Set required environment variables
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export JWT_ACCESS_SECRET=your-access-secret-min-256-bits
export JWT_REFRESH_SECRET=your-refresh-secret-min-256-bits

# Build
mvn clean package

# Run with dev profile
java -jar target/ShoppingSecService-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# Run with prod profile
java -jar target/ShoppingSecService-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Development:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## API

All endpoints are mapped to `/user`.  
Authentication is via `Authorization: Bearer <JWT>` header unless stated otherwise.

### Endpoints

| Method | Path | Auth | Request | Response | Description |
|--------|------|------|---------|----------|-------------|
| `POST` | `/user` | Public | `UserRequestDto` | `LocalDateTime` | Register user (returns savedTime) |
| `POST` | `/user/register` | Public | `UserRequestDto` | `TokenDto` + cookie | Register and auto‑login |
| `POST` | `/user/log` | Public | `UserRequestDto` | `TokenDto` + cookie | Login |
| `GET` | `/user/logout` | Public | — | `Boolean(true)` + cleared cookie | Revoke all tokens, clear cookie |
| `GET` | `/user/refresh` | Refresh token¹ | — | `TokenDto` + new cookie | Rotate refresh token |
| `GET` | `/user` | Public | `Authorization: Bearer <token>` | `UserInfoDto` | Validate access token |
| `GET` | `/user/{userName}` | JWT | — | `UserDto` | Get user profile |
| `PUT` | `/user/savedTime` | JWT | `UserDto` | `200 OK` | Update savedTime (ignores body.userName, uses authenticated user) |

¹ The refresh token can be sent in the `Authorization: Bearer <refreshToken>` header **or** in the `refreshToken` cookie.

### Data types – JSON schemas

#### `UserRequestDto`

```json
{
  "userName": "jan_kowalski",
  "password": "bezpieczneHaslo123",
  "savedTime": null
}
```

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `userName` | string | **Required,** unique | Username |
| `password` | string | **Required,** 8–64 chars | Plain‑text password (BCrypt hashed server‑side) |
| `savedTime` | string (ISO‑8601, nullable) | — | Initial timestamp (usually `null`) |

#### `TokenDto`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | string (JWT) | Short‑lived (15 min). Sent as `Authorization: Bearer <token>` |
| `refreshToken` | string (JWT) | Long‑lived (14 days). Also set as `HttpOnly; Secure; SameSite=Strict` cookie |

#### `UserInfoDto`

```json
{
  "userName": "jan_kowalski",
  "role": "USER"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `userName` | string | Username |
| `role` | enum | `USER` or `ADMIN` |

### Example flows

#### Registration + auto‑login

```
POST /user/register
Content-Type: application/json

{
  "userName": "jan_kowalski",
  "password": "bezpieczneHaslo123"
}

→ 200 OK
Set-Cookie: refreshToken=eyJ...; HttpOnly; Secure; SameSite=Strict
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### Login

```
POST /user/log
Content-Type: application/json

{
  "userName": "jan_kowalski",
  "password": "bezpieczneHaslo123"
}

→ 200 OK
Set-Cookie: refreshToken=eyJ...; HttpOnly; Secure; SameSite=Strict
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### Token validation (from other services)

```
GET /user
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

→ 200 OK
{
  "userName": "jan_kowalski",
  "role": "USER"
}
```

#### Refresh token rotation

```
GET /user/refresh
Cookie: refreshToken=eyJhbGciOiJIUzI1NiJ9...

→ 200 OK
Set-Cookie: refreshToken=eyJ...; HttpOnly; Secure; SameSite=Strict
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Refresh token rotation flow (detailed)

1. Client calls `GET /user/refresh` with the refresh token in the `Authorization` header (Bearer) or `refreshToken` cookie.
2. Server extracts the `jti` claim from the token and looks up the `RefreshToken` entity with a **pessimistic write lock**.
3. **If revoked** → **reuse detected**: all tokens for that user are revoked, a warning is logged, and `401` is returned.
4. **If expired** → `401`.
5. Otherwise: marks the current token as revoked, generates **new** access token (15 min) + refresh token (14 days), persists the new refresh token, and returns the pair.
6. The old token can no longer be used.

### Security

- **CSRF:** disabled (stateless JWT)
- **Sessions:** stateless
- **JWT filter:** `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter` on every request
  - Reads `Authorization: Bearer <token>` header, falls back to `refreshToken` cookie
  - For `/user/refresh`: validates as a refresh token
  - For all other paths: validates as an access token
- **Permitted paths:** `GET /user`, `POST /user`, `POST /user/register`, `POST /user/log`, `GET /user/logout`
- **All other paths:** require valid authentication

### Token details

| | Access token | Refresh token |
|---|---|---|
| Algorithm | HMAC-SHA256 | HMAC-SHA256 |
| Expiry | 15 minutes | 14 days |
| Key | `jwt.secret_key.access` | `jwt.secret_key.refresh` |
| Claims | `sub` (username), `iat`, `exp` | `sub` (username), `jti` (UUID), `iat`, `exp` |
| Storage | Client only (memory / localStorage) | Client + database (persisted) |

## Tests

```bash
mvn test
```

H2 in-memory database available for test isolation.

## Project structure

```
src/main/java/pl/kamjer/ShoppingSecService/
├── config/security/      # JwtAuthFilter, AuthEntryPoint, UserDetailService, WebSecurityConfiguration
├── controller/           # UserController + ShoppingListControllerAdvice (global error handler)
├── model/                # JPA entities (User, RefreshToken) and DTOs
├── repository/           # Spring Data repositories (UserRepository, JwtRepository)
├── service/              # UserService, JwtService, CustomService, Role enum
└── validation/           # @UniqUserNameConstraint + validator

src/main/resources/
├── application*.properties
└── db/migration/         # Flyway SQL
```

## Related repositories

### Client applications

| Repository | Description |
|------------|-------------|
| [**Shopping-list-web**](https://github.com/KamJer/Shopping-list-web) | Angular SPA |
| [**Shopping-List-Client**](https://github.com/KamJer/Shopping-List-Client) | Android application |

### Other backend services

| Repository | Description |
|------------|-------------|
| [**shopping-list-service**](https://github.com/KamJer/shopping-list-service) | Shopping list backend with WebSocket sync (delegates JWT validation here) |
| [**Shopping-list-recipes-service**](https://github.com/KamJer/Shopping-list-recipes-service) | Recipe microservice |

---

## Privacy Policy

Detailed information about data processing can be found here:
[Privacy Policy](PRIVACY_POLICY.md)

## Account Deletion

If you want to delete your account and associated data, follow the instructions here:
[Account Deletion](ACCOUNT_DELETION.md)

## Contact

For questions or concerns: [kamjersoft@gmail.com](mailto:kamjersoft@gmail.com)
