package com.selfdriveconnect.reviewservice.controller;

import com.selfdriveconnect.reviewservice.dto.ReviewRequest;
import com.selfdriveconnect.reviewservice.dto.ReviewResponse;
import com.selfdriveconnect.reviewservice.dto.VehicleRatingSummary;
import com.selfdriveconnect.reviewservice.security.AuthenticatedUser;
import com.selfdriveconnect.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request,
                                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return new ResponseEntity<>(reviewService.createReview(request, user.userId()), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(reviewService.getReviewsByVehicle(vehicleId));
    }

    @GetMapping("/vehicle/{vehicleId}/summary")
    public ResponseEntity<VehicleRatingSummary> getVehicleRatingSummary(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(reviewService.getVehicleRatingSummary(vehicleId));
    }

    @GetMapping("/renter/{renterId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByRenter(@PathVariable Long renterId) {
        return ResponseEntity.ok(reviewService.getReviewsByRenter(renterId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(reviewService.getReviewsByOwner(ownerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        reviewService.deleteReview(id, user.userId(), user.hasRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }
}
