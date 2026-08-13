package com.selfdriveconnect.reviewservice.repository;

import com.selfdriveconnect.reviewservice.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByVehicleId(Long vehicleId);

    List<Review> findByRenterId(Long renterId);

    List<Review> findByOwnerId(Long ownerId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.vehicleId = :vehicleId")
    Double findAverageRatingByVehicleId(@Param("vehicleId") Long vehicleId);

    long countByVehicleId(Long vehicleId);
}
