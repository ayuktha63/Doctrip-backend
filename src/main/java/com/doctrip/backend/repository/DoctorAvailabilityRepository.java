package com.doctrip.backend.repository;

import com.doctrip.backend.entity.DoctorAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, UUID> {

    List<DoctorAvailability> findByDoctorId(UUID doctorId);

    List<DoctorAvailability> findByDoctorIdAndSlotDate(UUID doctorId, LocalDate slotDate);

    List<DoctorAvailability> findByDoctorIdAndSlotDateBetween(UUID doctorId, LocalDate start, LocalDate end);
}
