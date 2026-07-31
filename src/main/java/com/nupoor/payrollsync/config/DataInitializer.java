package com.nupoor.payrollsync.config;

import com.nupoor.payrollsync.entity.Employee;
import com.nupoor.payrollsync.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final EmployeeRepository employeeRepository;

    public DataInitializer(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.count() == 0) {
            log.info("Seeding initial demo employees into database...");

            Employee emp1 = new Employee(
                    null,
                    "EMP-DE-001",
                    "Nupoor",
                    "Dhamal",
                    "nupoor.dhamal@example.com",
                    "DE89370400440532013001",
                    "DBEK234XXX",
                    "CLASS_1",
                    new BigDecimal("6500.00"),
                    "DE",
                    "EUR"
            );

            Employee emp2 = new Employee(
                    null,
                    "EMP-DE-002",
                    "Alex",
                    "Weber",
                    "alex.weber@example.com",
                    "DE89370400440532013002",
                    "COBA234XXX",
                    "CLASS_3",
                    new BigDecimal("7500.00"),
                    "DE",
                    "EUR"
            );

            employeeRepository.save(emp1);
            employeeRepository.save(emp2);

            log.info("Demo employees seeded successfully.");
        }
    }
}
