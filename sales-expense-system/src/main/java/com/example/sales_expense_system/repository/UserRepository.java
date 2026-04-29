package com.example.sales_expense_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sales_expense_system.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findWithRoleByUserId(Long userId);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findWithRoleByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"role"})
    List<User> findAll();
}