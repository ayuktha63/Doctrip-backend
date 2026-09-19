package com.doctrip.backend.department;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/departments")
public class DepartmentDiscoveryController {

    private final DepartmentDiscoveryService departmentDiscoveryService;

    public DepartmentDiscoveryController(DepartmentDiscoveryService departmentDiscoveryService) {
        this.departmentDiscoveryService = departmentDiscoveryService;
    }

    @GetMapping
    public List<DepartmentSummaryDto> getDepartments() {
        return departmentDiscoveryService.listDepartments();
    }

    @GetMapping("/{name}")
    public DepartmentDetailDto getDepartmentByName(@PathVariable String name) {
        return departmentDiscoveryService.getDepartmentByName(name);
    }
}
