package com.selfdriveconnect.userservice.controller;

import com.selfdriveconnect.userservice.dto.AuthResponse;
import com.selfdriveconnect.userservice.dto.BootstrapAdminRequest;
import com.selfdriveconnect.userservice.dto.LoginRequest;
import com.selfdriveconnect.userservice.dto.RegisterRequest;
import com.selfdriveconnect.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * One-time endpoint to create the platform's first ADMIN account.
     * Protected by a server-side secret (never exposed in the API
     * surface) and self-disables permanently once any admin exists.
     * See AuthService.bootstrapAdmin for the full contract.
     */
    @PostMapping("/bootstrap-admin")
    public ResponseEntity<AuthResponse> bootstrapAdmin(@Valid @RequestBody BootstrapAdminRequest request) {
        return new ResponseEntity<>(authService.bootstrapAdmin(request), HttpStatus.CREATED);
    }
}
