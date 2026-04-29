package com.example.sales_expense_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TotpResponse {
    private String username;
    private String rawTotpSecret; // Display once
    private String qrCodeUrl;      // For Google Authenticator
}