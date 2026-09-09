# Doctrip-backend

Backend for DocTrip — patient identity, registration, and authentication.

## Story 1 — Patient Registration, Login & Account Management

Building this story part by part, commit by commit, following its FR-01..FR-10 breakdown:

- [x] FR-01 Patient registration
- [x] FR-02 Registration OTP (generate/send/verify/expire/resend/attempt-limit)
- [x] FR-03 Successful registration verification → authenticated session
- [ ] FR-04 Login (password)
- [ ] FR-05 Login OTP
- [ ] FR-06 Google OAuth
- [ ] FR-07 Apple OAuth
- [ ] FR-08 Session management (access/refresh tokens, expiry)
- [ ] FR-09 Logout
- [ ] FR-10 Cross-platform identity (implicit once the above land; verify with tests)

See `CLAUDE.md` for architecture and conventions.

## Running

```bash
cp .env.example .env   # then edit with real values, or use your shell/IDE's env support
mvn spring-boot:run     # http://localhost:8081, requires Postgres reachable at DB_URL
mvn test                 # runs against H2, no external DB needed
```

OTP delivery is currently a dev stub (`ConsoleOtpDeliveryService`) that prints the code to stdout —
real email/SMS providers are not wired up yet.
