package com.dineease.service;

import com.dineease.dto.LoginRequest;
import com.dineease.dto.LoginResponse;
import com.dineease.dto.RegisterRequest;
import com.dineease.model.User;
import com.dineease.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // Storing as plain text for simplicity and ease of testing
        user.setEmail(request.getEmail());
        user.setRole("CUSTOMER");
        return userRepository.save(user);
    }

    public LoginResponse loginUser(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(request.getPassword())) {
                return new LoginResponse(user.getUsername(), user.getEmail(), user.getRole());
            }
        }
        throw new IllegalArgumentException("Invalid username or password");
    }
}
