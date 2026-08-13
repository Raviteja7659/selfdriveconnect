package com.selfdriveconnect.reviewservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service", url = "${booking-service.url}", configuration = FeignClientConfig.class)
public interface BookingServiceClient {

    @GetMapping("/api/v1/bookings/{id}")
    BookingDto getBookingById(@PathVariable("id") Long id);
}
