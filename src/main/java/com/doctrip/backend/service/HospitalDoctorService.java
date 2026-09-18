package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.DoctorRequest;
import com.doctrip.backend.dto.hospitaladmin.DoctorResponse;
import com.doctrip.backend.entity.Department;
import com.doctrip.backend.entity.HospitalDoctor;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.DepartmentRepository;
import com.doctrip.backend.repository.HospitalDoctorRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalDoctorService {

    private final HospitalDoctorRepository hospitalDoctorRepository;
    private final DepartmentRepository departmentRepository;
    private final HospitalAuditService hospitalAuditService;

    public HospitalDoctorService(
            HospitalDoctorRepository hospitalDoctorRepository,
            DepartmentRepository departmentRepository,
            HospitalAuditService hospitalAuditService) {
        this.hospitalDoctorRepository = hospitalDoctorRepository;
        this.departmentRepository = departmentRepository;
        this.hospitalAuditService = hospitalAuditService;
    }

    public List<DoctorResponse> list(UUID hospitalId) {
        List<HospitalDoctor> doctors = hospitalDoctorRepository.findByHospitalId(hospitalId);
        Map<UUID, String> departmentNames = departmentRepository.findByHospitalId(hospitalId).stream()
                .collect(java.util.stream.Collectors.toMap(Department::getId, Department::getName));
        return doctors.stream()
                .map(d -> DoctorResponse.from(d, departmentNames.get(d.getDepartmentId())))
                .toList();
    }

    @Transactional
    public DoctorResponse create(UUID hospitalId, UUID actorId, DoctorRequest request) {
        Department department = findDepartmentScoped(hospitalId, request.departmentId());
        if (hospitalDoctorRepository.existsByHospitalIdAndNameIgnoreCase(hospitalId, request.name())) {
            throw new ConflictException("A doctor with this name already exists.");
        }

        HospitalDoctor doctor = HospitalDoctor.builder()
                .hospitalId(hospitalId)
                .departmentId(request.departmentId())
                .name(request.name().trim())
                .specialty(request.specialty())
                .experienceYears(request.experienceYears())
                .build();
        doctor = hospitalDoctorRepository.save(doctor);

        DoctorResponse after = DoctorResponse.from(doctor, department.getName());
        hospitalAuditService.record(hospitalId, actorId, "DOCTOR", doctor.getId(), "CREATE", null, after);
        return after;
    }

    @Transactional
    public DoctorResponse update(UUID hospitalId, UUID actorId, UUID id, DoctorRequest request) {
        HospitalDoctor doctor = findScoped(hospitalId, id);
        Department department = findDepartmentScoped(hospitalId, request.departmentId());

        if (hospitalDoctorRepository.existsByHospitalIdAndNameIgnoreCaseAndIdNot(hospitalId, request.name(), id)) {
            throw new ConflictException("A doctor with this name already exists.");
        }

        String previousDepartmentName = departmentRepository
                .findById(doctor.getDepartmentId())
                .map(Department::getName)
                .orElse(null);
        DoctorResponse before = DoctorResponse.from(doctor, previousDepartmentName);

        doctor.setName(request.name().trim());
        doctor.setDepartmentId(request.departmentId());
        doctor.setSpecialty(request.specialty());
        doctor.setExperienceYears(request.experienceYears());
        doctor = hospitalDoctorRepository.save(doctor);

        DoctorResponse after = DoctorResponse.from(doctor, department.getName());
        hospitalAuditService.record(hospitalId, actorId, "DOCTOR", id, "UPDATE", before, after);
        return after;
    }

    @Transactional
    public DoctorResponse setStatus(UUID hospitalId, UUID actorId, UUID id, boolean active) {
        HospitalDoctor doctor = findScoped(hospitalId, id);
        String departmentName = departmentRepository
                .findById(doctor.getDepartmentId())
                .map(Department::getName)
                .orElse(null);
        DoctorResponse before = DoctorResponse.from(doctor, departmentName);

        doctor.setActive(active);
        doctor = hospitalDoctorRepository.save(doctor);

        DoctorResponse after = DoctorResponse.from(doctor, departmentName);
        hospitalAuditService.record(hospitalId, actorId, "DOCTOR", id, "STATUS_TOGGLE", before, after);
        return after;
    }

    private HospitalDoctor findScoped(UUID hospitalId, UUID id) {
        HospitalDoctor doctor =
                hospitalDoctorRepository.findById(id).orElseThrow(() -> new NotFoundException("Doctor not found."));
        if (!doctor.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Doctor not found.");
        }
        return doctor;
    }

    private Department findDepartmentScoped(UUID hospitalId, UUID departmentId) {
        Department department = departmentRepository
                .findById(departmentId)
                .orElseThrow(() -> new NotFoundException("Department not found."));
        if (!department.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Department not found.");
        }
        return department;
    }
}
