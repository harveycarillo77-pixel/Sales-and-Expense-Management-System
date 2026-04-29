package com.example.sales_expense_system.service.impl;

import com.example.sales_expense_system.service.TotpService;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

@Service
public class TotpServiceImpl implements TotpService {

    private final CodeVerifier verifier;

    public TotpServiceImpl(CodeVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }
}