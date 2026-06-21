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

### Profiles

| Profile | Config | Secrets |
|---------|--------|---------|
| Default | `application.properties` | None |
| `dev` | `application-dev.properties` | Imports `application-secret-dev.properties` |
| `prod` | `application-prod.properties` | Imports `application-secret-prod.properties` |

### Secret properties (gitignored)

Create `application-secret-dev.properties` and/or `application-secret-prod.properties` in `src/main/resources/` with:

| Property | Purpose |
|----------|---------|
| `spring.datasource.username` | Database user |
| `spring.datasource.password` | Database password |
| `spring.flyway.user` | Flyway user |
| `spring.flyway.password` | Flyway password |
| `jwt.secret_key.access` | HMAC secret for access JWTs (HS256) |
| `jwt.secret_key.refresh` | HMAC secret for refresh JWTs (HS256) |

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

```bash
mvn clean package -DskipTests
java -jar target/ShoppingSecService-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Development:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## API

All endpoints mapped to `/user`.

| Method | Path | Auth | Request | Response | Description |
|--------|------|------|---------|----------|-------------|
| `POST` | `/user` | Public | `UserRequestDto` | `LocalDateTime` | Register user |
| `POST` | `/user/register` | Public | `UserRequestDto` | `TokenDto` + cookie | Register + auto-login |
| `POST` | `/user/log` | Public | `UserRequestDto` | `TokenDto` + cookie | Login |
| `GET` | `/user/logout` | Public | – | `Boolean(true)` + cleared cookie | Revoke all tokens + clear cookie |
| `GET` | `/user/refresh` | Refresh token (cookie or header) | – | `TokenDto` + new cookie | Rotate refresh token |
| `GET` | `/user` | Public | `Authorization: Bearer <token>` | `UserInfoDto` | Validate access token |
| `GET` | `/user/{userName}` | Authenticated | – | `UserDto` | Get user profile |
| `PUT` | `/user/savedTime` | Authenticated | `UserDto` (body) | `200 OK` | Update saved time (always for authenticated user, ignores body userName) |

### Refresh token rotation flow

1. Client calls `GET /user/refresh` with the refresh token in the `Authorization` header (Bearer) or `refreshToken` cookie
2. Server extracts the `jti` claim from the token and looks up the `RefreshToken` entity with a pessimistic write lock
3. If the token is revoked → **reuse detected**: all tokens for that user are revoked, a warning is logged, and `401` is returned
4. If expired → `401`
5. Otherwise: marks the current token revoked, generates a new access token (15 min) and new refresh token (14 days), persists the new refresh token, returns the pair

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
