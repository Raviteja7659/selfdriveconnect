package com.selfdriveconnect.bookingservice.service;

import com.selfdriveconnect.bookingservice.client.VehicleDto;
import com.selfdriveconnect.bookingservice.client.VehicleServiceClient;
import com.selfdriveconnect.bookingservice.config.BookingConflictException;
import com.selfdriveconnect.bookingservice.config.ResourceNotFoundException;
import com.selfdriveconnect.bookingservice.config.VehicleNotAvailableException;
import com.selfdriveconnect.bookingservice.dto.BookingRequest;
import com.selfdriveconnect.bookingservice.dto.BookingResponse;
import com.selfdriveconnect.bookingservice.entity.Booking;
import com.selfdriveconnect.bookingservice.repository.BookingRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleServiceClient vehicleServiceClient;

    /**
     * Creates a booking. This method enforces the three core business
     * rules of the platform, in order:
     *
     * 1. The vehicle must exist and be ACTIVE (checked via vehicle-service)
     * 2. The requested date range must not overlap any existing
     *    PENDING/CONFIRMED booking for that vehicle (no double-booking)
     * 3. The price is calculated server-side from vehicle-service's
     *    rates — never trust a client-supplied price
     *
     * @Transactional ensures the overlap-check + insert happen atomically
     * from this service's point of view. Note: true double-booking
     * prevention under heavy concurrency would also want a DB-level
     * unique constraint or pessimistic lock; this is the applicationlevel
     * guard that covers the common case.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request, Long renterId) {
        validateDateRange(request.getStartDateTime(), request.getEndDateTime());

        VehicleDto vehicle = fetchVehicle(request.getVehicleId());

        if (!"ACTIVE".equals(vehicle.getStatus())) {
            throw new VehicleNotAvailableException(
                    "Vehicle " + vehicle.getId() + " is not available for booking (status: " + vehicle.getStatus() + ")");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                request.getVehicleId(), request.getStartDateTime(), request.getEndDateTime());

        if (!overlaps.isEmpty()) {
            throw new BookingConflictException(
                    "Vehicle " + request.getVehicleId() + " is already booked for an overlapping time slot");
        }

        BigDecimal totalAmount = calculatePrice(
                request.getPricingType(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                vehicle.getPricePerHour(),
                vehicle.getPricePerDay()
        );

        Booking booking = Booking.builder()
                .vehicleId(request.getVehicleId())
                .renterId(renterId)
                .ownerId(vehicle.getOwnerId())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .pricingType(request.getPricingType())
                .totalAmount(totalAmount)
                .status(Booking.BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} created for vehicle {} by renter {} — amount: {}",
                saved.getId(), saved.getVehicleId(), saved.getRenterId(), saved.getTotalAmount());

        return BookingResponse.fromEntity(saved);
    }

    /**
     * Read-only availability check — lets the frontend ask "is this
     * vehicle free for these dates?" before the renter commits,
     * without creating a booking.
     */
    public boolean isVehicleAvailable(Long vehicleId, LocalDateTime start, LocalDateTime end) {
        validateDateRange(start, end);
        return bookingRepository.findOverlappingBookings(vehicleId, start, end).isEmpty();
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return BookingResponse.fromEntity(booking);
    }

    public List<BookingResponse> getBookingsByRenter(Long renterId) {
        return bookingRepository.findByRenterId(renterId).stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    public List<BookingResponse> getBookingsByOwner(Long ownerId) {
        return bookingRepository.findByOwnerId(ownerId).stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    public List<BookingResponse> getBookingsByVehicle(Long vehicleId) {
        return bookingRepository.findByVehicleId(vehicleId).stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    @Transactional
    public BookingResponse confirmBooking(Long id, Long requesterId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // Confirming is an "accept this request" action performed by the
        // car's owner (or an admin) — not the renter who requested it.
        if (!isAdmin && !booking.getOwnerId().equals(requesterId)) {
            throw new AccessDeniedException("Only the vehicle owner can confirm this booking");
        }

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING bookings can be confirmed. Current status: " + booking.getStatus());
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return BookingResponse.fromEntity(bookingRepository.save(booking));
    }

    /**
     * Confirms a booking as a side effect of successful payment. This
     * intentionally skips the owner-only check that confirmBooking()
     * enforces — the caller here is payment-service itself (verified
     * via the internal API key at the controller layer), acting on
     * behalf of the platform because payment already succeeded, not
     * a renter or owner acting on their own authority.
     */
    @Transactional
    public BookingResponse confirmAfterPayment(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING bookings can be confirmed. Current status: " + booking.getStatus());
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return BookingResponse.fromEntity(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelBooking(Long id, String reason, Long requesterId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // Either party can cancel — the renter changing their mind, or
        // the owner pulling the car for some reason — plus admins.
        boolean isParty = booking.getRenterId().equals(requesterId) || booking.getOwnerId().equals(requesterId);
        if (!isAdmin && !isParty) {
            throw new AccessDeniedException("You are not a party to this booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a booking that is already COMPLETED");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already CANCELLED");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        return BookingResponse.fromEntity(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse completeBooking(Long id, Long requesterId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // Marking a rental "completed" (car returned) is the owner's call.
        if (!isAdmin && !booking.getOwnerId().equals(requesterId)) {
            throw new AccessDeniedException("Only the vehicle owner can mark this booking as completed");
        }

        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Only CONFIRMED bookings can be marked COMPLETED. Current status: " + booking.getStatus());
        }

        booking.setStatus(Booking.BookingStatus.COMPLETED);
        return BookingResponse.fromEntity(bookingRepository.save(booking));
    }

    // ---- helpers ----

    private VehicleDto fetchVehicle(Long vehicleId) {
        try {
            return vehicleServiceClient.getVehicleById(vehicleId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Vehicle not found with id: " + vehicleId);
        } catch (FeignException e) {
            log.error("vehicle-service call failed for vehicleId={}: {}", vehicleId, e.getMessage());
            throw new IllegalStateException("Unable to reach vehicle-service. Please try again shortly.");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("startDateTime must be before endDateTime");
        }
    }

    /**
     * Pricing rule:
     * - HOURLY: rounds UP to the next full hour (renter pays for any
     *   partial hour used — standard rental industry practice)
     * - DAILY: rounds UP to the next full day
     */
    private BigDecimal calculatePrice(Booking.PricingType type, LocalDateTime start, LocalDateTime end,
                                       BigDecimal pricePerHour, BigDecimal pricePerDay) {
        Duration duration = Duration.between(start, end);

        if (type == Booking.PricingType.HOURLY) {
            long hours = duration.toMinutes() % 60 == 0
                    ? duration.toHours()
                    : duration.toHours() + 1;
            hours = Math.max(hours, 1);
            return pricePerHour.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
        } else {
            long days = duration.toHours() % 24 == 0
                    ? duration.toDays()
                    : duration.toDays() + 1;
            days = Math.max(days, 1);
            return pricePerDay.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
