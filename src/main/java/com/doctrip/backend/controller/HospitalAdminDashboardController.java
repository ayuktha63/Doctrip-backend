package com.doctrip.backend.controller;

import com.doctrip.backend.dto.hospitaladmin.DashboardSummaryResponse;
import com.doctrip.backend.security.HospitalAdminPrincipal;
import com.doctrip.backend.service.DashboardSummaryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospital-admin/dashboard")
public class HospitalAdminDashboardController {

    private final DashboardSummaryService dashboardSummaryService;

    public HospitalAdminDashboardController(DashboardSummaryService dashboardSummaryService) {
        this.dashboardSummaryService = dashboardSummaryService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal HospitalAdminPrincipal principal) {
        return dashboardSummaryService.summarize(principal.getHospitalId());
    }
}
