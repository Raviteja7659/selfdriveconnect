package com.selfdriveconnect.bookingservice.dto;

import com.selfdriveconnect.bookingservice.entity.Booking;
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
public class BookingResponse {

    private Long id;
    private Long vehicleId;
    private Long renterId;
    private Long ownerId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Booking.PricingType pricingType;
    private BigDecimal totalAmount;
    private Booking.BookingStatus status;
    private LocalDateTime createdAt;

    public static BookingResponse fromEntity(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .vehicleId(b.getVehicleId())
                .renterId(b.getRenterId())
                .ownerId(b.getOwnerId())
                .startDateTime(b.getStartDateTime())
                .endDateTime(b.getEndDateTime())
                .pricingType(b.getPricingType())
                .totalAmount(b.getTotalAmount())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
