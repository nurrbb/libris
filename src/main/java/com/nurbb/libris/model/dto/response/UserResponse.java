package com.nurbb.libris.model.dto.response;

import com.nurbb.libris.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private boolean deleted;
    private String createdAt;
    private String updatedAt;
}
