package com.example.sales_expense_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateInactivityTimeoutRequest {

    @NotNull(message = "Inactivity timeout is required")
    @Min(value = 60, message = "Inactivity timeout must be at least 60 seconds")
    private Integer inactivityTimeout;

    public UpdateInactivityTimeoutRequest() {
    }

    public Integer getInactivityTimeout() {
        return inactivityTimeout;
    }

    public void setInactivityTimeout(Integer inactivityTimeout) {
        this.inactivityTimeout = inactivityTimeout;
    }
}