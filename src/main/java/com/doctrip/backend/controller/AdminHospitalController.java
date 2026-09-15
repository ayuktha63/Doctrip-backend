package com.doctrip.backend.controller;

import com.doctrip.backend.dto.HospitalRequest;
import com.doctrip.backend.dto.HospitalResponse;
import com.doctrip.backend.dto.HospitalStatusRequest;
import com.doctrip.backend.service.HospitalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * DocTrip Admin's hospital master-record management. No admin authentication is built for this
 * story — the reference mockup (Main.dc.html) has no login screen and starts pre-authenticated;
 * admin auth is its own future story.
 */
@RestController
@RequestMapping("/api/admin/hospitals")
public class AdminHospitalController {

    private final HospitalService hospitalService;

    public AdminHospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalResponse create(@Valid @RequestBody HospitalRequest request) {
        return hospitalService.create(request);
    }

    @GetMapping
    public List<HospitalResponse> list() {
        return hospitalService.list();
    }

    @GetMapping("/{id}")
    public HospitalResponse get(@PathVariable UUID id) {
        return hospitalService.get(id);
    }

    @PutMapping("/{id}")
    public HospitalResponse update(@PathVariable UUID id, @Valid @RequestBody HospitalRequest request) {
        return hospitalService.update(id, request);
    }

    @PostMapping("/{id}/status")
    public HospitalResponse setStatus(@PathVariable UUID id, @RequestBody HospitalStatusRequest request) {
        return hospitalService.setActive(id, request.active());
    }
}
