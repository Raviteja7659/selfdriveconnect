package com.selfdriveconnect.vehicleservice.dto;

import com.selfdriveconnect.vehicleservice.entity.Vehicle;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VehicleRequest {

    // Note: ownerId is intentionally NOT a field here. Trusting a
    // client-supplied ownerId would let anyone list a car "as" someone
    // else. The authenticated user's id (from the JWT) is used instead —
    // see VehicleController.

    @NotBlank(message = "brand is required")
    private String brand;

    @NotBlank(message = "model is required")
    private String model;

    @Min(value = 1990, message = "year must be reasonable")
    private Integer year;

    @NotBlank(message = "registrationNumber is required")
    private String registrationNumber;

    @NotNull(message = "fuelType is required")
    private Vehicle.FuelType fuelType;

    @NotNull(message = "transmissionType is required")
    private Vehicle.TransmissionType transmissionType;

    @Min(1)
    private Integer seatingCapacity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "pricePerHour must be positive")
    private BigDecimal pricePerHour;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "pricePerDay must be positive")
    private BigDecimal pricePerDay;

    @NotBlank(message = "city is required")
    private String city;

    private String pickupLocation;

    private Double latitude;

    private Double longitude;

    private String imageUrl;
}
