package com.selfdriveconnect.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BootstrapAdminRequest {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    private String phoneNumber;

    /**
     * Must match the server-side jwt.admin-bootstrap-secret (an env
     * var / config value, never committed to git). Proves the caller
     * has access to the server's configuration, not just the API.
     */
    @NotBlank(message = "bootstrapSecret is required")
    private String bootstrapSecret;
}
