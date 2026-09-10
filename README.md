# Doctrip-backend

Backend for DocTrip — patient identity, registration, and authentication.

## Story 1 — Patient Registration, Login & Account Management

Building this story part by part, commit by commit, following its FR-01..FR-10 breakdown:

- [x] FR-01 Patient registration
- [x] FR-02 Registration OTP (generate/send/verify/expire/resend/attempt-limit)
- [x] FR-03 Successful registration verification → authenticated session
- [x] FR-04 Login (password), Redis-backed lockout after repeated failures
- [x] FR-05 Login OTP
- [x] FR-06 Google OAuth
- [x] FR-07 Apple OAuth (scaffolded — needs a real Apple Services ID + generated client-secret JWT to actually run; see `.env.example`)
- [x] FR-08 Session management (rotating refresh tokens, access-token expiry)
- [x] FR-09 Logout (access-token denylist + refresh-token revocation)
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

Google/Apple sign-in is real Spring Security OAuth2 login (`GET /oauth2/authorization/google` or
`/apple`) — not a stub — but needs real provider credentials in `.env` to actually redirect
anywhere; Apple's "client secret" is a JWT you generate and sign yourself (see `.env.example`).

Every authentication path (`register`+verify, `login`, `login/otp`+verify, OAuth) returns both an
`accessToken` (short-lived JWT, 30 min) and a `refreshToken` (opaque, 30 days, rotates on every use
via `POST /api/auth/token/refresh`). `POST /api/auth/logout` (requires a valid access token) ends
the session immediately — denylists that access token and revokes every refresh token for the
patient — rather than waiting for natural expiry.

