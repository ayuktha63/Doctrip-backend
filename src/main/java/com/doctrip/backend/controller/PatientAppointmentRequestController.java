package com.doctrip.backend.controller;

import com.doctrip.backend.dto.AppointmentRequestCreateRequest;
import com.doctrip.backend.dto.AppointmentRequestDetailResponse;
import com.doctrip.backend.dto.AppointmentRequestSummaryResponse;
import com.doctrip.backend.dto.CoordinationLogEntryResponse;
import com.doctrip.backend.security.PatientPrincipal;
import com.doctrip.backend.service.AppointmentRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-11 — Patient cancellation of their own appointment request, at any point before Patient
 * Confirmation. Patient Confirmation itself (turning a Hospital-Admin/System-secured doctor+slot
 * into an actually Confirmed appointment, per BR-16/BR-20/BR-21) also lives here. Unlike {@code
 * /api/admin/**}/{@code /api/doctor/**} (open — no admin/doctor auth exists yet), this is the
 * Patient's own resource and Story 1's real patient JWT auth already exists, so both endpoints
 * require it — {@code anyRequest().authenticated()} in SecurityConfig covers it without any change
 * there.
 */
@RestController
@RequestMapping("/api/patient/appointment-requests")
public class PatientAppointmentRequestController {

    private final AppointmentRequestService appointmentRequestService;

    public PatientAppointmentRequestController(AppointmentRequestService appointmentRequestService) {
        this.appointmentRequestService = appointmentRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentRequestDetailResponse create(
            @Valid @RequestBody AppointmentRequestCreateRequest body,
            @AuthenticationPrincipal PatientPrincipal principal) {
        return appointmentRequestService.create(principal.getPatientId(), body);
    }

    @GetMapping
    public List<AppointmentRequestSummaryResponse> list(@AuthenticationPrincipal PatientPrincipal principal) {
        return appointmentRequestService.listForPatient(principal.getPatientId());
    }

    @GetMapping("/{id}")
    public AppointmentRequestDetailResponse get(
            @PathVariable UUID id, @AuthenticationPrincipal PatientPrincipal principal) {
        return appointmentRequestService.getForPatient(id, principal.getPatientId());
    }

    @GetMapping("/{id}/log")
    public List<CoordinationLogEntryResponse> log(
            @PathVariable UUID id, @AuthenticationPrincipal PatientPrincipal principal) {
        return appointmentRequestService.getLogForPatient(id, principal.getPatientId());
    }

    @PostMapping("/{id}/cancel")
    public AppointmentRequestDetailResponse cancel(
            @PathVariable UUID id, @AuthenticationPrincipal PatientPrincipal principal) {
        return appointmentRequestService.cancel(id, principal.getPatientId());
    }

    @PostMapping("/{id}/confirm")
    public AppointmentRequestDetailResponse confirm(
            @PathVariable UUID id, @AuthenticationPrincipal PatientPrincipal principal) {
        return appointmentRequestService.confirm(id, principal.getPatientId());
    }
}
