package com.nupoor.payrollsync.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class CreateBatchRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Payroll period must follow YYYY-MM format")
    private String period;

    @NotBlank
    private String idempotencyKey;

    public CreateBatchRequest() {}

    public CreateBatchRequest(String period, String idempotencyKey) {
        this.period = period;
        this.idempotencyKey = idempotencyKey;
    }

    public String period() { return period; }
    public String idempotencyKey() { return idempotencyKey; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
