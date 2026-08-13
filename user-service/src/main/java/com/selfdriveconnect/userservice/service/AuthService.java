package com.selfdriveconnect.userservice.service;

import com.selfdriveconnect.userservice.config.AdminBootstrapException;
import com.selfdriveconnect.userservice.config.EmailAlreadyExistsException;
import com.selfdriveconnect.userservice.dto.AuthResponse;
import com.selfdriveconnect.userservice.dto.BootstrapAdminRequest;
import com.selfdriveconnect.userservice.dto.LoginRequest;
import com.selfdriveconnect.userservice.dto.RegisterRequest;
import com.selfdriveconnect.userservice.entity.User;
import com.selfdriveconnect.userservice.repository.UserRepository;
import com.selfdriveconnect.userservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${admin.bootstrap-secret}")
    private String bootstrapSecret;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "An account with email " + request.getEmail() + " already exists");
        }

        if (request.getRoles().contains(User.Role.ADMIN)) {
            throw new IllegalArgumentException(
                    "ADMIN cannot be self-assigned at registration. It can only be granted via the " +
                            "one-time bootstrap process or by an existing admin promoting this account afterward.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(request.getRoles())
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(saved.getId())
                .email(saved.getEmail())
                .expiresInMillis(jwtService.getExpirationMs())
                .build();
    }

    /**
     * Creates the platform's very first ADMIN account. This endpoint is
     * self-disabling: once ANY user holds the ADMIN role, every future
     * call is rejected regardless of the secret, so it cannot be reused
     * as a backdoor after go-live.
     *
     * The caller must supply bootstrapSecret matching the server's
     * admin.bootstrap-secret config value — something only someone
     * with access to the deployment's configuration/environment would
     * know, not something guessable from the API surface alone.
     */
    @Transactional
    public AuthResponse bootstrapAdmin(BootstrapAdminRequest request) {
        if (userRepository.existsByAdminRole()) {
            throw new AdminBootstrapException(
                    "An admin already exists. Bootstrap is permanently disabled — " +
                            "ask an existing admin to promote your account instead.");
        }

        if (!bootstrapSecret.equals(request.getBootstrapSecret())) {
            throw new AdminBootstrapException("Invalid bootstrap secret");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "An account with email " + request.getEmail() + " already exists");
        }

        User admin = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(User.Role.ADMIN))
                .enabled(true)
                .build();

        User saved = userRepository.save(admin);
        String token = jwtService.generateToken(saved);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(saved.getId())
                .email(saved.getEmail())
                .expiresInMillis(jwtService.getExpirationMs())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Delegates to Spring Security's AuthenticationManager, which uses
        // our DaoAuthenticationProvider (UserDetailsService + PasswordEncoder)
        // under the hood. Throws BadCredentialsException on mismatch —
        // handled centrally by GlobalExceptionHandler.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User vanished after successful authentication"));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .expiresInMillis(jwtService.getExpirationMs())
                .build();
    }
}
