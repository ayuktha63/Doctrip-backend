Backend


Description

BE-2.1 — Support Public Treatment Package Discovery
Description

Implement backend support for public treatment package discovery.

Requirements:

Treatment packages must be discoverable without authentication.

Visitor/Patient must be able to retrieve available treatment packages.

Public discovery must not require a patient session.

Only approved/published treatment packages should be shown.

Inactive records must follow the approved visibility rule.

No additional visibility rule should be introduced unless already defined elsewhere.

Support the empty-result case when no treatment packages are available.

Support the backend error case.

Story coverage: AC-1, AC-12, Business Rules, Shared Data.

BE-2.2 — Support Public Treatment Package Details
Description

Implement backend support for viewing an individual treatment package.

Requirements:

A visitor/patient must be able to retrieve an approved treatment package's details without authentication.

Only approved package information should be exposed.

Patient-specific information must not be exposed through public discovery.

Handle a package that does not exist.

Handle a package that becomes unavailable.

Support the appropriate unavailable/error response needed by the client.

Story coverage: AC-2, Edge Cases, Business Rules.

BE-2.3 — Support Public Hospital Discovery
Description

Implement backend support for hospital discovery.

Requirements:

Hospital listings must be publicly accessible.

Authentication must not be required for hospital discovery.

Only permitted/approved hospital records should be displayed.

Inactive records must follow the approved visibility rule.

Support the empty hospital result case.

Support backend errors.

Story coverage: AC-3, AC-12, Business Rules.

BE-2.4 — Support Hospital Search
Description

Implement backend support for searching hospitals.

Requirements:

An unauthenticated visitor/patient must be able to search hospitals.

Matching hospitals must be returned when matching records exist.

No-match searches must be handled.

Search requirements that are marked TBD in the story must remain TBD.

Do not introduce additional search filters or search rules that are not confirmed in the story.

Story coverage: AC-4, Dependencies — Search, Business Rules.

BE-2.5 — Support Public Hospital Profile
Description

Implement backend support for viewing a hospital's approved profile information.

Requirements:

Hospital profile information must be publicly accessible.

Authentication must not be required.

Only approved profile information should be returned.

Patient-specific information must not be exposed.

Handle missing profile information.

Handle a hospital becoming unavailable while it is being viewed.

Story coverage: AC-7, Edge Cases, Permissions.

BE-2.6 — Support Public Doctor Discovery
Description

Implement backend support for doctor discovery.

Requirements:

Doctor listings must be publicly accessible.

Authentication must not be required.

Only permitted/approved doctor records should be displayed.

Inactive records must follow the approved visibility rule.

Support the empty doctor result case.

Support backend errors.

Story coverage: AC-5, AC-12, Business Rules.

BE-2.7 — Support Doctor Search
Description

Implement backend support for searching doctors.

Requirements:

An unauthenticated visitor/patient must be able to search doctors.

Matching doctors must be returned when matching records exist.

No-match searches must be handled.

Search/filter requirements that are not confirmed in the story must remain TBD.

Do not introduce additional search/filter rules.

Story coverage: AC-6, Dependencies — Search, Business Rules.

BE-2.8 — Support Public Doctor Profile
Description

Implement backend support for viewing an approved doctor's profile.

Requirements:

Doctor profile information must be publicly accessible.

Authentication must not be required.

Only approved profile information should be returned.

Patient-specific information must not be exposed.

Handle missing profile information.

Handle a doctor becoming unavailable while being viewed.

Story coverage: AC-7, Edge Cases, Permissions.

BE-2.9 — Apply Public Discovery Visibility Rules
Description

Apply the visibility requirements defined in Story 2 to public discovery information.

Requirements:

Only approved/published records should be shown.

Apply the approved visibility rule for inactive records.

Apply the rules to:

Treatment packages

Hospitals

Doctors

Discovery must remain available without authentication.

Do not create a new rule for inactive records because the story does not define the exact behavior.

Story coverage: Business Rules.

BE-2.10 — Support Shared Discovery Data for Website and Mobile
Description

Ensure the backend supports the shared discovery data required by both Website and Mobile.

Requirements:

Mobile and Website must consume the shared backend.

Approved healthcare information must remain consistent across both channels.

Public discovery data must be available without requiring a patient session.

Protected patient-specific information must require authentication.

Story coverage: Shared Data, Dependencies.

BE-2.11 — Support Authentication Requirement at Booking Entry
Description

Support the authentication requirement when a visitor/patient attempts to enter the protected booking journey.

Requirements:

Discovery itself must remain public.

Booking actions must check authentication status.

An authenticated user must be allowed to proceed into the booking journey.

An unauthenticated user must not enter the protected booking journey without authentication.

The client must receive the information necessary to handle the authentication handoff.

Story coverage: AC-8, AC-9, Business Rules.

BE-2.12 — Support Retention of Discovery Context During Booking Handoff
Description

Support retention of the applicable discovery context when the user moves from discovery toward booking.

The applicable context can include:

Treatment package

Hospital

Department

Doctor

Requirements:

The selected context should be retained where technically supported.

The user should not need to unnecessarily restart the discovery journey.

The context must remain associated with the booking journey after successful authentication where applicable.

Handle the case where the selected discovery record becomes unavailable before booking starts.

Do not invent additional context beyond what is explicitly listed in the story.

Story coverage: AC-10, AC-11, Edge Cases.

BE-2.13 — Protect Patient-Specific and Booking Functionality
Description

Ensure protected functionality is not available to unauthenticated visitors.

Requirements:

Public discovery information can be accessed without authentication.

Patient-specific information requires authentication.

A visitor cannot complete a booking without authentication.

Protected booking functionality requires authentication.

Patient-specific information must not be exposed through public discovery functionality.

Story coverage: Business Rules, Permissions.

BE-2.14 — Support Admin Management of Healthcare Discovery Data
Description

Support the Admin role's ability to manage healthcare discovery data according to the role/permission requirements of the story.

Discovery data includes:

Treatment data

Hospital data

Doctor data

Requirements:

Admin manages healthcare discovery data according to role.

Visitor cannot modify healthcare master data.

Patient cannot modify healthcare master data.

Story coverage: Permissions.

BE-2.15 — Record Administrative Discovery Changes in Audit Trail
Description

Administrative changes to discovery records must capture:

Entity

Action

Administrator

Timestamp

Changed fields

Requirements:

Apply the audit trail to administrative changes to discovery records.

Authentication and booking actions remain under their respective stories.

Story coverage: Audit Trail.