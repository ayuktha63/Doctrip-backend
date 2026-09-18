package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.AvailabilityRequest;
import com.doctrip.backend.dto.hospitaladmin.AvailabilityResponse;
import com.doctrip.backend.entity.DoctorAvailability;
import com.doctrip.backend.entity.HospitalDoctor;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.DoctorAvailabilityRepository;
import com.doctrip.backend.repository.HospitalDoctorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorAvailabilityService {

    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final HospitalDoctorRepository hospitalDoctorRepository;
    private final HospitalAuditService hospitalAuditService;

    public DoctorAvailabilityService(
            DoctorAvailabilityRepository doctorAvailabilityRepository,
            HospitalDoctorRepository hospitalDoctorRepository,
            HospitalAuditService hospitalAuditService) {
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.hospitalDoctorRepository = hospitalDoctorRepository;
        this.hospitalAuditService = hospitalAuditService;
    }

    /**
     * {@code weekStart} is optional — the frontend fetches every slot for a doctor and groups
     * them into weeks client-side (its own weekly grid navigation), so this never actually
     * receives one in practice. When absent, returns everything for the doctor rather than
     * requiring a filter the caller never sends.
     */
    public List<AvailabilityResponse> list(UUID hospitalId, UUID doctorId, LocalDate weekStart) {
        findDoctorScoped(hospitalId, doctorId);
        List<DoctorAvailability> slots = weekStart == null
                ? doctorAvailabilityRepository.findByDoctorId(doctorId)
                : doctorAvailabilityRepository.findByDoctorIdAndSlotDateBetween(doctorId, weekStart, weekStart.plusDays(6));
        return slots.stream().map(AvailabilityResponse::from).toList();
    }

    @Transactional
    public AvailabilityResponse create(UUID hospitalId, UUID actorId, UUID doctorId, AvailabilityRequest request) {
        findDoctorScoped(hospitalId, doctorId);

        List<DoctorAvailability> existingOnDate =
                doctorAvailabilityRepository.findByDoctorIdAndSlotDate(doctorId, request.slotDate());
        boolean conflicts = existingOnDate.stream()
                .anyMatch(existing -> existing.getStartTime().isBefore(request.endTime())
                        && request.startTime().isBefore(existing.getEndTime()));
        if (conflicts) {
            throw new ConflictException("This availability slot overlaps with an existing slot for this doctor.");
        }

        DoctorAvailability availability = DoctorAvailability.builder()
                .hospitalId(hospitalId)
                .doctorId(doctorId)
                .slotDate(request.slotDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .kind(request.kind())
                .build();
        availability = doctorAvailabilityRepository.save(availability);

        AvailabilityResponse after = AvailabilityResponse.from(availability);
        hospitalAuditService.record(
                hospitalId, actorId, "DOCTOR_AVAILABILITY", availability.getId(), "CREATE", null, after);
        return after;
    }

    @Transactional
    public void delete(UUID hospitalId, UUID actorId, UUID doctorId, UUID id) {
        findDoctorScoped(hospitalId, doctorId);
        DoctorAvailability availability = doctorAvailabilityRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Availability slot not found."));
        if (!availability.getHospitalId().equals(hospitalId) || !availability.getDoctorId().equals(doctorId)) {
            throw new NotFoundException("Availability slot not found.");
        }

        AvailabilityResponse before = AvailabilityResponse.from(availability);
        doctorAvailabilityRepository.delete(availability);

        hospitalAuditService.record(hospitalId, actorId, "DOCTOR_AVAILABILITY", id, "DELETE", before, null);
    }

    private HospitalDoctor findDoctorScoped(UUID hospitalId, UUID doctorId) {
        HospitalDoctor doctor = hospitalDoctorRepository
                .findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor not found."));
        if (!doctor.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Doctor not found.");
        }
        return doctor;
    }
}
