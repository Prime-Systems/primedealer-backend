package com.prime.userservice.domain.mapper;

import com.prime.userservice.domain.model.User;
import com.prime.userservice.domain.model.UserEntity;
import com.prime.userservice.web.dto.request.CreateUserRequest;
import com.prime.userservice.web.dto.request.UpdateUserRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    // Map UserEntity to User record
    User toUserRecord(UserEntity entity);

    List<User> toUserRecordList(List<UserEntity> entities);

    // Map CreateUserRequest to UserEntity
    @Mapping(target = "id", ignore = true) // Ignore ID as it will be generated
    @Mapping(target = "createdAt", ignore = true) // Ignore createdAt as it will be set by the service
    @Mapping(target = "updatedAt", ignore = true) // Ignore updatedAt as it will be set by the service
    @Mapping(target = "passwordHash", ignore = true) // Ignore passwordHash as it will be set by the service
    UserEntity toUserEntity(CreateUserRequest request);

    List<UserEntity> toUserEntityList(List<CreateUserRequest> requests);

    // Update existing UserEntity from UpdateUserRequest
    // @MappingTarget indicates that 'entity' is the target object to be updated
    @Mapping(target = "id", ignore = true) // Don't update the ID
    @Mapping(target = "passwordHash", ignore = true) // Password is not updated via this DTO
    @Mapping(target = "createdAt", ignore = true) // Don't update creation timestamp
    @Mapping(target = "updatedAt", ignore = true) // Updated by @PreUpdate callback
    void updateUserEntityFromUpdateRequest(UpdateUserRequest request, @MappingTarget UserEntity entity);

}
