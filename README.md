# Doctrip-backend

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
