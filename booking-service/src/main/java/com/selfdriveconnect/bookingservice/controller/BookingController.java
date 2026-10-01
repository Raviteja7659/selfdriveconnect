package com.selfdriveconnect.bookingservice.controller;

import com.selfdriveconnect.bookingservice.dto.BookingRequest;
import com.selfdriveconnect.bookingservice.dto.BookingResponse;
import com.selfdriveconnect.bookingservice.security.AuthenticatedUser;
import com.selfdriveconnect.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        return new ResponseEntity<>(bookingService.createBooking(request, user.userId()), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/renter/{renterId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByRenter(@PathVariable Long renterId) {
        return ResponseEntity.ok(bookingService.getBookingsByRenter(renterId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(bookingService.getBookingsByOwner(ownerId));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(bookingService.getBookingsByVehicle(vehicleId));
    }

    @GetMapping("/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        boolean available = bookingService.isVehicleAvailable(vehicleId, start, end);
        return ResponseEntity.ok(Map.of(
                "vehicleId", vehicleId,
                "start", start,
                "end", end,
                "available", available
        ));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long id,
                                                            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, user.userId(), user.hasRole("ADMIN")));
    }

    // Note: the old "/internal/confirm-after-payment" endpoint (guarded
    // by a shared X-Internal-Api-Key header) has been removed. That
    // service-to-service coordination now happens via RabbitMQ instead
    // — see PaymentEventListener. This is a deliberate architectural
    // change, not a removed feature: confirming a booking after payment
    // still happens automatically, just through an event instead of a
    // direct HTTP call with a shared secret.

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id,
                                                           @RequestParam(required = false) String reason,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, reason, user.userId(), user.hasRole("ADMIN")));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> completeBooking(@PathVariable Long id,
                                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(bookingService.completeBooking(id, user.userId(), user.hasRole("ADMIN")));
    }
}
