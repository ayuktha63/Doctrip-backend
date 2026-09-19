package com.doctrip.backend.controller;

import com.doctrip.backend.dto.AppointmentRequestDetailResponse;
import com.doctrip.backend.dto.AppointmentRequestSummaryResponse;
import com.doctrip.backend.dto.CoordinationLogEntryResponse;
import com.doctrip.backend.dto.DoctorAvailabilityResponse;
import com.doctrip.backend.dto.PendingAcknowledgementResponse;
import com.doctrip.backend.security.HospitalAdminPrincipal;
import com.doctrip.backend.service.AppointmentRequestService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * FR-01 — Hospital Admin reviewing appointment requests routed to their hospital.
 *
 * <p>Moved from {@code /api/admin/appointment-requests} (Story 3's blanket-open
 * {@code /api/admin/**} prefix) to {@code /api/hospital-admin/appointment-requests} — this
 * endpoint was never a DocTrip-Admin ("no auth, hospital master records only") concern, it always
 * carried real per-hospital patient data (names, ages, health requirements), and the earlier
 * {@code hospitalId} being a plain client-supplied query param meant any anonymous caller could
 * read or mutate any hospital's coordination queue by guessing/iterating hospital ids or request
 * ids. {@code hospitalId} is now always taken from the authenticated {@link HospitalAdminPrincipal}
 * (real JWT, one hospital per session — see {@code hospitalAdminAuthInterceptor}), never the
 * caller, and every single-resource lookup is ownership-checked against it in the service layer.
 */
@RestController
@RequestMapping("/api/hospital-admin/appointment-requests")
public class AdminAppointmentRequestController {

    private final AppointmentRequestService appointmentRequestService;

    public AdminAppointmentRequestController(AppointmentRequestService appointmentRequestService) {
        this.appointmentRequestService = appointmentRequestService;
    }

    @GetMapping
    public List<AppointmentRequestSummaryResponse> list(@AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return appointmentRequestService.listForHospital(principal.getHospitalId());
    }

    /** "Doctor approvals" sidebar screen — see AppointmentRequestService.listPendingAcknowledgements. */
    @GetMapping("/pending-acknowledgements")
    public List<PendingAcknowledgementResponse> pendingAcknowledgements(@AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return appointmentRequestService.listPendingAcknowledgements(principal.getHospitalId());
    }

    @GetMapping("/{id}")
    public AppointmentRequestDetailResponse get(@PathVariable UUID id, @AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return appointmentRequestService.get(id, principal.getHospitalId());
    }

    @GetMapping("/{id}/doctor-availability")
    public DoctorAvailabilityResponse doctorAvailability(
            @PathVariable UUID id, @AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return appointmentRequestService.getDoctorAvailability(id, principal.getHospitalId());
    }

    @GetMapping("/{id}/log")
    public List<CoordinationLogEntryResponse> log(@PathVariable UUID id, @AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return appointmentRequestService.getLog(id, principal.getHospitalId());
    }

    @PostMapping("/{id}/coordination/start")
    public AppointmentRequestDetailResponse startCoordination(
            @PathVariable UUID id, @AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return appointmentRequestService.startCoordination(id, principal.getHospitalId());
    }

    @PostMapping("/{id}/coordination/resolve-alternative")
    public AppointmentRequestDetailResponse resolveAlternative(
            @PathVariable UUID id, @AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return appointmentRequestService.resolveAlternative(id, principal.getHospitalId());
    }
}
