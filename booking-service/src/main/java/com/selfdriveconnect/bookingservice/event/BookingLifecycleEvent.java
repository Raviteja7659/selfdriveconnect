package com.selfdriveconnect.bookingservice.event;

import java.time.LocalDateTime;

/**
 * Published every time a booking's status changes. Right now the
 * only in-process consumer is RealtimeBroadcaster (pushes to any
 * open SSE connections for the renter/owner), but publishing it to
 * RabbitMQ as well means any future service — a notification-service
 * sending emails/push, an analytics pipeline, etc. — can subscribe
 * without booking-service needing to know they exist or call them
 * directly.
 */
public record BookingLifecycleEvent(
        Long bookingId,
        Long vehicleId,
        Long renterId,
        Long ownerId,
        String status, // PENDING, CONFIRMED, CANCELLED, COMPLETED
        LocalDateTime occurredAt
) {
}
