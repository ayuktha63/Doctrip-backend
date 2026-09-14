package com.doctrip.backend.doctor;

import java.util.List;

import org.springframework.stereotype.Service;

import com.doctrip.backend.common.ResourceNotFoundException;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public List<DoctorSummaryDto> getPubliclyDiscoverableDoctors() {
        return doctorRepository.findByApprovedTrue()
                .stream()
                .map(DoctorSummaryDto::from)
                .toList();
    }

    public List<DoctorSummaryDto> searchPubliclyDiscoverableDoctors(String name) {
        return doctorRepository.findByApprovedTrueAndNameContainingIgnoreCase(name)
                .stream()
                .map(DoctorSummaryDto::from)
                .toList();
    }

    public DoctorSummaryDto getPubliclyDiscoverableDoctorById(Long id) {
        return doctorRepository.findByIdAndApprovedTrue(id)
                .map(DoctorSummaryDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
    }
}
