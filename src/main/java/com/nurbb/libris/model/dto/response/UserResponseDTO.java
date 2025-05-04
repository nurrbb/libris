package com.nurbb.libris.model.dto.response;

import com.nurbb.libris.model.entity.Role;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String fullName,
        String email,
        String phone,
        Role role,
        boolean deleted,
        String createdAt,
        String updatedAt
) {}
