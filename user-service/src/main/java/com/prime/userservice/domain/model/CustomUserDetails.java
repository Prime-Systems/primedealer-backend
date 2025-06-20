package com.prime.userservice.domain.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {
    private UUID id;
    private String email;
    private String passwordHash;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean isActive;

    // Constructor to populate CustomUserDetails from a UserEntity
    public CustomUserDetails(UserEntity userEntity) {
        this.id = userEntity.getId();
        this.email = userEntity.getEmail();
        this.passwordHash = userEntity.getPasswordHash();
        this.isActive = userEntity.isActive();


        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(userEntity.getRole().name()));

    }


    public UUID getId() {
        return id;
    }

    // --- UserDetails Interface Implementations ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isActive;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
