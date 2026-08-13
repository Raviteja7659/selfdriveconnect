package com.selfdriveconnect.vehicleservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to owner in user-service (no hard FK across services)
    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    private Integer year;

    @Column(nullable = false)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    private TransmissionType transmissionType;

    private Integer seatingCapacity;

    @Column(nullable = false)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private BigDecimal pricePerDay;

    private String city;

    private String pickupLocation;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.PENDING_VERIFICATION;

    private String imageUrl;

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

    public enum FuelType {
        PETROL, DIESEL, ELECTRIC, CNG, HYBRID
    }

    public enum TransmissionType {
        MANUAL, AUTOMATIC
    }

    public enum VehicleStatus {
        PENDING_VERIFICATION, ACTIVE, INACTIVE, SUSPENDED
    }
}
