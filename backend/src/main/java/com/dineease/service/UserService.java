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

/**
 * Service class handling user registration and login authentication.
 */
@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Registers a new customer in the system. Checks for duplicate usernames and emails.
     * 
     * @param request Data transfer object containing the registration fields.
     * @return Saved User entity.
     */
    public User registerUser(RegisterRequest request) {
        // [DEBUG BREAKPOINT: Inspect registration parameters (username, email, password)]
        if (userRepository.existsByUsername(request.getUsername())) {
            // [DEBUG BREAKPOINT: Break here if username is already taken]
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            // [DEBUG BREAKPOINT: Break here if email address is already registered]
            throw new IllegalArgumentException("Email already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // Storing as plain text for simplicity and ease of testing
        user.setEmail(request.getEmail());
        user.setRole("CUSTOMER");
        return userRepository.save(user);
    }

    /**
     * Authenticates a user based on their username and password.
     * 
     * @param request Data transfer object containing username and password.
     * @return LoginResponse DTO containing user info if successful.
     */
    public LoginResponse loginUser(LoginRequest request) {
        // [DEBUG BREAKPOINT: Inspect login credentials]
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(request.getPassword())) {
                return new LoginResponse(user.getUsername(), user.getEmail(), user.getRole());
            } else {
                // [DEBUG BREAKPOINT: Break here if password does not match]
            }
        } else {
            // [DEBUG BREAKPOINT: Break here if username is not found in the database]
        }
        throw new IllegalArgumentException("Invalid username or password");
    }
}
