package com.example.sales_expense_system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.sales_expense_system.model.Role;
import com.example.sales_expense_system.repository.RoleRepository;

@Configuration
public class RoleInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByRoleName("ADMIN").isEmpty()) {
                Role admin = new Role();
                admin.setRoleName("ADMIN");
                admin.setDescription("System Administrator");
                roleRepository.save(admin);
            }

            if (roleRepository.findByRoleName("ACCOUNTANT").isEmpty()) {
                Role accountant = new Role();
                accountant.setRoleName("ACCOUNTANT");
                accountant.setDescription("Accountant user");
                roleRepository.save(accountant);
            }
        };
    }
}
