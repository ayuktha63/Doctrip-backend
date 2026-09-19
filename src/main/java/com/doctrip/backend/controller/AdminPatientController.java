package com.doctrip.backend.controller;

import com.doctrip.backend.dto.AdminPatientResponse;
import com.doctrip.backend.service.AdminPatientService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** DocTrip Admin's "Users" screen — every registered patient, web or mobile (see AdminPatientResponse). */
@RestController
@RequestMapping("/api/admin/patients")
public class AdminPatientController {

    private final AdminPatientService adminPatientService;

    public AdminPatientController(AdminPatientService adminPatientService) {
        this.adminPatientService = adminPatientService;
    }

    @GetMapping
    public List<AdminPatientResponse> list() {
        return adminPatientService.list();
    }
}
