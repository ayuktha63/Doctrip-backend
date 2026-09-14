package com.doctrip.backend.doctor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public List<DoctorSummaryDto> getDoctors(@RequestParam(required = false) String name) {
        if (name == null || name.isBlank()) {
            return doctorService.getPubliclyDiscoverableDoctors();
        }
        return doctorService.searchPubliclyDiscoverableDoctors(name);
    }

    @GetMapping("/{id}")
    public DoctorSummaryDto getDoctorById(@PathVariable Long id) {
        return doctorService.getPubliclyDiscoverableDoctorById(id);
    }
}
