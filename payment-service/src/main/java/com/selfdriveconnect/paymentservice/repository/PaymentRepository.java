package com.selfdriveconnect.paymentservice.repository;

import com.selfdriveconnect.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingId(Long bookingId);

    List<Payment> findByRenterId(Long renterId);

    List<Payment> findByOwnerId(Long ownerId);

    Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, Payment.PaymentStatus status);

    boolean existsByBookingIdAndStatus(Long bookingId, Payment.PaymentStatus status);
}
