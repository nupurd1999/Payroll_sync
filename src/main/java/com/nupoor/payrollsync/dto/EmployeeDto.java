package com.nupoor.payrollsync.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

public class EmployeeDto {

    @NotBlank
    private String employeeCode;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String iban;

    @NotBlank
    private String bic;

    @NotBlank
    private String taxClass;

    @NotNull
    @Positive
    private BigDecimal baseSalary;

    private String countryCode;
    private String currency;

    public EmployeeDto() {}

    public EmployeeDto(String employeeCode, String firstName, String lastName, String email, 
                       String iban, String bic, String taxClass, BigDecimal baseSalary, 
                       String countryCode, String currency) {
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.iban = iban;
        this.bic = bic;
        this.taxClass = taxClass;
        this.baseSalary = baseSalary;
        this.countryCode = countryCode;
        this.currency = currency;
    }

    public String employeeCode() { return employeeCode; }
    public String firstName() { return firstName; }
    public String lastName() { return lastName; }
    public String email() { return email; }
    public String iban() { return iban; }
    public String bic() { return bic; }
    public String taxClass() { return taxClass; }
    public BigDecimal baseSalary() { return baseSalary; }
    public String countryCode() { return countryCode; }
    public String currency() { return currency; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }

    public String getTaxClass() { return taxClass; }
    public void setTaxClass(String taxClass) { this.taxClass = taxClass; }

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
