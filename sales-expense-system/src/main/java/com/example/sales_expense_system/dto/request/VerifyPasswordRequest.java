package com.example.sales_expense_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPasswordRequest {

    @NotBlank(message = "Password is required.")
    private String password;
}