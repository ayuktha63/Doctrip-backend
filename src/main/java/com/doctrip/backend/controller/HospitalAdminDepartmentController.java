package com.doctrip.backend.controller;

import com.doctrip.backend.dto.hospitaladmin.DepartmentRequest;
import com.doctrip.backend.dto.hospitaladmin.DepartmentResponse;
import com.doctrip.backend.dto.hospitaladmin.StatusToggleRequest;
import com.doctrip.backend.security.HospitalAdminPrincipal;
import com.doctrip.backend.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital-admin/departments")
public class HospitalAdminDepartmentController {

    private final DepartmentService departmentService;

    public HospitalAdminDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<DepartmentResponse> list(@AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return departmentService.list(principal.getHospitalId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(
            @AuthenticationPrincipal HospitalAdminPrincipal principal, @Valid @RequestBody DepartmentRequest request) {
        return departmentService.create(principal.getHospitalId(), principal.getHospitalAdminId(), request);
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(principal.getHospitalId(), principal.getHospitalAdminId(), id, request);
    }

    @PatchMapping("/{id}/status")
    public DepartmentResponse setStatus(
            @AuthenticationPrincipal HospitalAdminPrincipal principal,
            @PathVariable UUID id,
            @RequestBody StatusToggleRequest request) {
        return departmentService.setStatus(
                principal.getHospitalId(), principal.getHospitalAdminId(), id, request.active());
    }
}
