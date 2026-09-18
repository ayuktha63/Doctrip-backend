package com.doctrip.backend.controller;

import com.doctrip.backend.dto.hospitaladmin.HospitalAdminCreateRequest;
import com.doctrip.backend.dto.hospitaladmin.HospitalAdminResponse;
import com.doctrip.backend.service.HospitalAdminProvisioningService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * DocTrip-admin-driven provisioning of Hospital Admin accounts. Create + list only, no
 * self-registration. TODO: lock down once platform-admin auth exists — matches existing Story 3
 * precedent (AdminHospitalController) rather than inventing a new unrequested auth tier.
 */
@RestController
@RequestMapping("/api/admin/hospital-admins")
public class AdminHospitalAdminController {

    private final HospitalAdminProvisioningService hospitalAdminProvisioningService;

    public AdminHospitalAdminController(HospitalAdminProvisioningService hospitalAdminProvisioningService) {
        this.hospitalAdminProvisioningService = hospitalAdminProvisioningService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalAdminResponse create(@Valid @RequestBody HospitalAdminCreateRequest request) {
        return hospitalAdminProvisioningService.create(request);
    }

    @GetMapping
    public List<HospitalAdminResponse> list(@RequestParam(required = false) UUID hospitalId) {
        return hospitalAdminProvisioningService.list(hospitalId);
    }
}
