package com.selfdriveconnect.reviewservice.service;

import com.selfdriveconnect.reviewservice.client.BookingDto;
import com.selfdriveconnect.reviewservice.client.BookingServiceClient;
import com.selfdriveconnect.reviewservice.config.BookingNotReviewableException;
import com.selfdriveconnect.reviewservice.config.ResourceNotFoundException;
import com.selfdriveconnect.reviewservice.dto.ReviewRequest;
import com.selfdriveconnect.reviewservice.dto.ReviewResponse;
import com.selfdriveconnect.reviewservice.dto.VehicleRatingSummary;
import com.selfdriveconnect.reviewservice.entity.Review;
import com.selfdriveconnect.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingServiceClient bookingServiceClient;

    /**
     * Creates a review. Enforces, in order:
     *
     * 1. The booking must exist (fetched from booking-service — never
     *    trust vehicleId/ownerId from the client)
     * 2. The caller must be the actual renter on that booking
     * 3. The booking must be COMPLETED — you can't review a car you
     *    never actually finished renting
     * 4. Only one review per booking, ever
     */
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long renterId) {
        BookingDto booking = bookingServiceClient.getBookingById(request.getBookingId());

        if (!booking.getRenterId().equals(renterId)) {
            throw new AccessDeniedException("You can only review your own bookings");
        }

        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new BookingNotReviewableException(
                    "Booking " + booking.getId() + " cannot be reviewed yet (status: " + booking.getStatus() +
                            "). Only completed rentals can be reviewed.");
        }

        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new BookingNotReviewableException(
                    "Booking " + booking.getId() + " has already been reviewed");
        }

        Review review = Review.builder()
                .bookingId(booking.getId())
                .vehicleId(booking.getVehicleId())
                .renterId(renterId)
                .ownerId(booking.getOwnerId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review {} created for vehicle {} (booking {}) — rating {}",
                saved.getId(), saved.getVehicleId(), saved.getBookingId(), saved.getRating());

        return ReviewResponse.fromEntity(saved);
    }

    public ReviewResponse getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        return ReviewResponse.fromEntity(review);
    }

    public List<ReviewResponse> getReviewsByVehicle(Long vehicleId) {
        return reviewRepository.findByVehicleId(vehicleId).stream()
                .map(ReviewResponse::fromEntity)
                .toList();
    }

    public List<ReviewResponse> getReviewsByRenter(Long renterId) {
        return reviewRepository.findByRenterId(renterId).stream()
                .map(ReviewResponse::fromEntity)
                .toList();
    }

    public List<ReviewResponse> getReviewsByOwner(Long ownerId) {
        return reviewRepository.findByOwnerId(ownerId).stream()
                .map(ReviewResponse::fromEntity)
                .toList();
    }

    public VehicleRatingSummary getVehicleRatingSummary(Long vehicleId) {
        Double average = reviewRepository.findAverageRatingByVehicleId(vehicleId);
        long count = reviewRepository.countByVehicleId(vehicleId);

        return VehicleRatingSummary.builder()
                .vehicleId(vehicleId)
                .averageRating(average == null ? 0.0 : Math.round(average * 10) / 10.0)
                .totalReviews(count)
                .build();
    }

    /**
     * Deletion is restricted to the review's own author or an admin —
     * an owner cannot delete an unflattering review of their own
     * vehicle, which is the whole point of reviews being trustworthy.
     */
    @Transactional
    public void deleteReview(Long id, Long requesterId, boolean isAdmin) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        if (!isAdmin && !review.getRenterId().equals(requesterId)) {
            throw new AccessDeniedException("You can only delete your own reviews");
        }

        reviewRepository.deleteById(id);
    }
}
