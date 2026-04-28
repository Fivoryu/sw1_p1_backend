package com.banco.workflow.controller;

import com.banco.workflow.model.Department;
import com.banco.workflow.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<Department>> listDepartments() {
        return ResponseEntity.ok(departmentService.getDepartments());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody Map<String, Object> payload) {
        Department department = departmentService.createDepartment(
                String.valueOf(payload.getOrDefault("name", "")),
                String.valueOf(payload.getOrDefault("role", "")),
                String.valueOf(payload.getOrDefault("description", ""))
        );
        return ResponseEntity.ok(department);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        Department department = departmentService.updateDepartment(
                id,
                String.valueOf(payload.getOrDefault("name", "")),
                String.valueOf(payload.getOrDefault("role", "")),
                String.valueOf(payload.getOrDefault("description", "")),
                Boolean.parseBoolean(String.valueOf(payload.getOrDefault("active", true)))
        );
        return ResponseEntity.ok(department);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
