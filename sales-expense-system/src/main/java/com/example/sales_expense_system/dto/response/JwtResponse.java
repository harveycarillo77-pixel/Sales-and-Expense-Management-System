package com.example.sales_expense_system.dto.response;

public class JwtResponse {

    private String token;
    private Long userId;
    private String username;
    private String roleName;
    private String status;
    private Integer inactivityTimeout;
    private Boolean twoFactorEnabled;

    public JwtResponse() {
    }

    public JwtResponse(String token,
                       Long userId,
                       String username,
                       String roleName,
                       String status,
                       Integer inactivityTimeout,
                       Boolean twoFactorEnabled) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.roleName = roleName;
        this.status = status;
        this.inactivityTimeout = inactivityTimeout;
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }
}