package com.selfdriveconnect.paymentservice.service;

import com.selfdriveconnect.paymentservice.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A fully self-contained, fake payment processor. No external API,
 * no API keys, no signup, no real money ever moves — this is what
 * makes payment-service free to run forever, with zero risk of an
 * accidental real charge.
 *
 * It mimics the SHAPE of a real gateway integration (an isolated
 * component with a processPayment() call that returns a transaction
 * id or a failure reason) so swapping in a real provider later (e.g.
 * Razorpay/Stripe test mode) means replacing this one class, not
 * touching PaymentService's business logic.
 */
@Component
public class MockPaymentGatewayService {

    public GatewayResult process(boolean simulateFailure) {
        if (simulateFailure) {
            return new GatewayResult(false, null, "Card declined by issuing bank (simulated failure)");
        }

        String transactionId = "MOCK-" + UUID.randomUUID();
        return new GatewayResult(true, transactionId, null);
    }

    public record GatewayResult(boolean success, String transactionId, String failureReason) {
    }
}
