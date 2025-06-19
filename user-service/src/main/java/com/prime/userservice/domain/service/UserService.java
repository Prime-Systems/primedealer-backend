package com.prime.userservice.domain.service;

import com.prime.userservice.domain.mapper.UserMapper;
import com.prime.userservice.domain.model.UserEntity;
import com.prime.userservice.domain.repository.UserRepository;
import com.prime.userservice.web.dto.request.CreateUserRequest;
import com.prime.userservice.web.dto.request.UpdateUserRequest;
import com.prime.userservice.web.dto.response.CreateUserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public CreateUserResponse createUser(CreateUserRequest createUserRequest) {
       String encodedPassword = passwordEncoder.encode(createUserRequest.password());
        UserEntity userEntity = userMapper.toUserEntity(createUserRequest);
        userEntity.setPasswordHash(encodedPassword);

        UserEntity savedUser = userRepository.save(userEntity);
        return new CreateUserResponse(
                savedUser.getId().toString(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.isActive()
        );
    }

    public UserEntity getUserById(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public void updateUser(String userId, UpdateUserRequest updateUserRequest) {
        UserEntity userEntity = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        userMapper.updateUserEntityFromUpdateRequest(updateUserRequest, userEntity);
        userRepository.save(userEntity);
    }
}
