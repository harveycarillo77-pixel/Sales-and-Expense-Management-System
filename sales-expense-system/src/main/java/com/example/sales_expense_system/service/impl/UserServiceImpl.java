package com.example.sales_expense_system.service.impl;

import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.repository.UserRepository;
import com.example.sales_expense_system.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id.longValue()) // userId is Long
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User createUser(User user) {
        // Hash password before saving
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Integer id, User user) {
        User existing = getUserById(id);
        existing.setUsername(user.getUsername());
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            existing.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        existing.setRole(user.getRole());
        existing.setStatus(user.getStatus());
        existing.setInactivityTimeout(user.getInactivityTimeout());
        return userRepository.save(existing);
    }

    @Override
    public void deleteUser(Integer id) {
        userRepository.deleteById(id.longValue()); // userId is Long
    }
}