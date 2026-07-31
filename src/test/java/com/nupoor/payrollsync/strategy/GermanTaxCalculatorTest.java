package com.nupoor.payrollsync.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GermanTaxCalculatorTest {

    private GermanTaxCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new GermanTaxCalculator();
    }

    @Test
    @DisplayName("Should return DE country code")
    void testCountryCode() {
        assertEquals("DE", calculator.getCountryCode());
    }

    @Test
    @DisplayName("Should calculate German Tax Class 1 deductions correctly")
    void testTaxClass1Calculation() {
        BigDecimal grossSalary = new BigDecimal("5000.00");
        TaxCalculatorStrategy.TaxBreakdown breakdown = calculator.calculateTax(grossSalary, "CLASS_1");

        // Income tax: 5000 * 0.24 = 1200.00
        assertEquals(new BigDecimal("1200.00"), breakdown.incomeTax());
        // Solidarity tax: 1200 * 0.055 = 66.00
        assertEquals(new BigDecimal("66.00"), breakdown.solidarityTax());
        // Health insurance: 5000 * 0.073 = 365.00
        assertEquals(new BigDecimal("365.00"), breakdown.healthInsurance());

        // Total deduction: 1200 + 66 + 365 = 1631.00
        assertEquals(new BigDecimal("1631.00"), breakdown.totalDeduction());
        // Net salary: 5000 - 1631 = 3369.00
        assertEquals(new BigDecimal("3369.00"), breakdown.netSalary());
    }

    @Test
    @DisplayName("Should calculate Tax Class 3 lower tax rate correctly")
    void testTaxClass3Calculation() {
        BigDecimal grossSalary = new BigDecimal("6000.00");
        TaxCalculatorStrategy.TaxBreakdown breakdown = calculator.calculateTax(grossSalary, "CLASS_3");

        // Income tax: 6000 * 0.16 = 960.00
        assertEquals(new BigDecimal("960.00"), breakdown.incomeTax());
        // Solidarity tax: 960 * 0.055 = 52.80
        assertEquals(new BigDecimal("52.80"), breakdown.solidarityTax());
        // Health insurance: 6000 * 0.073 = 438.00
        assertEquals(new BigDecimal("438.00"), breakdown.healthInsurance());
    }

    @Test
    @DisplayName("Should handle null or zero salary gracefully")
    void testZeroSalary() {
        TaxCalculatorStrategy.TaxBreakdown breakdown = calculator.calculateTax(BigDecimal.ZERO, "CLASS_1");
        assertEquals(BigDecimal.ZERO, breakdown.netSalary());
    }
}
