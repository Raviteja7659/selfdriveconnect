package com.selfdriveconnect.userservice.dto;

import com.selfdriveconnect.userservice.entity.User;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateRolesRequest {

    @NotEmpty(message = "roles cannot be empty")
    private Set<User.Role> roles;
}
