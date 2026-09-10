# Doctrip-backend

Backend for DocTrip — patient identity, registration, and authentication.

## Story 1 — Patient Registration, Login & Account Management

Building this story part by part, commit by commit, following its FR-01..FR-10 breakdown:

- [x] FR-01 Patient registration
- [x] FR-02 Registration OTP (generate/send/verify/expire/resend/attempt-limit)
- [x] FR-03 Successful registration verification → authenticated session
- [x] FR-04 Login (password), Redis-backed lockout after repeated failures
- [x] FR-05 Login OTP
- [ ] FR-06 Google OAuth
- [ ] FR-07 Apple OAuth
- [ ] FR-08 Session management (access/refresh tokens, expiry)
- [ ] FR-09 Logout
- [ ] FR-10 Cross-platform identity (implicit once the above land; verify with tests)

See `CLAUDE.md` for architecture and conventions.

## Running

```bash
cp .env.example .env   # then edit with real values, or use your shell/IDE's env support
mvn spring-boot:run     # http://localhost:8081, requires Postgres + Redis reachable
mvn test                 # runs against H2 + in-memory rate-limiter fakes, no external services needed
```

OTP delivery is currently a dev stub (`ConsoleOtpDeliveryService`) that prints the code to stdout —
real email/SMS providers are not wired up yet. OTP resend cooldowns and login-failure lockouts are
backed by Redis (`REDIS_HOST`/`REDIS_PORT` env vars, defaults to `localhost:6379`).
