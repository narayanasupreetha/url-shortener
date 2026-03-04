package com.urlshortener.service;

import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.LoginResponse;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.dto.RegisterResponse;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.exception.UserAlreadyExistsException;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for authentication operations.
 * Handles user registration, login, and JWT token generation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${jwt.expirationMs}")
    private long jwtExpirationMs;

    /**
     * Registers a new user.
     * - Checks if email already exists
     * - Encrypts the password
     * - Assigns USER role
     * - Saves user to database
     * - Returns registration details
     *
     * @param request registration details
     * @return response containing user id and email
     * @throws UserAlreadyExistsException if email is already registered
     */
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return new RegisterResponse(
                user.getId(),
                user.getEmail()
        );
    }

    /**
     * Authenticates user credentials and generates a JWT token.
     * - Validates credentials
     * - Loads user details
     * - Generates JWT token
     * - Returns token with expiry and role information
     *
     * @param request login details
     * @return response containing JWT token and user info
     */
    public LoginResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserDetails user = (UserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(user.getUsername());

        String role = user.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("USER");

        return new LoginResponse(
                token,
                jwtExpirationMs / 1000,
                user.getUsername(),
                role
        );
    }
}