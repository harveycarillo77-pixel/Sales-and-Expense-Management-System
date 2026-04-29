package com.example.sales_expense_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Return as PasswordEncoder interface, not BCryptPasswordEncoder
    }

    @Bean
    public CodeGenerator codeGenerator() {
        return new DefaultCodeGenerator();
    }

    @Bean
    public CodeVerifier codeVerifier(CodeGenerator codeGenerator) {
        // Only pass generator and time provider (v1.7.1 does not allow 'int')
        return new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());
    }

    @Bean
    public DefaultSecretGenerator secretGenerator() {
        return new DefaultSecretGenerator();
    }

    @Bean
    public QrGenerator qrGenerator() {
        return new ZxingPngQrGenerator(); // Default implementation from TOTP library
    }
}