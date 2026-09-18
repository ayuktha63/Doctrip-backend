package com.doctrip.backend.controller;

import com.doctrip.backend.dto.hospitaladmin.HospitalAdminHospitalResponse;
import com.doctrip.backend.dto.hospitaladmin.OperationalSettingsRequest;
import com.doctrip.backend.dto.hospitaladmin.OperationalSettingsResponse;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.HospitalRepository;
import com.doctrip.backend.security.HospitalAdminPrincipal;
import com.doctrip.backend.service.HospitalOperationalSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital-admin/hospital")
public class HospitalAdminHospitalController {

    private final HospitalRepository hospitalRepository;
    private final HospitalOperationalSettingsService hospitalOperationalSettingsService;

    public HospitalAdminHospitalController(
            HospitalRepository hospitalRepository,
            HospitalOperationalSettingsService hospitalOperationalSettingsService) {
        this.hospitalRepository = hospitalRepository;
        this.hospitalOperationalSettingsService = hospitalOperationalSettingsService;
    }

    @GetMapping
    public HospitalAdminHospitalResponse get(@AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return HospitalAdminHospitalResponse.from(hospitalRepository
                .findById(principal.getHospitalId())
                .orElseThrow(() -> new NotFoundException("Hospital not found.")));
    }

    @GetMapping("/operational-settings")
    public OperationalSettingsResponse getOperationalSettings(
            @AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return hospitalOperationalSettingsService.get(principal.getHospitalId());
    }

    @PutMapping("/operational-settings")
    public OperationalSettingsResponse updateOperationalSettings(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @Valid @RequestBody OperationalSettingsRequest request) {
        return hospitalOperationalSettingsService.upsert(
                principal.getHospitalId(), principal.getHospitalAdminId(), request);
    }
}
