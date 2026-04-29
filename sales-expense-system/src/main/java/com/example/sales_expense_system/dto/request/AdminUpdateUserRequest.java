package com.example.sales_expense_system.dto.request;

import jakarta.validation.constraints.Min;

public class AdminUpdateUserRequest {

    private String username;
    private String roleName;
    private String status;

    @Min(value = 300, message = "Inactivity timeout must be at least 300s/5mins")
    private Integer inactivityTimeout;

    public AdminUpdateUserRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getInactivityTimeout() {
        return inactivityTimeout;
    }

    public void setInactivityTimeout(Integer inactivityTimeout) {
        this.inactivityTimeout = inactivityTimeout;
    }
}