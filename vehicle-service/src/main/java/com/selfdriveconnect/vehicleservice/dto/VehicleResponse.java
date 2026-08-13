package com.selfdriveconnect.vehicleservice.dto;

import com.selfdriveconnect.vehicleservice.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private Long id;
    private Long ownerId;
    private String brand;
    private String model;
    private Integer year;
    private String registrationNumber;
    private Vehicle.FuelType fuelType;
    private Vehicle.TransmissionType transmissionType;
    private Integer seatingCapacity;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;
    private String city;
    private String pickupLocation;
    private Double latitude;
    private Double longitude;
    private Vehicle.VehicleStatus status;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static VehicleResponse fromEntity(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .ownerId(v.getOwnerId())
                .brand(v.getBrand())
                .model(v.getModel())
                .year(v.getYear())
                .registrationNumber(v.getRegistrationNumber())
                .fuelType(v.getFuelType())
                .transmissionType(v.getTransmissionType())
                .seatingCapacity(v.getSeatingCapacity())
                .pricePerHour(v.getPricePerHour())
                .pricePerDay(v.getPricePerDay())
                .city(v.getCity())
                .pickupLocation(v.getPickupLocation())
                .latitude(v.getLatitude())
                .longitude(v.getLongitude())
                .status(v.getStatus())
                .imageUrl(v.getImageUrl())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
