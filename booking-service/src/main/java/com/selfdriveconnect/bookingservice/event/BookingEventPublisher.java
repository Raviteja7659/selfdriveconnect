package com.selfdriveconnect.bookingservice.event;

import com.selfdriveconnect.bookingservice.config.RabbitMQConfig;
import com.selfdriveconnect.bookingservice.entity.Booking;
import com.selfdriveconnect.bookingservice.realtime.RealtimeBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Every booking status change goes through here, which does two
 * things:
 *
 * 1. Publishes to RabbitMQ (routing key "booking.event") so any other
 *    service can react without booking-service needing to know they
 *    exist — the actual point of event-driven design.
 * 2. Pushes an SSE update immediately to whichever of the renter/owner
 *    have an open connection, since that's an in-process call and
 *    doesn't need to round-trip through the broker for same-service
 *    delivery.
 *
 * Publishing failures are logged, not thrown — a broker hiccup should
 * never fail the booking operation that triggered it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RealtimeBroadcaster realtimeBroadcaster;

    public void publish(Booking booking) {
        BookingLifecycleEvent event = new BookingLifecycleEvent(
                booking.getId(),
                booking.getVehicleId(),
                booking.getRenterId(),
                booking.getOwnerId(),
                booking.getStatus().name(),
                LocalDateTime.now()
        );

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EVENTS_EXCHANGE, "booking.event", event);
        } catch (Exception e) {
            log.warn("Failed to publish booking event for booking {}: {}", booking.getId(), e.getMessage());
        }

        realtimeBroadcaster.sendToUser(booking.getRenterId(), "booking-update", event);
        realtimeBroadcaster.sendToUser(booking.getOwnerId(), "booking-update", event);
    }
}
