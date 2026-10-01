package com.selfdriveconnect.bookingservice.event;

import com.selfdriveconnect.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.selfdriveconnect.bookingservice.config.RabbitMQConfig.PAYMENT_EVENTS_QUEUE;

/**
 * This is the direct replacement for the old
 * "payment-service calls booking-service's internal REST endpoint
 * with a shared API key" flow. That approach had a real reliability
 * gap: if the HTTP call failed (network blip, booking-service briefly
 * restarting), the confirmation was silently lost — payment-service
 * only logged it and moved on, since it couldn't afford to undo an
 * already-successful charge just because a notification failed.
 *
 * With RabbitMQ in the loop, that gap mostly closes: if this listener
 * throws, the message is nacked and RabbitMQ redelivers it once
 * booking-service is healthy again, instead of the event being lost
 * the moment the HTTP call fails. It's not a full outbox pattern
 * (payment-service still publishes "best effort" after its own DB
 * commit), but it's a meaningfully more reliable design than a bare
 * synchronous call with no retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final BookingService bookingService;

    @RabbitListener(queues = PAYMENT_EVENTS_QUEUE)
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Received payment event for booking {} — status: {}", event.bookingId(), event.status());

        if (!"SUCCESS".equals(event.status())) {
            // Payment failed — booking correctly stays PENDING, renter
            // can retry payment. Nothing to confirm.
            return;
        }

        try {
            bookingService.confirmAfterPayment(event.bookingId());
        } catch (IllegalArgumentException e) {
            // Booking was already confirmed/cancelled by the time this
            // was processed (e.g. a redelivered message after a prior
            // successful attempt) — not an error worth retrying forever.
            log.info("Booking {} not confirmable (status conflict): {}", event.bookingId(), e.getMessage());
        }
    }
}
