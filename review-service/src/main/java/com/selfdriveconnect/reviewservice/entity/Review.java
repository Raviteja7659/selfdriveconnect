package com.selfdriveconnect.reviewservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A review is always tied to a specific completed booking, not
     * just "renter reviewed vehicle" in the abstract. This is what
     * lets us enforce "one review per booking" and "must have
     * actually completed a rental" rather than letting anyone review
     * any car they've merely heard of.
     */
    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false)
    private Long vehicleId;

    @Column(nullable = false)
    private Long renterId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
