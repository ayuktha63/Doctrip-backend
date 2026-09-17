package com.doctrip.backend.doctor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.doctrip.backend.common.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
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

    public DoctorDetailDto getPubliclyDiscoverableDoctorById(Long id) {
        Doctor doctor = doctorRepository.findByIdAndApprovedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        List<Doctor> colleagues = doctor.getHospital() != null
                ? doctorRepository.findByApprovedTrueAndHospitalIdAndIdNot(doctor.getHospital().getId(), doctor.getId())
                : List.of();
        return DoctorDetailDto.from(doctor, colleagues);
    }
}
