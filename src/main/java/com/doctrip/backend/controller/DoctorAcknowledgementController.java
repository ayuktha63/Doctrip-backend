package com.doctrip.backend.controller;

import com.doctrip.backend.dto.AppointmentRequestDetailResponse;
import com.doctrip.backend.dto.DoctorResponseRequest;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.service.AppointmentRequestService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    /**
     * Story 7 — the doctor's actual one-click Accept/Reject channel: a plain {@code GET} link
     * clicked straight from the acknowledgement email, which cannot carry a JSON body. Builds the
     * same {@link DoctorResponseRequest} the POST endpoint takes and calls the exact same service
     * method — no duplicated validation/atomicity logic. Renders a small HTML page (a browser
     * opens this link) rather than JSON, and turns the service's error cases into a plain sentence
     * instead of a raw error blob, since a doctor clicking a stale link should see a sentence, not
     * a 409.
     */
    @GetMapping(value = "/{id}/respond-link", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> respondViaLink(@PathVariable UUID id, @RequestParam String action) {
        try {
            appointmentRequestService.respondToAcknowledgement(id, new DoctorResponseRequest(action, null, null));
            String verb = "REJECT".equalsIgnoreCase(action) ? "declined" : "accepted";
            return ResponseEntity.ok(page("Thanks — recorded", "You've " + verb + " this appointment request."));
        } catch (NotFoundException | ConflictException e) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(page("This link is no longer valid", "This acknowledgement request has already been handled or has expired."));
        }
    }

    private String page(String title, String body) {
        return """
                <!doctype html>
                <html>
                <head><meta charset="utf-8"><title>%s</title></head>
                <body style="font-family: sans-serif; max-width: 480px; margin: 60px auto; text-align: center">
                  <h2>%s</h2>
                  <p>%s</p>
                </body>
                </html>
                """
                .formatted(title, title, body);
    }
}
