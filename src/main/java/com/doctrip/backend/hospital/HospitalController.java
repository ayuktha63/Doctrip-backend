package com.doctrip.backend.hospital;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/hospitals")
public class HospitalController {

    private final DiscoveryHospitalService hospitalService;

    public HospitalController(DiscoveryHospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public List<HospitalSummaryDto> getHospitals(@RequestParam(required = false) String name) {
        if (name == null || name.isBlank()) {
            return hospitalService.getPubliclyDiscoverableHospitals();
        }
        return hospitalService.searchPubliclyDiscoverableHospitals(name);
    }

    @GetMapping("/{id}")
    public HospitalDetailDto getHospitalById(@PathVariable Long id) {
        return hospitalService.getPubliclyDiscoverableHospitalById(id);
    }
}
