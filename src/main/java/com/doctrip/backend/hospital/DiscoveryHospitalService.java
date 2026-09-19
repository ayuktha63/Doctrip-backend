package com.doctrip.backend.hospital;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.doctrip.backend.common.ResourceNotFoundException;
import com.doctrip.backend.doctor.Doctor;
import com.doctrip.backend.doctor.DoctorRepository;

@Service
@Transactional(readOnly = true)
public class DiscoveryHospitalService {

    private final DiscoveryHospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;

    public DiscoveryHospitalService(DiscoveryHospitalRepository hospitalRepository, DoctorRepository doctorRepository) {
        this.hospitalRepository = hospitalRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<HospitalSummaryDto> getPubliclyDiscoverableHospitals() {
        return hospitalRepository.findByApprovedTrueAndDataHiddenFalse()
                .stream()
                .map(HospitalSummaryDto::from)
                .toList();
    }

    public List<HospitalSummaryDto> searchPubliclyDiscoverableHospitals(String name) {
        return hospitalRepository.findByApprovedTrueAndDataHiddenFalseAndNameContainingIgnoreCase(name)
                .stream()
                .map(HospitalSummaryDto::from)
                .toList();
    }

    public HospitalDetailDto getPubliclyDiscoverableHospitalById(Long id) {
        DiscoveryHospital hospital = hospitalRepository.findByIdAndApprovedTrueAndDataHiddenFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found"));
        List<Doctor> doctors = doctorRepository.findPubliclyDiscoverableByHospitalId(hospital.getId());
        return HospitalDetailDto.from(hospital, doctors);
    }
}
