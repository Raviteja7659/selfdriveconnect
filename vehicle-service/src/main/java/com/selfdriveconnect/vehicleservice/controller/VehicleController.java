package com.selfdriveconnect.vehicleservice.controller;

import com.selfdriveconnect.vehicleservice.dto.VehicleRequest;
import com.selfdriveconnect.vehicleservice.dto.VehicleResponse;
import com.selfdriveconnect.vehicleservice.entity.Vehicle;
import com.selfdriveconnect.vehicleservice.security.AuthenticatedUser;
import com.selfdriveconnect.vehicleservice.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponse> addVehicle(@Valid @RequestBody VehicleRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return new ResponseEntity<>(vehicleService.addVehicle(request, user.userId()), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping("/search")
    public ResponseEntity<List<VehicleResponse>> searchByCity(@RequestParam String city) {
        return ResponseEntity.ok(vehicleService.searchByCity(city));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(ownerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable Long id,
                                                           @Valid @RequestBody VehicleRequest request,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request, user.userId(), user.hasRole("ADMIN")));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VehicleResponse> updateStatus(@PathVariable Long id,
                                                          @RequestParam Vehicle.VehicleStatus status,
                                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(vehicleService.updateStatus(id, status, user.hasRole("ADMIN")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        vehicleService.deleteVehicle(id, user.userId(), user.hasRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }
}
