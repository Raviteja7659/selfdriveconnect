package com.selfdriveconnect.reviewservice.config;

public class BookingNotReviewableException extends RuntimeException {
    public BookingNotReviewableException(String message) {
        super(message);
    }
}
