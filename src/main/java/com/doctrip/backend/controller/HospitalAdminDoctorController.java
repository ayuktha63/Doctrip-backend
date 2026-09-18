package com.doctrip.backend.controller;

import com.doctrip.backend.dto.hospitaladmin.DoctorRequest;
import com.doctrip.backend.dto.hospitaladmin.DoctorResponse;
import com.doctrip.backend.dto.hospitaladmin.StatusToggleRequest;
import com.doctrip.backend.security.HospitalAdminPrincipal;
import com.doctrip.backend.service.HospitalDoctorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital-admin/doctors")
public class HospitalAdminDoctorController {

    private final HospitalDoctorService hospitalDoctorService;

    public HospitalAdminDoctorController(HospitalDoctorService hospitalDoctorService) {
        this.hospitalDoctorService = hospitalDoctorService;
    }

    @GetMapping
    public List<DoctorResponse> list(@AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return hospitalDoctorService.list(principal.getHospitalId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorResponse create(
            @AuthenticationPrincipal HospitalAdminPrincipal principal, @Valid @RequestBody DoctorRequest request) {
        return hospitalDoctorService.create(principal.getHospitalId(), principal.getHospitalAdminId(), request);
    }

    @PutMapping("/{id}")
    public DoctorResponse update(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody DoctorRequest request) {
        return hospitalDoctorService.update(principal.getHospitalId(), principal.getHospitalAdminId(), id, request);
    }

    @PatchMapping("/{id}/status")
    public DoctorResponse setStatus(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable UUID id,
            @RequestBody StatusToggleRequest request) {
        return hospitalDoctorService.setStatus(
                principal.getHospitalId(), principal.getHospitalAdminId(), id, request.active());
    }
}
