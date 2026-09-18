package com.doctrip.backend.controller;

import com.doctrip.backend.dto.hospitaladmin.AvailabilityRequest;
import com.doctrip.backend.dto.hospitaladmin.AvailabilityResponse;
import com.doctrip.backend.security.HospitalAdminPrincipal;
import com.doctrip.backend.service.DoctorAvailabilityService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital-admin/doctors/{doctorId}/availability")
public class HospitalAdminAvailabilityController {

    private final DoctorAvailabilityService doctorAvailabilityService;

    public HospitalAdminAvailabilityController(DoctorAvailabilityService doctorAvailabilityService) {
        this.doctorAvailabilityService = doctorAvailabilityService;
    }

    @GetMapping
    public List<AvailabilityResponse> list(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable UUID doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return doctorAvailabilityService.list(principal.getHospitalId(), doctorId, weekStart);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityResponse create(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable UUID doctorId,
            @Valid @RequestBody AvailabilityRequest request) {
        return doctorAvailabilityService.create(
                principal.getHospitalId(), principal.getHospitalAdminId(), doctorId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable UUID doctorId,
            @PathVariable UUID id) {
        doctorAvailabilityService.delete(principal.getHospitalId(), principal.getHospitalAdminId(), doctorId, id);
    }
}
