package com.selfdriveconnect.bookingservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirrors payment-service's own PaymentEvent. Deliberately a separate
 * class, not a shared library dependency — each service owns its own
 * copy of the contracts it consumes, same "don't couple services at
 * compile time" principle already used for BookingDto/VehicleDto
 * elsewhere in this project.
 */
public record PaymentEvent(
        Long paymentId,
        Long bookingId,
        Long renterId,
        BigDecimal amount,
        String status, // SUCCESS or FAILED
        String failureReason,
        LocalDateTime occurredAt
) {
}
