package com.selfdriveconnect.reviewservice.dto;

import com.selfdriveconnect.reviewservice.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long bookingId;
    private Long vehicleId;
    private Long renterId;
    private Long ownerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .bookingId(r.getBookingId())
                .vehicleId(r.getVehicleId())
                .renterId(r.getRenterId())
                .ownerId(r.getOwnerId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
