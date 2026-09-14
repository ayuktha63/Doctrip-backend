package com.doctrip.backend.controller;

import com.doctrip.backend.dto.HospitalSummaryResponse;
import com.doctrip.backend.service.HospitalService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, patient-facing view of active hospitals only. */
@RestController
@RequestMapping("/api/hospitals")
public class HospitalDiscoveryController {

    private final HospitalService hospitalService;

    public HospitalDiscoveryController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public List<HospitalSummaryResponse> discover() {
        return hospitalService.discover();
    }
}
