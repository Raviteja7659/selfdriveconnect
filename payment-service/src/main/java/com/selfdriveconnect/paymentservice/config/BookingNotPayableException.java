package com.selfdriveconnect.paymentservice.config;

public class BookingNotPayableException extends RuntimeException {
    public BookingNotPayableException(String message) {
        super(message);
    }
}
