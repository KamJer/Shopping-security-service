# Plan napraw - ShoppingSecService

**Data utworzenia:** 2026-09-23
**Stack:** Spring Boot 3.4.5, Java 21, Spring Security, JJWT 0.11.5, MariaDB

---

## PRIORYTET 1: Bezpieczenstwo (KRYTYCZNE)

### 1.1. Username enumeration via registration + validate endpoints

**Problem:** Registration endpoint zwraca 200 na success, 400 na duplicate username. Public `validateUser` endpoint rzuca `BadCredentialsException` dla invalid tokens i non-existent users — to samo.
**Lokalizacja:** `UserController.java:116`, `UserService.java:68,74,81`

**Plan:**

Krok 1: Unifikacja message bledow
- Registration: zawsze zwracac 201 z `savedTime` (nawet przy duplicate)
- Login: zawsze `BadCredentialsException` z message "Invalid username or password" (nie leakuje czy user exists)
- Validate: zawsze `BadCredentialsException` z message "Invalid or expired token" (nie leakuje czy user exists)

Krok 2: Usuniec `BadCredentialsException` dla non-authentication bledow
- `UserService.java:68,74,81` — `BadCredentialsException` uzyty dla "user not found"
- Zmienic na custom exception np. `AuthenticationFailedException`

---

### 1.2. Public validateUser endpoint bez rate limiting

**Problem:** `GET /user` (permitAll) bez rate limiting — mozliwosc brute-force tokenow.
**Lokalizacja:** `UserController.java:116`

**Plan:**

Krok 1: Dodac Spring Boot RateLimiter (lub Resilience4j)
- Ograniczyc `/user` endpoint do N requestow na minute na IP
- Zwracac 429 Too Many Requests przy przekroczeniu

Krok 2: Dodac header `Retry-After` w odpowiedzi 429

---

### 1.3. JWT secret key length validation

**Problem:** Brak walidacji minimum length secreta — short secret = weak JWT (HS256).
**Lokalizacja:** `JwtService.java:36-42`

**Plan:**

Krok 1: Dodac walidacje length w `JwtService`
- Min length: 32 bytes (256 bits) dla HS256
- Rzucic exception na startup jesli secret za krotki

Krok 2: Dodac configuration property z warningiem dla short secrets

---

### 1.4. BCrypt strength factor

**Problem:** Brak configured strength factor — default 10. Zalecane: 12 dla nowoczesnych aplikacji.
**Lokalizacja:** `SecurityConfig.java` (PasswordEncoder bean)

**Plan:**

Krok 1: Zmienic `new BCryptPasswordEncoder()` na `new BCryptPasswordEncoder(12)`

---

### 1.5. Zwrocic access token jako HttpOnly cookie (dual-delivery)

**Problem:** Obecnie access token zwrocany tylko w body JSON. Browser (ShoppingListWeb) potrzebuje dostepu do access tokena w pamieci (in-memory storage) — HttpOnly cookie chroni przed XSS.
**Lokalizacja:** `LoginResponseDto.java`, `AuthController.java`, `UserController.java` (login, register, refresh endpoints)

**Plan:**

Krok 1: Zwrocic access token **jednoczesnie** w HttpOnly cookie i w body JSON
- Endpointy: `/user/login`, `/user/register`, `/user/refresh`
- Cookie name: `access_token`
- Atrybuty: `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/`, `Max-Age` = czas zycia access_tokena
- Body JSON: zwrocic access_token (dla Androida, ktory nie obsluguje HttpOnly cookies automatycznie)
- Body JSON: zwrocic refresh_token, savedTime (bez zmian)

Krok 2: Zaktualizowac JWT validation filter
- `JwtAuthFilter.java` — sprawdzac token z cookies (obok Authorization header / query param)
- `request.getCookies()` → wyszukac cookie o nazwie `access_token`
- Ustawic `SecurityContextHolder` z tokenem z cookie

Krok 3: Zaktualizowac endpoint logout
- `UserController.java` — logout musi czyszczyc HttpOnly cookie (`access_token=null`, maxAge=0)

**Testy do napisania:**
- `AuthControllerTest.java` — login zwraca HttpOnly cookie i access_token w body
- `JwtAuthFilterTest.java` — token z cookie, token z header, brak token
- `UserControllerTest.java` — logout czysci cookie


---

## PRIORYTET 2: Jakosc kodu

### 2.1. `UserService extends CustomService` — nieintuicyjne share dependency

**Problem:** `UserService` extends `CustomService` tylko po to zeby udostepnic `SecClient` — composition byloby bardziej idiomaticzne.
**Lokalizacja:** `UserService.java:21`

**Plan:**

Krok 1: Zmienic na constructor injection
- Usunac `extends CustomService`
- Dodac `private final SecClient secClient;` w constructor

### 2.2. Brak `@Transactional(readOnly = true)` na read-only methods

**Problem:** Read-only methods nie oznaczone jako read-only — marnowanie zasobow DB.
**Lokalizacja:** `UserService.java` — `getAllUsers()`, `findByUserName()`, etc.

**Plan:**

Krok 1: Dodac `@Transactional(readOnly = true)` do wszystkich read-only method

### 2.3. `User.java:33-39` — `convertToSpringUser()` w entity

**Problem:** `convertToSpringUser()` embeds Spring Security concerns inside JPA entity — violation of separation of concerns.

**Plan:**

Krok 1: Przeniesc do separate mapper/utility class
- `UserMapper.toSpringUser(User)` lub podobne

---

## PRIORYTET 3: Testy

### 3.1. Testy JWT + Token

**Plan:**
- `JwtServiceTest.java` — token generation, extraction, expiration, invalid signature, key validation
- `TokenServiceTest.java` — rotation, reuse detection, revocation on password change
- `JwtAuthFilterTest.java` — token z header, token z cookie, expired token, invalid token

### 3.2. Testy Authentication

**Plan:**
- `UserServiceTest.java` — login flow, password comparison, registration, duplicate username
- `JwtAuthenticationProviderTest.java` — valid/invalid/expired token, SecClient error handling

### 3.3. Testy Authorization

**Plan:**
- `UserServiceTest.java` — `changeUserRole`, `deleteUser`, `changeUserPassword`
- `UserServiceTest.java` — self-modification prevention, SUPER_ADMIN protections
- `JwtAuthFilterTest.java` — role-based access (USER vs ADMIN vs SUPER_ADMIN)

### 3.4. Testy Controller

**Plan:**
- `UserControllerTest.java` — wszystkie endpointy (login, register, validate, refresh, logout, admin)
- `UserControllerTest.java` — various status codes, validation errors

### 3.5. Testy Integration

**Plan:**
- Full login → validate → refresh → logout flow
- Token rotation scenario (stale refresh token → revoked)
- Password change → all tokens revoked scenario

---

## PRIORYTET 4: Wydajnosc

### 4.1. Dodać index na refresh_token.user_name

**Problem:** `revokeAllForUser` query to full table scan bez index.
**Lokalizacja:** `db/migration/V2__add_refresh_tokens.sql`, `JwtRepository.java:22`

**Plan:**
- Dodac migration: `V3__add_refresh_token_user_index.sql`
- `CREATE INDEX idx_refresh_token_user ON refresh_token(user_name)`

---

## PRIORYTET 5: Cleanup

### 5.1. Usunac unnecessary dependencies

**Plan:**
- `pom.xml` — usunac explicit `hibernate-core` (Spring Boot manages)

### 5.2. Fix Flyway Maven plugin

**Problem:** Plugin ma hardcoded `shopping_list_db` ale aplikacja uzywa `shopping_list_users_db`.
**Lokalizacja:** `pom.xml:127-133`

**Plan:**
- Zmienic schema na `shopping_list_users_db` w plugin config

### 5.3. Upgrade JJWT

**Problem:** jjwt 0.11.5 z 2021. Rozważyć upgrade do 0.12.x dla better security i Java 21 compatibility.
**Lokalizacja:** `pom.xml:37-50`
