package com.selfdriveconnect.reviewservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRatingSummary {
    private Long vehicleId;
    private double averageRating;
    private long totalReviews;
}
