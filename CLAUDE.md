# CLAUDE.md — DocTrip Backend

Conventions for future work in this repo. Keep this file in sync with what's actually implemented.
Mirrors the sibling `HRMS-backend` repo's architecture closely (same author's prior project), with
two deliberate deviations: **this repo uses Lombok** for entities/DTOs (`@Getter`/`@Setter`/
`@NoArgsConstructor`/`@AllArgsConstructor`/`@Builder`) rather than hand-written accessors, and
**this repo uses Redis** for rate limiting/caching (HRMS-backend has no such layer).

## Modules

- **Story 1 — Patient Registration, Login & Account Management**: `auth` (registration, OTP,
  login, OAuth, sessions, logout) + `patient` (`GET /api/patients/me`). Backend is now complete
  against the story's FR-01..FR-10 breakdown — see `README.md`'s checklist and this file's section
  headings below for what each FR actually built.
- **`RegisterRequest.mobile`** is validated as exactly 10 digits (`^[0-9]{10}$`, tightened from an
  earlier 7–15-digit range) and **`RegisterRequest.agreeToTerms`** (`@AssertTrue`) is required —
  both changed to match `Doctrip-frontend`'s registration form exactly (the reference mockup,
  `doctrip-web-panels 2.html`'s `PatientPanel.dc.html` artboard, validates a strict 10-digit mobile
  number and requires a Terms of Service checkbox before submit). `Patient.termsAcceptedAt` records
  when consent was given; `AuthService.register` sets it to `Instant.now()`. If the frontend's
  mobile format or a required-consent field ever changes, update this contract in both repos
  together — see `Doctrip-frontend`'s `CLAUDE.md` for the other side of this mapping.

## Java & Spring Boot versions

- Java 17, Spring Boot 3.3.4, Maven (`spring-boot-starter-parent` as parent POM).
- Base package: `com.doctrip.backend`. Entry point: `com.doctrip.backend.DoctripBackendApplication`.
- Runs on port **8081** (HRMS-backend already uses 8080 on this machine).

## Architecture

```
src/main/java/com/doctrip/backend/
  controller/    thin @RestController classes — no business logic, delegate to services
  service/       business logic, @Transactional boundaries
  repository/    Spring Data JPA repositories
  entity/        JPA entities (Lombok-annotated)
  dto/           request/response DTOs (Lombok-annotated) — never expose entities over the API
  exception/     custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
  security/      JwtService, JwtAuthenticationFilter, PatientPrincipal,
                 OAuth2LoginSuccessHandler, OAuth2LoginFailureHandler
  config/        SecurityConfig, PasswordEncoderConfig, OtpProperties, LoginProperties
```

## OTP design (FR-02/FR-05)

- `OtpChallenge` entity stores only a **hash** of the code (via the same `PasswordEncoder` bean
  used for account passwords) plus channel/purpose/target/expiry/attempts — the plaintext OTP is
  never persisted, logged, or returned from any service method except transiently into
  `OtpDeliveryService.send(...)`.
- `OtpService.issue(...)`: invalidates any previously-issued, still-live OTP for the same
  patient+purpose before creating a new one (BR-07), and rejects a resend inside the configured
  cooldown window (`doctrip.otp.resend-cooldown-seconds`, default 30s) with 429.
- `OtpService.verify(...)`: rejects consumed/invalidated/expired codes and locks out after
  `doctrip.otp.max-attempts` (default 5) wrong attempts, all with 400/429 responses — never a
  generic 500.
- **Rollback gotcha**: `OtpService.verify` is `@Transactional`, and the exceptions it throws on a
  wrong/expired/locked OTP are unchecked, so Spring's default rollback behavior would silently
  undo the attempt-count increment (and the audit record) along with everything else in that
  transaction — defeating the attempt limit entirely. Fixed by moving both writes into their own
  `REQUIRES_NEW` transactions: `OtpAttemptRecorder.recordAttempt(...)` and
  `AuditService.record(...)` both commit independently of the caller's transaction outcome. Do not
  inline those saves back into `OtpService`/`AuthService`'s own transactional methods.
- `doctrip.otp.*` properties live in `config/OtpProperties.java`.

## Rate limiting — Redis-backed (FR-02/FR-04/FR-05/BR-06)

- `OtpRateLimiter`/`LoginRateLimiter` are the abstractions. `RedisOtpRateLimiter` uses `SET NX EX`
  (`StringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration)`) for the OTP resend
  cooldown — atomic across concurrent requests, and doesn't need its own DB row/timestamp to derive
  state from (an earlier version compared `OtpChallenge.createdAt` in the DB; replaced per the
  user's explicit request to move rate limiting onto Redis). `RedisLoginRateLimiter` uses
  `INCR`+`EXPIRE` keyed by the raw login identifier string (email or mobile, whatever the caller
  typed) to lock out after `doctrip.login.max-failed-attempts` (default 5) failures within
  `doctrip.login.lockout-window-seconds` (default 900s).
- Both are `@Profile("!test")`; the `test` profile swaps in `InMemoryOtpRateLimiter`/
  `InMemoryLoginRateLimiter` (in `src/test/java`, same package) with identical semantics, so the
  test suite never needs a real Redis instance — same pattern as
  `ConsoleOtpDeliveryService`/`CapturingOtpDeliveryService`.
- `spring.data.redis.*` (`REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`) configures the connection;
  `spring-boot-starter-data-redis` autoconfigures a `StringRedisTemplate` — no custom `RedisConfig`
  class needed for this usage.
- Login-OTP success does **not** reset `LoginRateLimiter`'s failure counter for that identity — the
  counter only tracks the password path's own brute-force attempts, and OTP login never reads or
  clears it. Don't wire a cross-reset between the two without re-checking this is still the
  intended behavior.

## OTP delivery (dev stub — real providers not wired yet)

- `OtpDeliveryService` is the abstraction; `ConsoleOtpDeliveryService` (`@Profile("!test")`) prints
  `[DEV OTP] channel=... target=... otp=...` to **stdout directly** (not through the logger, so it
  never lands in the rolling `logs/doctrip.log` file) — this is temporary scaffolding per the
  user's plan to wire real email/SMS providers in a later session.
- Under the `test` profile, `CapturingOtpDeliveryService` (in `src/test/java`) captures the last
  OTP sent per target instead, so integration tests can complete a real verify flow without
  scraping stdout or the database.
- When real providers land, add new `OtpDeliveryService` implementations (e.g.
  `SesOtpDeliveryService`, `TwilioOtpDeliveryService`) selected by profile/config — don't delete
  `ConsoleOtpDeliveryService`/`CapturingOtpDeliveryService`, they stay useful for local dev and
  tests.

## OAuth2 login (FR-06 Google, FR-07 Apple)

- Real Spring Security OAuth2 Client login — `spring-boot-starter-oauth2-client`, registrations
  for `google` and `apple` in `application.yml`, credentials from env vars
  (`GOOGLE_CLIENT_ID`/`SECRET`, `APPLE_CLIENT_ID`/`SECRET`). Not a stub: `GET
  /oauth2/authorization/{google|apple}` does a real redirect and handshake once real credentials
  are in `.env` — the only thing left as a manual step is generating Apple's client-secret JWT
  (Apple's "client secret" isn't static; it's a JWT you sign yourself with your Apple private key,
  and it expires — see the comment in `application-example.yml`). No client-secret auto-generation
  job exists yet; add one before this goes anywhere near production.
- `OAuth2LoginSuccessHandler` extracts `sub`/`email`/`name`/`email_verified` from the provider's
  claims, builds an `OAuthCompleteRequest`, and calls `AuthService.completeOAuth(...)` — then
  redirects to `doctrip.oauth2.frontend-redirect-uri` (`{frontend}/auth/callback`) with
  `?token=...&refreshToken=...` (or `?error=...` on any failure, via `OAuth2LoginFailureHandler`
  for handshake-level failures or the success handler's own catch block for business-logic
  failures like an unverified email) — both tokens, matching every other auth path's `AuthResponse`
  shape (FR-08). An earlier version of this redirect only carried `token`, silently dropping the
  refresh token for OAuth logins alone; fixed once `Doctrip-frontend`'s OAuth callback route needed
  to consume both.
- **Deliberate deviation from HRMS-backend**: there is **no public `POST /api/auth/oauth/complete`
  endpoint**. HRMS-backend exposes exactly that, permitAll, accepting `{provider, externalId,
  email, name}` directly from any client — which means anyone can self-declare an arbitrary
  "verified" email and create/link an account without ever proving they own that identity through
  a real OAuth handshake. `OAuthCompleteRequest` here is documented as internal-only (see its
  javadoc) and only ever constructed by `OAuth2LoginSuccessHandler` from claims Spring Security
  already validated against the provider. Do not add an HTTP endpoint that accepts this DTO's
  fields as user input — test `AuthService.completeOAuth(...)` directly instead (see
  `OAuthFlowTests`, which autowires `AuthService` rather than going through MockMvc for this part).
- Account-linking safety mirrors HRMS-backend's reasoning: `completeOAuth` links to an existing
  patient purely by email match, safe only because it also checks `emailVerified` — but only
  rejects when the provider explicitly reports `false`. An absent claim is treated as "provider
  doesn't tell us," not "unverified" (relevant for providers that omit the claim under some
  conditions).
- `ExternalIdentity` (provider + externalId, unique together, → patientId) is the link table,
  mirroring HRMS-backend's entity of the same purpose. A brand-new OAuth identity creates a patient
  with `status="active"` and `emailVerified=true` directly — OAuth doesn't go through the
  registration OTP flow, since the provider has already proven the identity.

## Session management — access/refresh tokens (FR-08) and logout (FR-09)

- **Access token**: the existing short-lived JWT (`JwtService`, 1800s default,
  `doctrip.jwt.expiry-seconds`), stateless, carries a `jti`.
- **Refresh token**: `RefreshToken` entity — an opaque high-entropy random value (32 bytes,
  URL-safe base64), **not** a JWT, because it must be revocable server-side. Only its SHA-256 hash
  is stored (`TokenHasher.sha256Hex`, `service/TokenHasher.java`) — SHA-256, not BCrypt, is
  deliberate here: unlike a password or OTP, a refresh token has no guessable keyspace to slow
  down, so a fast deterministic hash is correct and lets lookup use a plain indexed equality query
  instead of iterating candidates. Default lifetime 30 days (`doctrip.jwt.refresh-expiry-seconds`).
- **Rotation on every use**: `RefreshTokenService.verifyAndRotate(...)` revokes the presented
  refresh token and issues a new one in the same call — every authenticated response
  (`AuthResponse`) carries both `accessToken` and `refreshToken`, and `POST
  /api/auth/token/refresh` is how a client exchanges a still-valid refresh token for a new access
  token. Reusing an already-rotated (or expired, or never-existed) refresh token is a 401 — this is
  the "invalid/expired refresh token" rejection FR-08/BR-12 call for, and it also catches stolen
  refresh-token reuse (a legitimate client's next real refresh will fail with the same 401, a
  detectable sign of token theft — no additional handling wired up for that signal yet).
- `AuthService.issueSession(Patient)` is the single place that mints both tokens — every
  authentication path (`verifyOtp` for register/login-OTP, `login`, `completeOAuth`) calls it
  rather than constructing `AuthResponse` by hand. Keep it that way; don't reintroduce a
  single-token `new AuthResponse(token, ...)` call site.
- **Logout** (`POST /api/auth/logout`, requires a valid access token): denylists that specific
  access token by its `jti` (`RevokedToken` entity/table, checked in
  `JwtAuthenticationFilter.isRevoked(...)` on every request — same pattern as HRMS-backend's
  `RevokedTokenRepository`) AND revokes every refresh token for the patient
  (`RefreshTokenService.revokeAllForPatient`), so neither the current access token nor any
  outstanding refresh token can be used afterward. `revoked_token` rows are never pruned — same
  known gap as HRMS-backend, low urgency (expired-but-listed rows only cost a lookup, not
  correctness), but a scheduled cleanup would be a reasonable follow-up.
- FR-08's "handle expired/invalid refresh tokens" and FR-09's "require re-authentication when the
  session can no longer be refreshed" are both just: `/api/auth/token/refresh` returns 401, and the
  frontend's job is to send the patient back through login when that happens — no server-side
  "session" object beyond the `refresh_token` table rows.

## Controller / service / repository conventions

- Most auth endpoints are pre-auth (registration/OTP/login) and take no principal; `POST
  /api/auth/logout` is the first protected endpoint and uses `@AuthenticationPrincipal
  PatientPrincipal` (populated by `JwtAuthenticationFilter`) — that's the pattern to follow for any
  future endpoint that needs "the current patient."
- One service per resource area (`AuthService`, `OtpService`, `AuditService`); services throw the
  custom exceptions in `exception/` (`BadRequestException`, `ConflictException`, `NotFoundException`,
  `TooManyRequestsException`, `UnauthorizedException`) rather than returning error codes —
  `GlobalExceptionHandler` maps them to HTTP status + a consistent JSON body.
- DTOs are plain Lombok classes; response DTOs get a static `from(Entity)` factory method for
  entity → response mapping (see `PatientResponse.from`).

## Database configuration

- PostgreSQL in real environments, entirely via env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) —
  see `.env.example`. `spring.jpa.hibernate.ddl-auto: validate` — schema changes go through Flyway
  migrations in `src/main/resources/db/migration/`, never Hibernate auto-DDL.
- Test profile (`application-test.yml`, `@ActiveProfiles("test")`) uses H2 in `MODE=PostgreSQL` so
  the same Flyway migrations run against it.

## API conventions

- Base path `/api` on every controller.
- Every endpoint returns a DTO, never a JPA entity.
- Error shape (`exception/ApiError.java`): `{status, error, message, path, timestamp}`.
- `POST /api/auth/register` → 201 `{patientId, otpChannel, otpTarget, otpValiditySeconds}`
  (`OtpChallengeResponse` — shared shape with the login-OTP endpoint below)
- `POST /api/auth/otp/verify` → 200 `AuthResponse {accessToken, refreshToken, patient}` (completes
  registration when `purpose=REGISTER`; just authenticates when `purpose=LOGIN`) — 400 on
  wrong/expired/already-used code, 429 once the attempt limit is hit
- `POST /api/auth/otp/resend` → 204, 429 inside the resend cooldown
- `POST /api/auth/login` → 200 `AuthResponse`; 401 on wrong credentials or an unknown identifier
  (same status for both — don't leak which one was wrong); 429 once `LoginRateLimiter` locks the
  identifier out
- `POST /api/auth/login/otp` → 200 `OtpChallengeResponse`; 404 if no account matches
  `loginId` (email or mobile) — verify with the same `/api/auth/otp/verify` endpoint,
  `purpose=LOGIN`
- `POST /api/auth/token/refresh` → 200 `{accessToken, refreshToken, patient}`; 401 on an
  invalid/expired/already-used refresh token
- `POST /api/auth/logout` → 204; requires `Authorization: Bearer <accessToken>`
- `GET /api/patients/me` → 200 `PatientResponse`; requires `Authorization: Bearer <accessToken>` —
  FR-10's "same identity regardless of channel" made concrete: any valid access token, from any
  login path, resolves to the same patient row here (see `CrossPlatformIdentityTests`)

## Security requirements

- JWT bearer auth (`Authorization: Bearer <token>`), stateless sessions
  (`SessionCreationPolicy.STATELESS`), issued/validated by `JwtService` using `JWT_SECRET` from env.
  Default access-token expiry: 1800 seconds. See the "Session management" section above for the
  refresh-token/logout design (FR-08/FR-09) — both are implemented, not just scaffolded.
- `PasswordEncoderConfig` holds the `PasswordEncoder` bean (`BCryptPasswordEncoder`) separately from
  `SecurityConfig`, matching HRMS-backend's pattern (avoids a circular bean dependency now that
  `SecurityConfig` → `OAuth2LoginSuccessHandler` → `AuthService` → `PasswordEncoder` is a real
  chain).
- Password policy (`PasswordPolicy.isValid`): 8+ chars, upper, lower, digit, special character.
- CORS is restricted to `CORS_ALLOWED_ORIGIN` (default `http://localhost:4200`), configured in
  `SecurityConfig.corsConfigurationSource()`.

## Running & testing

```bash
cp .env.example .env
mvn spring-boot:run     # http://localhost:8081, requires Postgres + Redis reachable
mvn test                 # runs against H2 + in-memory rate-limiter fakes, no external services needed
mvn package               # builds target/doctrip-backend-0.0.1-SNAPSHOT.jar
```

## Constraints / things not to reintroduce

- No hardcoded credentials, secrets, or origins anywhere in source — everything environment-driven
  with documented defaults in `.env.example` / `application-example.yml`.
- No Hibernate auto-DDL in real profiles (`ddl-auto: validate` only) — schema changes always go
  through a new Flyway migration file.
- Never persist, log, or return the plaintext OTP value anywhere outside `OtpService.issue`'s local
  variable and its call into `OtpDeliveryService.send(...)`.
