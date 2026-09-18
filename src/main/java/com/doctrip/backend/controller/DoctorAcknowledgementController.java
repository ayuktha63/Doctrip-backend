package com.doctrip.backend.controller;

import com.doctrip.backend.dto.AppointmentRequestDetailResponse;
import com.doctrip.backend.dto.DoctorResponseRequest;
import com.doctrip.backend.service.AppointmentRequestService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * FR-06 — a doctor's response to one acknowledgement request (accept/reject/propose an
 * alternative). The Doctor's own login/acknowledgement UI is a separate story; this endpoint
 * exists so the coordination rules it triggers (FR-07/FR-08, built in this story) can actually be
 * exercised end-to-end before that story lands. No doctor authentication exists yet — same
 * no-auth posture as {@code /api/admin/**} in this backend right now.
 */
@RestController
@RequestMapping("/api/doctor/acknowledgements")
public class DoctorAcknowledgementController {

    private final AppointmentRequestService appointmentRequestService;

    public DoctorAcknowledgementController(AppointmentRequestService appointmentRequestService) {
        this.appointmentRequestService = appointmentRequestService;
    }

    @PostMapping("/{id}/respond")
    public AppointmentRequestDetailResponse respond(@PathVariable UUID id, @RequestBody DoctorResponseRequest body) {
        return appointmentRequestService.respondToAcknowledgement(id, body);
    }
}
