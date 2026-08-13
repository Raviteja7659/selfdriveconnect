package com.selfdriveconnect.userservice.service;

import com.selfdriveconnect.userservice.config.ResourceNotFoundException;
import com.selfdriveconnect.userservice.dto.UserResponse;
import com.selfdriveconnect.userservice.entity.User;
import com.selfdriveconnect.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponse.fromEntity(user);
    }

    /**
     * Grants/revokes roles for a target user. Only callable by an
     * existing admin — enforced in UserController before this is
     * ever invoked. This is how every admin AFTER the bootstrap
     * admin gets created: an existing admin promotes someone.
     */
    @Transactional
    public UserResponse updateRoles(Long targetUserId, Set<User.Role> newRoles) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        user.setRoles(newRoles);
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }
}
