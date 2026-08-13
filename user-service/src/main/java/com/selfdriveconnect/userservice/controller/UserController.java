package com.selfdriveconnect.userservice.controller;

import com.selfdriveconnect.userservice.dto.UpdateRolesRequest;
import com.selfdriveconnect.userservice.dto.UserResponse;
import com.selfdriveconnect.userservice.entity.User;
import com.selfdriveconnect.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Returns the profile of whoever the JWT belongs to — this is the
     * pattern other services (booking-service, vehicle-service) use
     * to figure out "who is making this request".
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getCurrentUser(user.getEmail()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Admin-only. This is how every admin account AFTER the one-time
     * bootstrap gets created — an existing admin promotes someone
     * else's account by granting them the ADMIN role (or revokes it,
     * or adjusts OWNER/RENTER for any user).
     */
    @PatchMapping("/{id}/roles")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateRolesRequest request,
                                                      @AuthenticationPrincipal User requester) {
        if (!requester.getRoles().contains(User.Role.ADMIN)) {
            throw new AccessDeniedException("Only an admin can change another user's roles");
        }
        return ResponseEntity.ok(userService.updateRoles(id, request.getRoles()));
    }
}
