package com.nupoor.payrollsync.strategy;

import java.math.BigDecimal;
import java.util.Objects;

public interface TaxCalculatorStrategy {

    String getCountryCode();

    TaxBreakdown calculateTax(BigDecimal grossSalary, String taxClass);

    class TaxBreakdown {
        private final BigDecimal incomeTax;
        private final BigDecimal solidarityTax;
        private final BigDecimal healthInsurance;
        private final BigDecimal totalDeduction;
        private final BigDecimal netSalary;

        public TaxBreakdown(BigDecimal incomeTax, BigDecimal solidarityTax, BigDecimal healthInsurance, BigDecimal totalDeduction, BigDecimal netSalary) {
            this.incomeTax = incomeTax;
            this.solidarityTax = solidarityTax;
            this.healthInsurance = healthInsurance;
            this.totalDeduction = totalDeduction;
            this.netSalary = netSalary;
        }

        public BigDecimal incomeTax() { return incomeTax; }
        public BigDecimal solidarityTax() { return solidarityTax; }
        public BigDecimal healthInsurance() { return healthInsurance; }
        public BigDecimal totalDeduction() { return totalDeduction; }
        public BigDecimal netSalary() { return netSalary; }

        public BigDecimal getIncomeTax() { return incomeTax; }
        public BigDecimal getSolidarityTax() { return solidarityTax; }
        public BigDecimal getHealthInsurance() { return healthInsurance; }
        public BigDecimal getTotalDeduction() { return totalDeduction; }
        public BigDecimal getNetSalary() { return netSalary; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TaxBreakdown breakdown = (TaxBreakdown) o;
            return Objects.equals(incomeTax, breakdown.incomeTax) &&
                   Objects.equals(solidarityTax, breakdown.solidarityTax) &&
                   Objects.equals(healthInsurance, breakdown.healthInsurance) &&
                   Objects.equals(totalDeduction, breakdown.totalDeduction) &&
                   Objects.equals(netSalary, breakdown.netSalary);
        }

        @Override
        public int hashCode() {
            return Objects.hash(incomeTax, solidarityTax, healthInsurance, totalDeduction, netSalary);
        }
    }
}
