# CLAUDE.md — DocTrip Backend

Conventions for future work in this repo. Keep this file in sync with what's actually implemented.

## Stack

- Spring Boot 3.3.4, Java 17, Maven, package `com.doctrip.backend`.
- Postgres (local dev DB name `doctrip`), managed via **Liquibase** (formatted-SQL changesets in
  `db/changelog/changes/*.sql`, included from `db.changelog-master.yaml`).
- **Lombok** for entities (`@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder`).
- Config is env-var-driven via `application.yml` + `spring-dotenv` reading a gitignored `.env`
  (see `.env.example` for the required keys — never put real secrets in `application.yml` itself).

## Story 3 — DocTrip Admin: Hospital Master Records

This branch (`DT-3-story-3-doctrip-hospital-management`) is branched directly from `develop` and
is intentionally **independent** of the patient-facing Story 1 branch — it does not reuse or
depend on Story 1's `Patient`/OTP/OAuth/session code in any way. The two stories will be merged
into `develop` separately.

- **No admin authentication is built for this story.** The reference mockup
  (`doctrip-web-panels 2.html`'s `Main.dc.html` artboard, labelled "Story 3 · Hospital
  management") has no login screen and starts pre-authenticated — admin login/JWT auth is
  explicitly out of scope here and belongs to a future story. `/api/admin/hospitals/**` is
  currently open; don't add auth to it as a side effect of other work without a story for it.
- **`Hospital`** is the only entity: `hospitalCode` (generated `HSP-%04d` from a Postgres
  sequence), `name`, `type` (default `Multi-speciality`), `city`, `state` (default `Kerala`),
  `registrationNumber`, `accreditation`, `beds`, `contactPerson`/`contactEmail`/`contactPhone`,
  `active` (defaults **false** — new hospitals start inactive until explicitly activated),
  `createdAt`/`updatedAt`.
- **Duplicate check**: create/update reject a hospital whose `name`+`city` (case-insensitive)
  already exists elsewhere, via a repository query — not just the DB's case-sensitive unique index
  (`ux_hospital_name_city`), which is a backstop, not the primary validation path.
- **Two controllers, two audiences**:
  - `AdminHospitalController` (`/api/admin/hospitals`) — full CRUD + status toggle, for DocTrip
    Admin.
  - `HospitalDiscoveryController` (`/api/hospitals`) — public, read-only, **active hospitals
    only**, and returns the narrow `HospitalSummaryResponse` shape (no registration number, no
    contact details) — never widen this response without checking what's meant to stay
    admin-only.
- **Non-goals, don't build here**: department management, doctor management/assignment,
  availability management, appointment coordination, doctor acknowledgement, patient booking.
  DocTrip Admin only creates/views/edits/activates/deactivates hospital master records.
- **CORS**: handled by a plain `WebMvcConfigurer` (`CorsConfig`), not Spring Security — there's no
  security dependency on this branch at all.

## Running & testing

```bash
mvn spring-boot:run   # or ./mvnw if a wrapper is present
mvn test
```

Requires Postgres running locally with a `doctrip` database and `.env` populated (see
`.env.example`). Backend serves on `http://localhost:8081`; CORS defaults to allow
`http://localhost:4300` (the frontend's dev server port).
