package com.prime.userservice.web.dto.request;

public record UpdateUserRequest (
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean isActive
){
}
