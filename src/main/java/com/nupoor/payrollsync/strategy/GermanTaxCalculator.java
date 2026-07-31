package com.nupoor.payrollsync.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GermanTaxCalculator implements TaxCalculatorStrategy {

    @Override
    public String getCountryCode() {
        return "DE";
    }

    @Override
    public TaxBreakdown calculateTax(BigDecimal grossSalary, String taxClass) {
        if (grossSalary == null || grossSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return new TaxBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        String normalizedClass = taxClass != null ? taxClass.toUpperCase().trim() : "CLASS_1";

        BigDecimal incomeTaxRate;
        switch (normalizedClass) {
            case "CLASS_1":
            case "TAX_CLASS_1":
                incomeTaxRate = new BigDecimal("0.24");
                break;
            case "CLASS_2":
            case "TAX_CLASS_2":
                incomeTaxRate = new BigDecimal("0.21");
                break;
            case "CLASS_3":
            case "TAX_CLASS_3":
                incomeTaxRate = new BigDecimal("0.16");
                break;
            case "CLASS_4":
            case "TAX_CLASS_4":
                incomeTaxRate = new BigDecimal("0.24");
                break;
            case "CLASS_5":
            case "TAX_CLASS_5":
                incomeTaxRate = new BigDecimal("0.32");
                break;
            case "CLASS_6":
            case "TAX_CLASS_6":
                incomeTaxRate = new BigDecimal("0.36");
                break;
            default:
                incomeTaxRate = new BigDecimal("0.25");
                break;
        }

        BigDecimal incomeTax = grossSalary.multiply(incomeTaxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal solidarityTax = incomeTax.multiply(new BigDecimal("0.055")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal healthInsurance = grossSalary.multiply(new BigDecimal("0.073")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDeduction = incomeTax.add(solidarityTax).add(healthInsurance);
        BigDecimal netSalary = grossSalary.subtract(totalDeduction).setScale(2, RoundingMode.HALF_UP);

        return new TaxBreakdown(incomeTax, solidarityTax, healthInsurance, totalDeduction, netSalary);
    }
}
