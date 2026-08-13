package com.selfdriveconnect.paymentservice.controller;

import com.selfdriveconnect.paymentservice.dto.PaymentRequest;
import com.selfdriveconnect.paymentservice.dto.PaymentResponse;
import com.selfdriveconnect.paymentservice.security.AuthenticatedUser;
import com.selfdriveconnect.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request,
                                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return new ResponseEntity<>(paymentService.initiatePayment(request, user.userId()), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id,
                                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(paymentService.getPaymentById(id, user.userId(), user.hasRole("ADMIN")));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentsByBooking(bookingId));
    }

    @GetMapping("/renter/{renterId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByRenter(@PathVariable Long renterId) {
        return ResponseEntity.ok(paymentService.getPaymentsByRenter(renterId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOwner(ownerId));
    }

    @PatchMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long id,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(paymentService.refundPayment(id, user.userId(), user.hasRole("ADMIN")));
    }
}
