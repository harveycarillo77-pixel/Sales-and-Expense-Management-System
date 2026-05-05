package com.example.sales_expense_system.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${app.version}")
    private String version;

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of("version", version, "status", "OK");
    }
}