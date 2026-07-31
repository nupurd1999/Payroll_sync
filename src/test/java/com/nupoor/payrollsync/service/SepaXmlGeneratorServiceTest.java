package com.nupoor.payrollsync.service;

import com.nupoor.payrollsync.entity.Employee;
import com.nupoor.payrollsync.entity.PayrollBatch;
import com.nupoor.payrollsync.entity.PayrollItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SepaXmlGeneratorServiceTest {

    private SepaXmlGeneratorService sepaXmlGeneratorService;

    @BeforeEach
    void setUp() {
        sepaXmlGeneratorService = new SepaXmlGeneratorService(
                "Enterprise PayrollSync Corp",
                "Enterprise PayrollSync Corp HQ",
                "DE89370400440532013000",
                "COBA234XXX"
        );
    }

    @Test
    @DisplayName("Should generate valid SEPA ISO 20022 pain.001.001.03 XML string")
    void testSepaXmlGeneration() {
        PayrollBatch batch = new PayrollBatch(UUID.randomUUID(), "BATCH-2026-07-001", "2026-07", "CALCULATED", "IDEM-1001");
        batch.setTotalNet(new BigDecimal("3369.00"));

        Employee employee = new Employee(UUID.randomUUID(), "EMP-001", "John", "Doe", "john.doe@example.com",
                "DE89370400440532013001", "DBEK234XXX", "CLASS_1", new BigDecimal("5000.00"), "DE", "EUR");

        PayrollItem item = new PayrollItem(batch, employee, new BigDecimal("5000.00"),
                new BigDecimal("1266.00"), new BigDecimal("365.00"), new BigDecimal("3369.00"), "PENDING");
        item.setId(UUID.randomUUID());

        String xml = sepaXmlGeneratorService.generatePain001Xml(batch, List.of(item));

        assertNotNull(xml);
        assertTrue(xml.contains("urn:iso:std:iso:20022:tech:xsd:pain.001.001.03"));
        assertTrue(xml.contains("<Nm>Enterprise PayrollSync Corp HQ</Nm>"));
        assertTrue(xml.contains("<IBAN>DE89370400440532013000</IBAN>"));
        assertTrue(xml.contains("<IBAN>DE89370400440532013001</IBAN>"));
        assertTrue(xml.contains("<InstdAmt Ccy=\"EUR\">3369.00</InstdAmt>"));
        assertTrue(xml.contains("Salary Payout Period 2026-07 Ref EMP-001"));
    }
}
