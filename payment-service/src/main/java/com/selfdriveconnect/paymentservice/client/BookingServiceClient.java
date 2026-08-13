package com.selfdriveconnect.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "booking-service", url = "${booking-service.url}", configuration = FeignClientConfig.class)
public interface BookingServiceClient {

    @GetMapping("/api/v1/bookings/{id}")
    BookingDto getBookingById(@PathVariable("id") Long id);

    /**
     * Calls booking-service's internal-only endpoint that flips a
     * booking PENDING -> CONFIRMED after a successful payment. This is
     * a service-to-service call, authenticated with a shared internal
     * API key (X-Internal-Api-Key) rather than a user's JWT — the
     * renter who paid didn't necessarily have permission to confirm
     * their own booking under the normal (owner-only) rule, so this
     * bypasses that via a trusted service-to-service path instead.
     */
    @RequestMapping(method = RequestMethod.PATCH, value = "/api/v1/bookings/{id}/internal/confirm-after-payment")
    BookingDto confirmAfterPayment(@PathVariable("id") Long id,
                                    @RequestHeader("X-Internal-Api-Key") String internalApiKey);

    @RequestMapping(method = RequestMethod.PATCH, value = "/api/v1/bookings/{id}/cancel")
    BookingDto cancelBooking(@PathVariable("id") Long id, @RequestParam("reason") String reason);
}
