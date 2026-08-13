package com.selfdriveconnect.userservice.repository;

import com.selfdriveconnect.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Used to gate the one-time admin bootstrap endpoint: once at least
     * one ADMIN exists, bootstrap is permanently disabled and further
     * admins can only be created by promotion from an existing admin.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE r = 'ADMIN'")
    boolean existsByAdminRole();
}
