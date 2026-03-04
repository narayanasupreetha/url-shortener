package com.urlshortener.service;

import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.dto.RegisterResponse;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.exception.UserAlreadyExistsException;
import com.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for admin user management.
 * Handles creation of admin accounts with proper validation
 * and password encryption.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new admin user.
     * - Checks if email already exists
     * - Encodes the password
     * - Assigns ADMIN role
     * - Saves user to database
     * - Returns basic admin details
     *
     * @param request contains admin registration details
     * @return response with created admin id and email
     * @throws UserAlreadyExistsException if email is already registered
     */
    public RegisterResponse createAdmin(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        User admin = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);

        return new RegisterResponse(admin.getId(), admin.getEmail());
    }
}
