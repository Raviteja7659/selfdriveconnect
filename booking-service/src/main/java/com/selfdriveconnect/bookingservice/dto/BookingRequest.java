package com.selfdriveconnect.bookingservice.dto;

import com.selfdriveconnect.bookingservice.entity.Booking;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {

    // Note: renterId is intentionally NOT a field here. Trusting a
    // client-supplied renterId would let anyone book a car "as" someone
    // else. The authenticated user's id (from the JWT) is used instead —
    // see BookingController.

    @NotNull(message = "vehicleId is required")
    private Long vehicleId;

    @NotNull(message = "startDateTime is required")
    @Future(message = "startDateTime must be in the future")
    private LocalDateTime startDateTime;

    @NotNull(message = "endDateTime is required")
    @Future(message = "endDateTime must be in the future")
    private LocalDateTime endDateTime;

    @NotNull(message = "pricingType is required")
    private Booking.PricingType pricingType;
}
