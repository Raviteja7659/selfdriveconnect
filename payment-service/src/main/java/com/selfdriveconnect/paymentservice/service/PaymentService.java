package com.selfdriveconnect.paymentservice.service;

import com.selfdriveconnect.paymentservice.client.BookingDto;
import com.selfdriveconnect.paymentservice.client.BookingServiceClient;
import com.selfdriveconnect.paymentservice.config.BookingNotPayableException;
import com.selfdriveconnect.paymentservice.config.DuplicatePaymentException;
import com.selfdriveconnect.paymentservice.config.ResourceNotFoundException;
import com.selfdriveconnect.paymentservice.dto.PaymentRequest;
import com.selfdriveconnect.paymentservice.dto.PaymentResponse;
import com.selfdriveconnect.paymentservice.entity.Payment;
import com.selfdriveconnect.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;
    private final MockPaymentGatewayService mockGateway;

    @Value("${internal.service-key}")
    private String internalApiKey;

    /**
     * Initiates payment for a booking. Order of operations matters:
     *
     * 1. Fetch the booking (server-side truth for who owns it and
     *    what it costs — never trust client input for either)
     * 2. Confirm the caller is actually the renter on that booking
     * 3. Confirm the booking is in a payable state (PENDING, and not
     *    already paid)
     * 4. Run it through the (mock) gateway
     * 5. Record the result
     * 6. On success, tell booking-service to auto-confirm the booking
     *
     * Step 6 is a cross-service side effect that can fail independently
     * of the payment itself — see the comment further down for how
     * that's handled and what the "correct" production fix would be.
     */
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, Long renterId) {
        BookingDto booking = bookingServiceClient.getBookingById(request.getBookingId());

        if (!booking.getRenterId().equals(renterId)) {
            throw new AccessDeniedException("You can only pay for your own bookings");
        }

        if (!"PENDING".equals(booking.getStatus())) {
            throw new BookingNotPayableException(
                    "Booking " + booking.getId() + " is not payable (status: " + booking.getStatus() + ")");
        }

        if (paymentRepository.existsByBookingIdAndStatus(booking.getId(), Payment.PaymentStatus.SUCCESS)) {
            throw new DuplicatePaymentException(
                    "Booking " + booking.getId() + " has already been paid for");
        }

        MockPaymentGatewayService.GatewayResult result = mockGateway.process(request.isSimulateFailure());

        Payment payment = Payment.builder()
                .bookingId(booking.getId())
                .renterId(renterId)
                .ownerId(booking.getOwnerId())
                .amount(booking.getTotalAmount())
                .currency("INR")
                .method(request.getMethod())
                .status(result.success() ? Payment.PaymentStatus.SUCCESS : Payment.PaymentStatus.FAILED)
                .transactionId(result.transactionId())
                .failureReason(result.failureReason())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} for booking {} — status: {}", saved.getId(), saved.getBookingId(), saved.getStatus());

        if (result.success()) {
            confirmBookingAfterPayment(booking.getId());
        }

        return PaymentResponse.fromEntity(saved);
    }

    /**
     * Tells booking-service to flip PENDING -> CONFIRMED now that
     * payment succeeded. This is a "best effort" call: if it fails
     * (booking-service down, network blip), the PAYMENT record still
     * stands as SUCCESS — we do not roll back a successful charge just
     * because a downstream notification failed.
     *
     * This is a known simplification. The correct production fix is
     * an outbox pattern / message queue (e.g. publish a
     * "PaymentSucceeded" event to Kafka/RabbitMQ, booking-service
     * consumes it and confirms asynchronously with retries) so this
     * step can't be silently lost. Worth calling out explicitly in
     * an interview as "I know this is the weak point, here's how I'd
     * harden it."
     */
    private void confirmBookingAfterPayment(Long bookingId) {
        try {
            bookingServiceClient.confirmAfterPayment(bookingId, internalApiKey);
        } catch (Exception e) {
            log.error("Payment succeeded for booking {} but confirming the booking failed: {}. " +
                    "Booking remains PENDING until manually reconciled.", bookingId, e.getMessage());
        }
    }

    public PaymentResponse getPaymentById(Long id, Long requesterId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        boolean isParty = payment.getRenterId().equals(requesterId) || payment.getOwnerId().equals(requesterId);
        if (!isAdmin && !isParty) {
            throw new AccessDeniedException("You do not have permission to view this payment");
        }

        return PaymentResponse.fromEntity(payment);
    }

    public List<PaymentResponse> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    public List<PaymentResponse> getPaymentsByRenter(Long renterId) {
        return paymentRepository.findByRenterId(renterId).stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    public List<PaymentResponse> getPaymentsByOwner(Long ownerId) {
        return paymentRepository.findByOwnerId(ownerId).stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    /**
     * Refunds a successful payment. Only the vehicle owner or an admin
     * can trigger a refund — mirrors the same ownership pattern used
     * for confirm/complete in booking-service. Also asks booking-service
     * to cancel the underlying booking; that call is made using the
     * OWNER's own JWT (forwarded automatically by FeignClientConfig),
     * which booking-service's normal cancel rule already permits.
     */
    @Transactional
    public PaymentResponse refundPayment(Long id, Long requesterId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (!isAdmin && !payment.getOwnerId().equals(requesterId)) {
            throw new AccessDeniedException("Only the vehicle owner can refund this payment");
        }

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException(
                    "Only a SUCCESS payment can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);

        try {
            bookingServiceClient.cancelBooking(payment.getBookingId(), "Refunded by owner/admin");
        } catch (Exception e) {
            log.error("Payment {} refunded but cancelling booking {} failed: {}. " +
                    "Booking may need manual cancellation.", saved.getId(), payment.getBookingId(), e.getMessage());
        }

        return PaymentResponse.fromEntity(saved);
    }
}
