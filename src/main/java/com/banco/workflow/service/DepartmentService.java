package com.banco.workflow.service;

import com.banco.workflow.model.Department;
import com.banco.workflow.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> getDepartments() {
        return departmentRepository.findAll();
    }

    public List<Department> getActiveDepartments() {
        return departmentRepository.findByActiveTrue();
    }

    public Optional<Department> getDepartment(String id) {
        return departmentRepository.findById(id);
    }

    public Department createDepartment(String name, String role, String description) {
        validate(name, role);
        Department department = Department.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .role(role)
                .description(description)
                .active(true)
                .build();
        return departmentRepository.save(department);
    }

    public Department updateDepartment(String id, String name, String role, String description, boolean active) {
        validate(name, role);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado: " + id));
        department.setName(name);
        department.setRole(role);
        department.setDescription(description);
        department.setActive(active);
        return departmentRepository.save(department);
    }

    public void deleteDepartment(String id) {
        departmentRepository.deleteById(id);
    }

    public boolean existsByRole(String role) {
        return departmentRepository.findByRole(role).isPresent();
    }

    private void validate(String name, String role) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del departamento es obligatorio");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("El rol del departamento es obligatorio");
        }
    }
}
