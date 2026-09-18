package com.doctrip.backend.controller;

import com.doctrip.backend.dto.hospitaladmin.StatusToggleRequest;
import com.doctrip.backend.dto.hospitaladmin.TreatmentRequest;
import com.doctrip.backend.dto.hospitaladmin.TreatmentResponse;
import com.doctrip.backend.security.HospitalAdminPrincipal;
import com.doctrip.backend.service.HospitalTreatmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital-admin/treatments")
public class HospitalAdminTreatmentController {

    private final HospitalTreatmentService hospitalTreatmentService;

    public HospitalAdminTreatmentController(HospitalTreatmentService hospitalTreatmentService) {
        this.hospitalTreatmentService = hospitalTreatmentService;
    }

    @GetMapping
    public List<TreatmentResponse> list(@AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return hospitalTreatmentService.list(principal.getHospitalId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentResponse create(
            @AuthenticationPrincipal HospitalAdminPrincipal principal, @Valid @RequestBody TreatmentRequest request) {
        return hospitalTreatmentService.create(principal.getHospitalId(), principal.getHospitalAdminId(), request);
    }

    @PutMapping("/{id}")
    public TreatmentResponse update(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TreatmentRequest request) {
        return hospitalTreatmentService.update(
                principal.getHospitalId(), principal.getHospitalAdminId(), id, request);
    }

    @PatchMapping("/{id}/status")
    public TreatmentResponse setStatus(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable Long id,
            @RequestBody StatusToggleRequest request) {
        return hospitalTreatmentService.setListed(
                principal.getHospitalId(), principal.getHospitalAdminId(), id, request.active());
    }
}
