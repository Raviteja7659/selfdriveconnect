package com.selfdriveconnect.reviewservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 1000, message = "comment must be under 1000 characters")
    private String comment;

    // Note: vehicleId, renterId, and ownerId are NOT fields here —
    // they're all derived server-side from the booking itself (via
    // booking-service) and from the caller's JWT. Same "don't trust
    // the client for anything identity determines" principle used
    // throughout this project.
}
