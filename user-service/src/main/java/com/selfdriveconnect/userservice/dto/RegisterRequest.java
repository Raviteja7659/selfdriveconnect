package com.selfdriveconnect.userservice.dto;

import com.selfdriveconnect.userservice.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class RegisterRequest {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    private String phoneNumber;

    @NotEmpty(message = "at least one role is required (RENTER and/or OWNER)")
    private Set<User.Role> roles;

    // Note: ADMIN cannot be requested here — AuthService rejects it.
    // The only ways to get ADMIN are the one-time bootstrap endpoint
    // (for the very first admin) or being promoted by an existing admin.
}
