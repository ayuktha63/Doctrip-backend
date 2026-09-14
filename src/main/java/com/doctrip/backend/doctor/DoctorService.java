package com.doctrip.backend.doctor;

import java.util.List;

import org.springframework.stereotype.Service;

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
}
