package com.selfdriveconnect.bookingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "vehicle-service", url = "${vehicle-service.url}")
public interface VehicleServiceClient {

    @GetMapping("/api/v1/vehicles/{id}")
    VehicleDto getVehicleById(@PathVariable("id") Long id);
}
