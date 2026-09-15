package com.doctrip.backend.controller;

import com.doctrip.backend.dto.PatientResponse;
import com.doctrip.backend.security.PatientPrincipal;
import com.doctrip.backend.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-10 — a protected client (web or mobile) resolves "who am I" the same way regardless of which
 * channel it authenticated through: the access token's subject always names the same Patient row.
 */
@RestController
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/api/patients/me")
    public ResponseEntity<PatientResponse> me(@AuthenticationPrincipal PatientPrincipal principal) {
        return ResponseEntity.ok(patientService.getById(principal.getPatientId()));
    }
}
