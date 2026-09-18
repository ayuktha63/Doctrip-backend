package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.DepartmentRequest;
import com.doctrip.backend.dto.hospitaladmin.DepartmentResponse;
import com.doctrip.backend.entity.Department;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.DepartmentRepository;
import com.doctrip.backend.repository.HospitalDoctorRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final HospitalDoctorRepository hospitalDoctorRepository;
    private final HospitalAuditService hospitalAuditService;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            HospitalDoctorRepository hospitalDoctorRepository,
            HospitalAuditService hospitalAuditService) {
        this.departmentRepository = departmentRepository;
        this.hospitalDoctorRepository = hospitalDoctorRepository;
        this.hospitalAuditService = hospitalAuditService;
    }

    public List<DepartmentResponse> list(UUID hospitalId) {
        return departmentRepository.findByHospitalId(hospitalId).stream()
                .map(d -> DepartmentResponse.from(d, countDoctors(d.getId())))
                .toList();
    }

    @Transactional
    public DepartmentResponse create(UUID hospitalId, UUID actorId, DepartmentRequest request) {
        if (departmentRepository.existsByHospitalIdAndCodeIgnoreCase(hospitalId, request.code())) {
            throw new ConflictException("A department with this code already exists.");
        }

        Department department = Department.builder()
                .hospitalId(hospitalId)
                .name(request.name().trim())
                .code(request.code().trim())
                .head(request.head())
                .build();
        department = departmentRepository.save(department);

        hospitalAuditService.record(
                hospitalId, actorId, "DEPARTMENT", department.getId(), "CREATE", null,
                DepartmentResponse.from(department, 0));

        return DepartmentResponse.from(department, 0);
    }

    @Transactional
    public DepartmentResponse update(UUID hospitalId, UUID actorId, UUID id, DepartmentRequest request) {
        Department department = findScoped(hospitalId, id);

        if (departmentRepository.existsByHospitalIdAndCodeIgnoreCaseAndIdNot(hospitalId, request.code(), id)) {
            throw new ConflictException("A department with this code already exists.");
        }

        long doctorCount = countDoctors(id);
        DepartmentResponse before = DepartmentResponse.from(department, doctorCount);

        department.setName(request.name().trim());
        department.setCode(request.code().trim());
        department.setHead(request.head());
        department = departmentRepository.save(department);

        DepartmentResponse after = DepartmentResponse.from(department, doctorCount);
        hospitalAuditService.record(hospitalId, actorId, "DEPARTMENT", id, "UPDATE", before, after);

        return after;
    }

    @Transactional
    public DepartmentResponse setStatus(UUID hospitalId, UUID actorId, UUID id, boolean active) {
        Department department = findScoped(hospitalId, id);
        long doctorCount = countDoctors(id);
        DepartmentResponse before = DepartmentResponse.from(department, doctorCount);

        department.setActive(active);
        department = departmentRepository.save(department);

        DepartmentResponse after = DepartmentResponse.from(department, doctorCount);
        hospitalAuditService.record(hospitalId, actorId, "DEPARTMENT", id, "STATUS_TOGGLE", before, after);

        return after;
    }

    private Department findScoped(UUID hospitalId, UUID id) {
        Department department =
                departmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Department not found."));
        if (!department.getHospitalId().equals(hospitalId)) {
            throw new NotFoundException("Department not found.");
        }
        return department;
    }

    private long countDoctors(UUID departmentId) {
        return hospitalDoctorRepository.countByDepartmentId(departmentId);
    }
}
