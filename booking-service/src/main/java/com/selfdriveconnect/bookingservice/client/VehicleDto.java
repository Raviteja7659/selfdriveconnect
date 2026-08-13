package com.selfdriveconnect.bookingservice.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Mirrors the subset of vehicle-service's VehicleResponse that
 * booking-service actually needs. Deliberately NOT the same class
 * as vehicle-service's own DTO — each service owns its own contract,
 * even when the shape overlaps. This is intentional decoupling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {

    private Long id;
    private Long ownerId;
    private String brand;
    private String model;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;
    private String status; // ACTIVE, INACTIVE, PENDING_VERIFICATION, SUSPENDED
}
