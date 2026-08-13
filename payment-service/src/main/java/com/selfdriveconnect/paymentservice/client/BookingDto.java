package com.selfdriveconnect.paymentservice.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

    private Long id;
    private Long vehicleId;
    private Long renterId;
    private Long ownerId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String pricingType;
    private BigDecimal totalAmount;
    private String status; // PENDING, CONFIRMED, CANCELLED, COMPLETED
    private LocalDateTime createdAt;
}
