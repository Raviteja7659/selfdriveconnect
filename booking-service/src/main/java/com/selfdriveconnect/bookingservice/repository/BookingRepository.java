package com.selfdriveconnect.bookingservice.repository;

import com.selfdriveconnect.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRenterId(Long renterId);

    List<Booking> findByVehicleId(Long vehicleId);

    List<Booking> findByOwnerId(Long ownerId);

    /**
     * Core availability-conflict query.
     *
     * Two date ranges [startA, endA) and [startB, endB) overlap
     * if and only if: startA < endB AND startB < endA.
     *
     * We only check against bookings in PENDING or CONFIRMED status —
     * CANCELLED/COMPLETED bookings no longer occupy the calendar.
     *
     * This single query is what prevents double-booking a car.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.vehicleId = :vehicleId
            AND b.status IN ('PENDING', 'CONFIRMED')
            AND b.startDateTime < :endDateTime
            AND b.endDateTime > :startDateTime
            """)
    List<Booking> findOverlappingBookings(
            @Param("vehicleId") Long vehicleId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * Same as above but excludes a given booking id — used when
     * a renter is rescheduling an existing booking rather than
     * creating a fresh one, so it doesn't conflict with itself.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.vehicleId = :vehicleId
            AND b.id <> :excludeBookingId
            AND b.status IN ('PENDING', 'CONFIRMED')
            AND b.startDateTime < :endDateTime
            AND b.endDateTime > :startDateTime
            """)
    List<Booking> findOverlappingBookingsExcluding(
            @Param("vehicleId") Long vehicleId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("excludeBookingId") Long excludeBookingId
    );
}
