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
- [x] FR-10 Cross-platform identity (one Patient identity/JWT regardless of channel; see `GET /api/patients/me` and `CrossPlatformIdentityTests`)

Story 1's backend is feature-complete against its FR breakdown. `Doctrip-frontend` (Angular, same
stack as `HRMS-frontend`) hasn't been started yet.

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


## Story 3 — DocTrip Admin: Hospital Master Records

Independent of the patient-facing Story 1 backend (separate branch, no shared auth/session code).
DocTrip Admin creates, views, edits, and activates/deactivates hospital master records; a public
endpoint exposes active hospitals for patient-facing discovery.

- [x] AC-1: Create a hospital master record (name, type, city, state, registration number,
      accreditation, beds, contact desk details) — starts inactive
- [x] AC-2: Reject creation with missing required fields or a duplicate name+city
- [x] AC-3: Edit an existing hospital's details
- [x] AC-4: Activate / deactivate a hospital
- [x] AC-5: Responsibility boundary — no department/doctor management, assignment, availability,
      appointment coordination, or booking in this story
- [x] AC-6: Active hospitals are discoverable via a public, read-only endpoint with a narrow
      response shape (no registration number or contact details)
- [ ] Admin authentication — explicitly out of scope; the reference mockup has no login screen

See `CLAUDE.md` for architecture and design rationale.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/admin/hospitals` | Create a hospital |
| GET | `/api/admin/hospitals` | List all hospitals |
| GET | `/api/admin/hospitals/{id}` | Get one hospital |
| PUT | `/api/admin/hospitals/{id}` | Edit a hospital |
| POST | `/api/admin/hospitals/{id}/status` | Activate/deactivate (`{"active": true\|false}`) |
| GET | `/api/hospitals` | Public: active hospitals only, narrow shape |

## Running

```bash
cp .env.example .env   # fill in real DB credentials
mvn spring-boot:run
mvn test
```

Requires a local Postgres database named `doctrip`. Serves on `http://localhost:8081`.
