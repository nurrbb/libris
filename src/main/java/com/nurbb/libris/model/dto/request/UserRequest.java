package com.nurbb.libris.model.dto.request;

import com.nurbb.libris.model.entity.valueobject.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {

    @NotBlank(message = "Full name must not be blank")
    private String fullName;

    @Email(message = "Email format is invalid")
    @NotBlank(message = "Email must not be blank")
    private String email;

    @Size(min = 5, message = "Password must be at least 5 characters")
    @NotBlank(message = "Password must not be blank")
    private String password;

    private String phone;

    private Role role;
}
