package com.nupoor.payrollsync.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_batch_employee", columnNames = {"batch_id", "employee_id"})
})
public class PayrollItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private PayrollBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "tax_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxDeductions;

    @Column(name = "social_security_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal socialSecurityDeductions;

    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // PENDING, PROCESSING, SUCCESS, FAILED

    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionReference;

    @Column(name = "sepa_msg_id", length = 100)
    private String sepaMsgId;

    @Column(name = "processed_at")
    private ZonedDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public PayrollItem() {}

    public PayrollItem(PayrollBatch batch, Employee employee, BigDecimal grossSalary, 
                       BigDecimal taxDeductions, BigDecimal socialSecurityDeductions, BigDecimal netSalary, String status) {
        this.batch = batch;
        this.employee = employee;
        this.grossSalary = grossSalary;
        this.taxDeductions = taxDeductions;
        this.socialSecurityDeductions = socialSecurityDeductions;
        this.netSalary = netSalary;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public PayrollBatch getBatch() { return batch; }
    public void setBatch(PayrollBatch batch) { this.batch = batch; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getTaxDeductions() { return taxDeductions; }
    public void setTaxDeductions(BigDecimal taxDeductions) { this.taxDeductions = taxDeductions; }

    public BigDecimal getSocialSecurityDeductions() { return socialSecurityDeductions; }
    public void setSocialSecurityDeductions(BigDecimal socialSecurityDeductions) { this.socialSecurityDeductions = socialSecurityDeductions; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getSepaMsgId() { return sepaMsgId; }
    public void setSepaMsgId(String sepaMsgId) { this.sepaMsgId = sepaMsgId; }

    public ZonedDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(ZonedDateTime processedAt) { this.processedAt = processedAt; }
}
