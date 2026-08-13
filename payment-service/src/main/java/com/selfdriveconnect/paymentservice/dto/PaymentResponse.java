package com.selfdriveconnect.paymentservice.dto;

import com.selfdriveconnect.paymentservice.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private Long renterId;
    private Long ownerId;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentMethod method;
    private Payment.PaymentStatus status;
    private String transactionId;
    private String failureReason;
    private LocalDateTime createdAt;

    public static PaymentResponse fromEntity(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .bookingId(p.getBookingId())
                .renterId(p.getRenterId())
                .ownerId(p.getOwnerId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .method(p.getMethod())
                .status(p.getStatus())
                .transactionId(p.getTransactionId())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
