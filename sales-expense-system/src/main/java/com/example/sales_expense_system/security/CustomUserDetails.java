package com.example.sales_expense_system.security;

import com.example.sales_expense_system.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();  // Correct — Lombok generates getPasswordHash() from field 'passwordHash'
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Fixed: status is UserStatus enum, not a String
        return user.getStatus() != User.UserStatus.INACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Fixed: status is UserStatus enum, not a String
        return user.getStatus() == User.UserStatus.ACTIVE;
    }
}