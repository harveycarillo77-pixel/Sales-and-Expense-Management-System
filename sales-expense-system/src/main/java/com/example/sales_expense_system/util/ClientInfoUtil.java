package com.example.sales_expense_system.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientInfoUtil {

    public String getDeviceName(HttpServletRequest request) {
        return headerOrNull(request, "X-Device-Name");
    }

    public String getMacAddress(HttpServletRequest request) {
        return headerOrNull(request, "X-Mac-Address");
    }

    public String getLocalIp(HttpServletRequest request) {
        return headerOrNull(request, "X-Local-IP");
    }

    public String getUserAgent(HttpServletRequest request) {
        return headerOrNull(request, "X-User-Agent");
    }

    public String getRequestIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String headerOrNull(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return (value == null || value.isBlank()) ? null : value;
    }
}