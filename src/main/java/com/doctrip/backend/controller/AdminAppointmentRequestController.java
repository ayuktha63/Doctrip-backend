package com.doctrip.backend.controller;

import com.doctrip.backend.dto.AppointmentRequestDetailResponse;
import com.doctrip.backend.dto.AppointmentRequestSummaryResponse;
import com.doctrip.backend.dto.CoordinationLogEntryResponse;
import com.doctrip.backend.dto.DoctorAvailabilityResponse;
import com.doctrip.backend.service.AppointmentRequestService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * FR-01 — Hospital Admin reviewing appointment requests routed to their hospital. No admin
 * authentication is built for this story (see backend CLAUDE.md) — {@code hospitalId} is passed
 * explicitly by the caller rather than resolved from a session.
 */
@RestController
@RequestMapping("/api/admin/appointment-requests")
public class AdminAppointmentRequestController {

    private final AppointmentRequestService appointmentRequestService;

    public AdminAppointmentRequestController(AppointmentRequestService appointmentRequestService) {
        this.appointmentRequestService = appointmentRequestService;
    }

    @GetMapping
    public List<AppointmentRequestSummaryResponse> list(@RequestParam UUID hospitalId) {
        return appointmentRequestService.listForHospital(hospitalId);
    }

    @GetMapping("/{id}")
    public AppointmentRequestDetailResponse get(@PathVariable UUID id) {
        return appointmentRequestService.get(id);
    }

    @GetMapping("/{id}/doctor-availability")
    public DoctorAvailabilityResponse doctorAvailability(@PathVariable UUID id) {
        return appointmentRequestService.getDoctorAvailability(id);
    }

    @GetMapping("/{id}/log")
    public List<CoordinationLogEntryResponse> log(@PathVariable UUID id) {
        return appointmentRequestService.getLog(id);
    }

    @PostMapping("/{id}/coordination/start")
    public AppointmentRequestDetailResponse startCoordination(@PathVariable UUID id) {
        return appointmentRequestService.startCoordination(id);
    }

    @PostMapping("/{id}/coordination/resolve-alternative")
    public AppointmentRequestDetailResponse resolveAlternative(@PathVariable UUID id) {
        return appointmentRequestService.resolveAlternative(id);
    }
}
