package com.selfdriveconnect.paymentservice.dto;

import com.selfdriveconnect.paymentservice.entity.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotNull(message = "method is required")
    private Payment.PaymentMethod method;

    /**
     * Testing hook ONLY — lets you deliberately trigger a FAILED
     * payment to exercise that code path, since the mock gateway
     * otherwise always succeeds. Defaults to false. A real gateway
     * integration would remove this field entirely; failures there
     * come from the actual processor, not a client-supplied flag.
     */
    private boolean simulateFailure = false;

    // Note: amount is intentionally NOT a field here. The amount is
    // always the booking's own totalAmount, fetched server-side from
    // booking-service — never trusted from the client. Same principle
    // as booking-service never trusting a client-supplied price.
}
