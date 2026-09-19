STORY 7 — DOCTOR APPOINTMENT ACKNOWLEDGEMENT


Description

USER STORY
As a Doctor, I want to review an appointment request assigned to me and Accept, Reject, or Suggest an Alternative Slot so that the Hospital or System can establish a valid appointment proposal for Patient confirmation.

BUSINESS VALUE
Ensures Doctor acknowledgement before Patient confirmation.

Allows the Doctor to accept, reject, or propose an alternative appointment time.

Supports alternative Doctor coordination when the requested Doctor is unavailable.

Allows eligible Doctors to be contacted simultaneously where configured.

Prevents unnecessary Patient delay when Hospital Admin does not act within the Admin SLA.

Allows the System to continue the same coordination process after an Admin SLA breach.

Prevents duplicate Doctor assignment and appointment-slot allocation.

Ensures only one valid Doctor/slot combination can become the active appointment proposal.

Protects the appointment from stale responses, conflicting actions, and race conditions.

SCOPE / OVERVIEW
This story covers the Doctor's acknowledgement and response to an appointment request.

The Doctor receives an appointment acknowledgement request through the configured notification channel, such as email. The notification directs the Doctor to the appropriate Doctor workflow where the Doctor can review and respond.

The Doctor may:

Accept the requested appointment.

Reject / indicate unavailability.

Suggest an alternative date/time.

Hospital Admin coordinates the appointment where required. If Hospital Admin does not act within the configured Admin SLA, the System automatically takes over the same coordination mechanism.

Hospital Admin may intervene at any time, subject to the latest appointment state.

The Doctor's acceptance does not constitute final appointment confirmation. A valid Doctor response creates an appointment proposal that remains subject to Patient confirmation.

COORDINATION FLOW
https://orqueinnovationsllp.sharepoint.com/:i:/s/OrqueInnovationsLLP/IQDqVrIRBG8ISY2MigzIWfUkAaZYH-q0HRnsZvTtnWVRQk8?e=abO2eeConnect your OneDrive account

APPOINTMENT STATE FLOW
Successful Doctor Acceptance


DOCTOR COORDINATION
        ↓
AWAITING DOCTOR RESPONSE
        ↓
DOCTOR ACCEPTS
        ↓
VALIDATION
        ↓
DOCTOR + SLOT SECURED
        ↓
AWAITING PATIENT CONFIRMATION
Doctor Rejection


AWAITING DOCTOR RESPONSE
        ↓
DOCTOR REJECTS
        ↓
DOCTOR REQUEST CLOSED
        ↓
COORDINATION CONTINUES
Alternative Slot


AWAITING DOCTOR RESPONSE
        ↓
ALTERNATIVE PROPOSED
        ↓
SLOT VALIDATION
        ↓
VALID PROPOSAL
        ↓
AWAITING PATIENT CONFIRMATION
No Doctor Resolution


DOCTOR COORDINATION
        ↓
NO VALID DOCTOR / SLOT
        ↓
ESCALATION
        ↓
HOSPITAL ADMIN / CONFIGURED FALLBACK
REQUIREMENTS
FR-01 — Receive Appointment Acknowledgement Request
The Doctor shall receive an acknowledgement request through the configured notification channel.

The notification may be delivered by email and shall contain or provide access to the information required for the Doctor to evaluate the request.

The request shall identify:

Appointment/request ID.

Hospital.

Department.

Healthcare service/reason.

Requested date.

Requested time.

Relevant booking information.

Patient information permitted for Doctor access.

Permitted supporting documents or clinical information.

Response deadline, where applicable.

The notification itself shall not be treated as Doctor acknowledgement. The Doctor's response must be explicitly submitted through the supported Doctor workflow.

FR-02 — Review Appointment Request
The Doctor shall be able to review the appointment information required to determine whether the requested appointment can be accepted.

The Doctor shall be able to identify:

Patient.

Hospital.

Department.

Service/reason.

Requested date.

Requested time.

Relevant appointment information.

Response deadline.

Only information permitted for the Doctor's role shall be displayed.

FR-03 — Doctor Response
For an active acknowledgement request, the Doctor shall be able to submit exactly one response:

Accept

Reject / Unavailable

Suggest Alternative Slot

The response shall record:

Appointment/request ID.

Doctor ID.

Response type.

Response timestamp.

Proposed date/time, where applicable.

Appointment version.

Response/request identifier.

Once a response has been successfully committed, duplicate responses for the same acknowledgement request shall not modify the appointment.

FR-04 — Accept Requested Appointment
When the Doctor selects Accept, the System shall validate the request before committing the acceptance.

The System shall verify:

The appointment request is still active.

The appointment has not been cancelled.

The Doctor is still eligible.

The Doctor is still associated with the applicable Hospital/Department where required.

The requested slot is still valid and available.

The appointment version is current.

No other Doctor/slot has already been successfully secured.

If all validations succeed:

The Doctor and appointment slot shall be secured atomically.

The Doctor shall become the selected Doctor.

The appointment version shall be updated.

All other pending Doctor acknowledgement requests shall be closed.

The appointment shall move to Awaiting Patient Confirmation.

The Patient shall be notified that a valid appointment proposal is available for confirmation.

Doctor acceptance shall not make the appointment fully Confirmed.

FR-05 — Reject / Unavailable
When the Doctor selects Reject / Unavailable:

The rejection shall be recorded.

The Doctor's active acknowledgement request shall be closed.

The rejection reason shall be captured where configured.

Hospital Admin/System shall be notified.

Coordination shall continue according to the applicable coordination rules.

A Doctor rejection shall not cancel the Patient's overall appointment request.

If eligible alternative Doctors are available, coordination may continue with those Doctors.

FR-06 — Suggest Alternative Slot
When the Doctor cannot accept the requested time but can offer another suitable time, the Doctor may select Suggest Alternative Slot.

The Doctor shall provide:

Alternative date.

Alternative time.

Any required response information.

The System shall then:

Validate the Doctor's eligibility.

Validate the proposed date/time.

Validate applicable Hospital operating hours.

Validate Doctor availability.

Validate that the slot has not already been secured.

Validate the latest appointment version.

If the proposed slot is invalid or unavailable:

The proposal shall not become an appointment proposal.

The Doctor shall be informed that the proposed slot cannot be used.

Coordination shall continue according to the applicable rules.

If valid:

The proposal shall be recorded.

The valid Doctor/slot proposal shall be made available for Patient consideration.

The appointment shall remain Awaiting Patient Confirmation.

The Patient must explicitly confirm the proposal before the appointment becomes Confirmed.

A Doctor-proposed alternative shall never automatically become a confirmed appointment.

FR-07 — Requested Doctor Unavailable
If the requested Doctor is unavailable or cannot accept the request:

Hospital Admin/System shall identify eligible alternative Doctors.

Alternative Doctors shall be selected according to configured eligibility rules.

Same-Department Doctors shall be considered where applicable.

Doctors available for the requested time shall be preferred where configured.

Eligible Doctors may receive acknowledgement requests simultaneously.

Each Doctor independently responds with:

Accept

Reject / Unavailable

Suggest Alternative Slot

The requested Doctor's unavailability shall not require the Patient to restart the booking request.

FR-08 — Alternative Doctor Acceptance
When an alternative Doctor accepts:

The System shall perform the same validation required for the requested Doctor.

The Doctor and slot shall be atomically secured.

The alternative Doctor shall become the selected Doctor.

Other pending Doctor requests shall be closed.

The appointment shall move to Awaiting Patient Confirmation.

The Patient shall receive the actual coordinated Hospital, Department, Doctor, Date and Time.

A replacement Doctor must independently acknowledge the appointment.

FR-09 — Doctor Response SLA
Each Doctor acknowledgement request shall have a configured response window.

If the Doctor does not respond within the applicable response window:

The Doctor acknowledgement request shall expire.

The response shall no longer be accepted.

Hospital Admin/System shall be notified where applicable.

The request shall continue through the configured coordination/fallback process.

A late Doctor response shall not automatically modify the appointment.

FR-10 — Hospital Admin SLA and System Takeover
Hospital Admin may coordinate Doctor acknowledgement at any time.

If Hospital Admin does not complete the required coordination within the configured Admin SLA(1hr):

The System shall automatically take over the coordination process.

The System shall continue using the same defined Doctor-selection and acknowledgement rules.

Eligible Doctors may be contacted according to those rules.

The Patient shall not be required to restart the request.

Hospital Admin shall remain able to intervene, subject to the latest appointment state.

The takeover shall be recorded in the audit trail.

System takeover shall not create a separate coordination process or change the Doctor's available response options.

FR-11 — First Valid Acceptance Wins
When multiple eligible Doctors are contacted simultaneously, more than one Doctor may attempt to accept.

The System shall determine the winner based on the first successfully committed valid acceptance.

The acceptance process shall:

Validate the latest appointment state.

Validate the appointment version.

Validate Doctor eligibility.

Validate slot availability.

Atomically secure the Doctor and slot.

Commit the appointment state change.

Once one acceptance is successfully committed:

That Doctor becomes the selected Doctor.

The appointment moves to Awaiting Patient Confirmation.

Other pending Doctor requests are closed.

Subsequent acceptance attempts are treated as stale.

The winner shall be determined by successful system/database commitment, not merely by which Doctor clicked Accept first.

FR-12 — Stale Doctor Response
A Doctor response shall be considered stale when:

Another Doctor has already been selected.

The appointment/slot has already been secured.

The Patient has cancelled.

The appointment request has expired.

The request has been closed.

The Doctor is no longer eligible.

The appointment version has changed.

The response window has expired.

A stale response shall:

Not modify the appointment.

Not create or secure a Doctor/slot.

Not reopen a closed request.

Be recorded where required for audit purposes.

Return the latest applicable appointment state to the Doctor where supported.

FR-13 — Patient Cancellation During Doctor Coordination
The Patient may cancel the appointment request while it remains in a cancellable pre-confirmation state.

When Patient cancellation is successfully committed:

Active Doctor coordination shall stop.

Pending Doctor acknowledgement requests shall close.

Subsequent Doctor responses shall not modify the request.

The Booking Buffer shall be released.

The cancellation shall be recorded.

Relevant actors shall be notified.

The appointment shall move to Cancelled.

If Patient cancellation is committed before Doctor acceptance, a later Doctor acceptance shall be rejected as stale and shall not create or confirm the appointment.

FR-14 — Appointment Version and Concurrency Control
All Doctor responses that can change the appointment state shall use the latest appointment version.

The System shall prevent two competing responses from simultaneously securing different Doctors or slots.

A successful state-changing operation shall update the appointment version.

Any subsequent operation using an outdated version shall be rejected as stale.

BUSINESS RULES
ID

Rule

BR-01

Doctor acknowledgement is required before Patient confirmation.

BR-02

A Doctor may Accept, Reject / Unavailable, or Suggest Alternative Slot.

BR-03

The requested Doctor receives priority when available and eligible.

BR-04

If the requested Doctor is unavailable, eligible alternative Doctors may be contacted according to configured coordination rules.

BR-05

Eligible alternative Doctors may be contacted simultaneously.

BR-06

The first successfully committed valid Doctor/slot acceptance wins.

BR-07

Doctor selection and slot reservation shall be atomic.

BR-08

Once a Doctor/slot is secured, all other pending Doctor requests shall close.

BR-09

Alternative slots must be validated before becoming an actionable appointment proposal.

BR-10

A Doctor-proposed alternative is a proposal and does not constitute Patient confirmation.

BR-11

A replacement Doctor must independently acknowledge the appointment.

BR-12

Doctor rejection does not cancel the Patient's overall request.

BR-13

A Doctor response received after its response window shall not modify the appointment.

BR-14

Late/stale Doctor responses shall not override a secured appointment.

BR-15

Patient cancellation before confirmation stops active Doctor coordination.

BR-16

Patient cancellation releases the Booking Buffer.

BR-17

The Booking Buffer represents a provisional hold and does not constitute final appointment confirmation.

BR-18

Every committed Doctor/slot selection must be validated against the latest appointment state.

BR-19

Appointment version control shall be used to prevent conflicting state changes.

BR-20

Hospital Admin may intervene at any time, subject to the latest appointment state.

BR-21

When the Admin SLA expires, the System takes over using the same coordination mechanism.

BR-22

System takeover shall not require the Patient to restart the request.

BR-23

Doctor acceptance does not constitute final Patient confirmation.

BR-24

Only one Doctor/slot combination may be secured for an active appointment request.

BR-25

A cancelled or closed appointment cannot be reactivated by a late Doctor response.

ACCEPTANCE CRITERIA
AC-01 — Doctor Receives Request
Given a valid appointment requires Doctor acknowledgement,
when the request is assigned to an eligible Doctor,
then the Doctor shall receive an acknowledgement notification and access to the appointment request.

AC-02 — Doctor Reviews Request
Given the acknowledgement request is active,
when the Doctor opens it,
then the Doctor shall see the permitted information required to evaluate the appointment.

AC-03 — Doctor Accepts
Given the request, Doctor and slot are still valid,
when the Doctor accepts,
then the System shall atomically secure the Doctor and slot and move the appointment to Awaiting Patient Confirmation.

AC-04 — Doctor Acceptance Is Not Final Confirmation
Given the Doctor has accepted,
when Patient confirmation has not yet occurred,
then the appointment shall not be represented as fully Confirmed.

AC-05 — Doctor Rejects
Given the Doctor rejects the request,
when the rejection is successfully committed,
then the Doctor's acknowledgement request shall close and applicable coordination shall continue.

AC-06 — Doctor Suggests Alternative
Given the Doctor cannot accept the requested time,
when the Doctor submits an alternative slot,
then the System shall validate the proposed slot before it becomes an actionable proposal.

AC-07 — Valid Alternative
Given the Doctor's alternative slot is valid and available,
when validation succeeds,
then the alternative shall become a valid appointment proposal and the appointment shall await Patient confirmation.

AC-08 — Invalid Alternative
Given the Doctor proposes an unavailable or invalid slot,
when validation fails,
then the proposal shall not be committed as the appointment slot and coordination shall continue.

AC-09 — Requested Doctor Unavailable
Given the requested Doctor is unavailable,
when alternative coordination begins,
then eligible alternative Doctors shall be identified according to the configured rules and may be contacted simultaneously.

AC-10 — Alternative Doctor Accepts
Given an eligible alternative Doctor receives the request,
when that Doctor successfully accepts while the request remains valid,
then that Doctor and slot shall be secured and the appointment shall move to Awaiting Patient Confirmation.

AC-11 — Simultaneous Doctor Acceptance
Given multiple Doctors have active acknowledgement requests,
when more than one Doctor attempts to accept,
then only the first successfully committed valid acceptance shall secure the appointment.

AC-12 — Pending Requests Close
Given one Doctor/slot has been successfully secured,
when the appointment moves to Awaiting Patient Confirmation,
then all other pending Doctor acknowledgement requests shall be closed.

AC-13 — Doctor Response SLA
Given a Doctor does not respond within the configured Doctor response window,
when the response window expires,
then the Doctor request shall expire and the applicable coordination/fallback process shall continue.

AC-14 — Admin SLA Takeover
Given Hospital Admin has not completed required coordination before the configured Admin SLA expires,
when the SLA expires,
then the System shall take over and continue the same Doctor coordination mechanism.

AC-15 — Admin Intervention
Given the System has taken over coordination,
when Hospital Admin intervenes,
then Admin shall be able to manage the request subject to the latest appointment state.

AC-16 — Stale Doctor Response
Given another Doctor has already secured the appointment,
when a second Doctor subsequently accepts,
then the second acceptance shall be rejected as stale and shall not modify the appointment.

AC-17 — Patient Cancellation
Given the appointment is not finally confirmed,
when Patient cancellation is successfully committed,
then active Doctor coordination shall stop, pending Doctor requests shall close, and the Booking Buffer shall be released.

AC-18 — Cancellation Race Condition
Given Patient cancellation is committed before Doctor acceptance,
when the Doctor subsequently accepts,
then the acceptance shall be rejected and shall not create or confirm the appointment.

AC-19 — No Doctor Resolution
Given no eligible Doctor accepts and no valid alternative is established within the configured coordination process,
when the coordination process reaches its configured limit,
then the request shall follow the configured escalation/fallback process and the appropriate parties shall be notified.

AC-20 — No False Confirmation
Given a Doctor has accepted,
when Patient confirmation is still pending,
then the appointment shall remain Awaiting Patient Confirmation.

EDGE CASES
EC-01 — Requested Doctor Becomes Unavailable
Revalidate the Doctor before commitment and continue alternative Doctor coordination where applicable.

EC-02 — Slot Conflict During Acceptance
If the slot becomes unavailable before commitment, reject the acceptance and continue applicable coordination.

EC-03 — Doctor Responds After Selection
Reject the response as stale and do not modify the secured appointment.

EC-04 — Patient Cancels During Doctor Response
Whichever valid state-changing transaction commits first determines the resulting state. If cancellation commits first, Doctor acceptance must fail as stale.

EC-05 — Alternative Slot Becomes Unavailable
Revalidate the slot. Do not secure the unavailable slot. Continue applicable coordination.

EC-06 — Duplicate Doctor Response
Only one valid response for an active acknowledgement request shall be processed.

EC-07 — Doctor Responds After SLA
Reject the response as expired/stale and do not modify the appointment.

EC-08 — Requested Doctor Becomes Available Later
A later availability change shall not displace an already secured alternative Doctor.

EC-09 — Multiple Doctors Accept Simultaneously
Use atomic state/slot commitment and appointment version control. Only one Doctor shall become selected.

EC-10 — Patient Cancels After Doctor Acceptance
If Doctor acceptance has already committed, Patient cancellation shall follow the applicable pre-confirmation cancellation rules. A cancellation must not be treated as though the Doctor acceptance never occurred.

EC-11 — System Takes Over While Admin Is Acting
The latest successfully committed appointment state shall determine which action is valid. Conflicting stale actions shall be rejected.

EC-12 — Admin Intervenes After System Takeover
Admin may continue coordination subject to the latest appointment state.

EC-13 — Doctor Response After Patient Cancellation
The response shall be rejected as stale and shall not reactivate the cancelled request.

EC-14 — Doctor Response After Patient Confirmation
The response shall not modify the confirmed appointment.

EC-15 — Appointment Version Conflict
If the submitted response contains an outdated appointment version, reject the state-changing operation and return the latest appointment state.

NOTIFICATIONS
Event

Recipient

Platform

Notification

Channel

Doctor acknowledgement request

Doctor

Website

Appointment acknowledgement request

In-Website + Email

Doctor acknowledgement reminder

Doctor

Website

Appointment acknowledgement reminder

In-Website + Email

Doctor accepts

Hospital Admin / System

Website / System

Doctor acceptance notification

In-Website + Email

Doctor rejects

Hospital Admin / System

Website / System

Doctor rejection notification

In-Website + Email

Doctor proposes alternative

Hospital Admin / System

Website / System

Alternative slot proposal

In-Website + Email

Valid appointment proposal

Patient

Mobile App / Website

Appointment available for confirmation

Mobile Push + In-App + Email

Valid alternative proposal

Patient

Mobile App / Website

Alternative appointment proposal available

Mobile Push + In-App + Email

Doctor coordination continues

Hospital Admin / System

Website / System

Coordination status update

In-Website + Email

Doctor coordination cannot be completed

Patient + Hospital Admin

Mobile App / Website

Coordination could not be completed

Patient: Mobile Push + In-App + Email; Hospital Admin: In-Website + Email

Patient confirmation

Doctor + Hospital Admin

Website

Patient confirmed appointment

In-Website + Email

Patient cancellation

Hospital Admin + Relevant Doctor

Website

Patient cancellation notification

In-Website + Email

Admin SLA expired / System takeover

Hospital Admin + Audit

Website / System

System takeover notification / audit event

In-Website + Email

Doctor response expired

Hospital Admin / System, where applicable

Website / System

Doctor response expiry notification

In-Website + Email

The primary Doctor notification channel may be email. The notification shall direct the Doctor to the appropriate response workflow.

AUDIT TRAIL
The System shall record, where applicable:

Appointment/request creation.

Doctor assignment.

Doctor acknowledgement request.

Notification delivery event.

Doctor response.

Response type.

Response timestamp.

Proposed alternative slot.

Slot validation.

Doctor eligibility validation.

Doctor selection.

Doctor reassignment.

Appointment-slot reservation.

Pending Doctor request closure.

Doctor response SLA start/expiry.

Admin SLA start/expiry.

System takeover.

Admin intervention.

Patient notification.

Patient cancellation.

Booking Buffer creation/release.

Escalation.

Appointment state changes.

Appointment version changes.

Actor.

Timestamp.

Previous state.

New state.

Reason/event code.

DEPENDENCIES
Hospital Appointment Coordination.

Doctor Access.

Doctor Availability.

Hospital Calendar.

Booking Buffer.

Appointment Lifecycle.

SLA Engine.

Hospital Operating Hours.

Doctor Notification Service.

Patient Notification Service.

Patient Confirmation.

Concurrency / Version Control.

Authentication / Authorisation.

OUT OF SCOPE
The following are outside this story:

Patient registration/login implementation.

Patient appointment booking form.

Patient final confirmation workflow.

Payment processing.

Confirmed-appointment cancellation.

Refund processing.

Doctor clinical decision-making.

Hospital master-data management.

Doctor availability administration.

DEFINITION OF DONE
Doctor receives appointment acknowledgement requests.

Doctor can review the permitted appointment information.

Doctor can Accept, Reject, or Suggest Alternative Slot.

Requested Doctor receives priority when available and eligible.

Alternative Doctors can be coordinated according to configured rules.

Simultaneous Doctor requests are supported where applicable.

First successfully committed valid acceptance wins.

Doctor/slot selection is atomic.

Only one Doctor/slot combination can be secured.

Pending Doctor requests close after selection.

Alternative slots are validated.

Invalid slots cannot be secured.

Doctor rejection continues applicable coordination.

Doctor response SLA works.

Hospital Admin SLA works.

System takeover after Admin SLA works.

Hospital Admin can intervene after System takeover.

Stale and duplicate Doctor responses cannot modify the appointment.

Appointment version/concurrency control is enforced.

Patient cancellation stops active coordination where applicable.

Patient cancellation releases the Booking Buffer.

Doctor acceptance does not create final Patient confirmation.

Valid Doctor acknowledgement produces an Awaiting Patient Confirmation proposal.

Appropriate notifications are generated.

Audit trail is complete.

Race conditions and concurrency scenarios are tested.

QA/UAT completed.