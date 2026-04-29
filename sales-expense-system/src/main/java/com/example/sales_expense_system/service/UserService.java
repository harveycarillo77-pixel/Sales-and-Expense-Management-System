package com.example.sales_expense_system.service;

import com.example.sales_expense_system.model.User;
import java.util.List;

public interface UserService {

    List<User> getAllUsers();

    User getUserById(Integer id);

    User createUser(User user);

    User updateUser(Integer id, User user);

    void deleteUser(Integer id);
    
}
