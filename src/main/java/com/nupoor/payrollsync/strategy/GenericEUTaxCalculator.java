package com.nupoor.payrollsync.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GenericEUTaxCalculator implements TaxCalculatorStrategy {

    @Override
    public String getCountryCode() {
        return "GENERIC_EU";
    }

    @Override
    public TaxBreakdown calculateTax(BigDecimal grossSalary, String taxClass) {
        if (grossSalary == null || grossSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return new TaxBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal incomeTax = grossSalary.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal solidarityTax = BigDecimal.ZERO;
        BigDecimal socialSecurity = grossSalary.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDeduction = incomeTax.add(socialSecurity);
        BigDecimal netSalary = grossSalary.subtract(totalDeduction).setScale(2, RoundingMode.HALF_UP);

        return new TaxBreakdown(incomeTax, solidarityTax, socialSecurity, totalDeduction, netSalary);
    }
}
