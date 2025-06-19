package com.prime.userservice.web.controllers;


import com.prime.userservice.domain.mapper.UserMapper;
import com.prime.userservice.domain.model.User;
import com.prime.userservice.domain.model.UserEntity;
import com.prime.userservice.domain.service.UserService;
import com.prime.userservice.web.dto.request.CreateUserRequest;
import com.prime.userservice.web.dto.request.UpdateUserRequest;
import com.prime.userservice.web.dto.response.CreateUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // This endpoint should be allowed without authentication (from SecurityConfig)
    @PostMapping("/register")
    public ResponseEntity<CreateUserResponse> registerUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse createdUser = userService.createUser(request);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    // This endpoint requires authentication. Only ADMIN can access.
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or (hasAuthority('USER') and #id == authentication.principal.id)") // Example for user to see their own data
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        User user = userService.getUserById(String.valueOf(id));
        return ResponseEntity.ok(user);
    }

    // This endpoint requires authentication. Only ADMIN can update any user.
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {

        User updatedUser = userService.updateUser(String.valueOf(id), request);
        return ResponseEntity.ok(updatedUser);
    }

    // This endpoint requires authentication. Only ADMIN can delete.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(String.valueOf(id));
        return ResponseEntity.noContent().build();
    }

}
