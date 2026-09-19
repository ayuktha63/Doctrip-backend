package com.doctrip.backend.doctor;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.doctrip.backend.common.ResourceNotFoundException;
import com.doctrip.backend.repository.DoctorAvailabilityRepository;

@Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;

    public DoctorService(DoctorRepository doctorRepository, DoctorAvailabilityRepository doctorAvailabilityRepository) {
        this.doctorRepository = doctorRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
    }

    public List<DoctorSummaryDto> getPubliclyDiscoverableDoctors() {
        return doctorRepository.findPubliclyDiscoverable()
                .stream()
                .map(DoctorSummaryDto::from)
                .toList();
    }

    public List<DoctorSummaryDto> searchPubliclyDiscoverableDoctors(String name) {
        return doctorRepository.findPubliclyDiscoverableByNameContaining(name)
                .stream()
                .map(DoctorSummaryDto::from)
                .toList();
    }

    public DoctorDetailDto getPubliclyDiscoverableDoctorById(Long id) {
        Doctor doctor = doctorRepository.findPubliclyDiscoverableById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        List<Doctor> colleagues = doctor.getHospital() != null
                ? doctorRepository.findPubliclyDiscoverableByHospitalIdAndIdNot(doctor.getHospital().getId(), doctor.getId())
                : List.of();
        return DoctorDetailDto.from(doctor, colleagues);
    }

    /**
     * Real, hospital-admin-published open slots for a publicly discoverable doctor — resolved via
     * {@code Doctor.hospitalDoctorId} to Story 4's {@code entity.HospitalDoctor}/{@code
     * DoctorAvailability} rows. Only future slots are returned; the patient picks one of these as
     * their preferred date/time when booking, so what they choose from is real, not simulated.
     */
    public List<DoctorAvailabilitySlotDto> getAvailability(Long id) {
        Doctor doctor = doctorRepository.findPubliclyDiscoverableById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        if (doctor.getHospitalDoctorId() == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return doctorAvailabilityRepository.findByDoctorId(doctor.getHospitalDoctorId()).stream()
                .filter(a -> !a.getSlotDate().isBefore(today))
                .sorted(Comparator.comparing(com.doctrip.backend.entity.DoctorAvailability::getSlotDate)
                        .thenComparing(com.doctrip.backend.entity.DoctorAvailability::getStartTime))
                .map(DoctorAvailabilitySlotDto::from)
                .toList();
    }
}
