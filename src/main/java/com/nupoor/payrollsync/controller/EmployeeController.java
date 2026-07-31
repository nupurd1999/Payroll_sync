package com.nupoor.payrollsync.controller;

import com.nupoor.payrollsync.dto.EmployeeDto;
import com.nupoor.payrollsync.entity.Employee;
import com.nupoor.payrollsync.repository.EmployeeRepository;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody EmployeeDto dto) {
        Employee employee = new Employee(
                null,
                dto.employeeCode(),
                dto.firstName(),
                dto.lastName(),
                dto.email(),
                dto.iban(),
                dto.bic(),
                dto.taxClass(),
                dto.baseSalary(),
                dto.countryCode() != null ? dto.countryCode() : "DE",
                dto.currency() != null ? dto.currency() : "EUR"
        );
        Employee saved = employeeRepository.save(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable UUID id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
