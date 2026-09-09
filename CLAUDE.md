# CLAUDE.md — DocTrip Backend

Conventions for future work in this repo. Keep this file in sync with what's actually implemented.
Mirrors the sibling `HRMS-backend` repo's architecture closely (same author's prior project), with
one deliberate deviation: **this repo uses Lombok** for entities/DTOs (`@Getter`/`@Setter`/
`@NoArgsConstructor`/`@AllArgsConstructor`/`@Builder`) rather than hand-written accessors.

## Modules

- **Story 1 — Patient Registration, Login & Account Management**: `auth` (registration, OTP,
  login, OAuth, sessions, logout). Built commit-by-commit against the FR-01..FR-10 breakdown — see
  `README.md` for what's landed so far. Don't jump ahead to a later FR until earlier ones are
  committed and tested.

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
  security/      JwtService, JwtAuthenticationFilter, PatientPrincipal
  config/        SecurityConfig, PasswordEncoderConfig, OtpProperties
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

## Controller / service / repository conventions

- Controllers take no `@AuthenticationPrincipal` yet in this slice (registration/OTP endpoints are
  all pre-auth) — `PatientPrincipal` exists for `JwtAuthenticationFilter` to populate once
  protected endpoints exist.
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
- `POST /api/auth/otp/verify` → 200 `{accessToken, patient}` (completes registration when
  `purpose=REGISTER`)
- `POST /api/auth/otp/resend` → 204

## Security requirements

- JWT bearer auth (`Authorization: Bearer <token>`), stateless sessions
  (`SessionCreationPolicy.STATELESS`), issued/validated by `JwtService` using `JWT_SECRET` from env.
  Default expiry: 1800 seconds — **access token only** for now; refresh-token issuance/rotation and
  expiry handling is FR-08, not yet built (see README checklist).
- `PasswordEncoderConfig` holds the `PasswordEncoder` bean (`BCryptPasswordEncoder`) separately from
  `SecurityConfig`, matching HRMS-backend's pattern (avoids a circular bean dependency once OAuth2
  login is wired in a later commit).
- Password policy (`PasswordPolicy.isValid`): 8+ chars, upper, lower, digit, special character.
- OAuth2 (Google/Apple, FR-06/FR-07) and logout/token-revocation (FR-09) are **not implemented
  yet** — `SecurityConfig` deliberately has no `oauth2Login()` block and no revoked-token check in
  `JwtAuthenticationFilter`. Both existed in HRMS-backend's SecurityConfig/JwtAuthenticationFilter
  as a reference for how to add them here.
- CORS is restricted to `CORS_ALLOWED_ORIGIN` (default `http://localhost:4200`), configured in
  `SecurityConfig.corsConfigurationSource()`.

## Running & testing

```bash
cp .env.example .env
mvn spring-boot:run     # http://localhost:8081, requires Postgres reachable at DB_URL
mvn test                 # runs against H2, no external DB needed
mvn package               # builds target/doctrip-backend-0.0.1-SNAPSHOT.jar
```

## Constraints / things not to reintroduce

- No hardcoded credentials, secrets, or origins anywhere in source — everything environment-driven
  with documented defaults in `.env.example` / `application-example.yml`.
- No Hibernate auto-DDL in real profiles (`ddl-auto: validate` only) — schema changes always go
  through a new Flyway migration file.
- Never persist, log, or return the plaintext OTP value anywhere outside `OtpService.issue`'s local
  variable and its call into `OtpDeliveryService.send(...)`.
