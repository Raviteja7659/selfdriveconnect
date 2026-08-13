package com.selfdriveconnect.bookingservice.config;

/**
 * Thrown when a requested booking's date range overlaps with an
 * existing PENDING or CONFIRMED booking for the same vehicle.
 */
public class BookingConflictException extends RuntimeException {
    public BookingConflictException(String message) {
        super(message);
    }
}
