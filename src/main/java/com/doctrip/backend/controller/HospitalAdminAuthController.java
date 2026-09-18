package com.doctrip.backend.controller;

import com.doctrip.backend.dto.hospitaladmin.HospitalAdminLoginRequest;
import com.doctrip.backend.dto.hospitaladmin.HospitalAdminLoginResponse;
import com.doctrip.backend.service.HospitalAdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital-admin/auth")
public class HospitalAdminAuthController {

    private final HospitalAdminAuthService hospitalAdminAuthService;

    public HospitalAdminAuthController(HospitalAdminAuthService hospitalAdminAuthService) {
        this.hospitalAdminAuthService = hospitalAdminAuthService;
    }

    @PostMapping("/login")
    public HospitalAdminLoginResponse login(@Valid @RequestBody HospitalAdminLoginRequest request) {
        return hospitalAdminAuthService.login(request);
    }
}
