package com.prime.userservice.domain.service;

import com.prime.userservice.config.JwtUtil;
import com.prime.userservice.domain.exception.InvalidCredentialsException;
import com.prime.userservice.domain.model.CustomUserDetails;
import com.prime.userservice.domain.model.UserEntity;
import com.prime.userservice.domain.repository.UserRepository;
import com.prime.userservice.web.dto.request.LoginRequest;
import com.prime.userservice.web.dto.response.AuthResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository; // Inject UserRepository to fetch UserEntity if CustomUserDetails doesn't provide enough data for AuthResponse directly

    // Constructor to inject all necessary dependencies
    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String jwt = jwtUtil.generateToken(userDetails);


            UUID authenticatedUserId;
            String authenticatedUserEmail = userDetails.getUsername(); // This is the email

            if (userDetails instanceof CustomUserDetails customUserDetails) {
                authenticatedUserId = customUserDetails.getId();
            } else {
                UserEntity userEntity = userRepository.findByEmail(authenticatedUserEmail)
                        .orElseThrow(() -> new RuntimeException("User not found after authentication! This should not happen."));
                authenticatedUserId = userEntity.getId();
            }

            return new AuthResponse(authenticatedUserId, authenticatedUserEmail, jwt, "Login successful!");

        } catch (BadCredentialsException e) {
            System.out.println("Invalid credentials provided: " + e.getMessage());
            throw new InvalidCredentialsException("Invalid email or password provided.");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }
}
