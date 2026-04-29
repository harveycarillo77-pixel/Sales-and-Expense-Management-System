package com.example.sales_expense_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdminCreateUserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Role name is required")
    private String roleName;

    @NotNull(message = "Inactivity timeout is required")
    @Min(value = 300, message = "Inactivity timeout must be at least 300s/5mins")
    private Integer inactivityTimeout;

    public AdminCreateUserRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getInactivityTimeout() {
        return inactivityTimeout;
    }

    public void setInactivityTimeout(Integer inactivityTimeout) {
        this.inactivityTimeout = inactivityTimeout;
    }
}