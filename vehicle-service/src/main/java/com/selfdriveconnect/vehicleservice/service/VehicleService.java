package com.selfdriveconnect.vehicleservice.service;

import com.selfdriveconnect.vehicleservice.config.ResourceNotFoundException;
import com.selfdriveconnect.vehicleservice.dto.VehicleRequest;
import com.selfdriveconnect.vehicleservice.dto.VehicleResponse;
import com.selfdriveconnect.vehicleservice.entity.Vehicle;
import com.selfdriveconnect.vehicleservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleResponse addVehicle(VehicleRequest request, Long ownerId) {
        if (vehicleRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new IllegalArgumentException(
                    "A vehicle with registration number " + request.getRegistrationNumber() + " already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .ownerId(ownerId)
                .brand(request.getBrand())
                .model(request.getModel())
                .year(request.getYear())
                .registrationNumber(request.getRegistrationNumber())
                .fuelType(request.getFuelType())
                .transmissionType(request.getTransmissionType())
                .seatingCapacity(request.getSeatingCapacity())
                .pricePerHour(request.getPricePerHour())
                .pricePerDay(request.getPricePerDay())
                .city(request.getCity())
                .pickupLocation(request.getPickupLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(request.getImageUrl())
                .status(Vehicle.VehicleStatus.PENDING_VERIFICATION)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(saved);
    }

    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        return VehicleResponse.fromEntity(vehicle);
    }

    public List<VehicleResponse> searchByCity(String city) {
        return vehicleRepository.findByCityIgnoreCaseAndStatus(city, Vehicle.VehicleStatus.ACTIVE)
                .stream()
                .map(VehicleResponse::fromEntity)
                .toList();
    }

    public List<VehicleResponse> getVehiclesByOwner(Long ownerId) {
        return vehicleRepository.findByOwnerId(ownerId)
                .stream()
                .map(VehicleResponse::fromEntity)
                .toList();
    }

    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(VehicleResponse::fromEntity)
                .toList();
    }

    public VehicleResponse updateVehicle(Long id, VehicleRequest request, Long requesterId, boolean isAdmin) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (!isAdmin && !vehicle.getOwnerId().equals(requesterId)) {
            throw new AccessDeniedException("You do not have permission to modify this vehicle");
        }

        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setTransmissionType(request.getTransmissionType());
        vehicle.setSeatingCapacity(request.getSeatingCapacity());
        vehicle.setPricePerHour(request.getPricePerHour());
        vehicle.setPricePerDay(request.getPricePerDay());
        vehicle.setCity(request.getCity());
        vehicle.setPickupLocation(request.getPickupLocation());
        vehicle.setLatitude(request.getLatitude());
        vehicle.setLongitude(request.getLongitude());
        vehicle.setImageUrl(request.getImageUrl());

        Vehicle updated = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(updated);
    }

    public VehicleResponse updateStatus(Long id, Vehicle.VehicleStatus status, boolean isAdmin) {
        if (!isAdmin) {
            throw new AccessDeniedException("Only an admin can change a vehicle's verification status");
        }
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setStatus(status);
        return VehicleResponse.fromEntity(vehicleRepository.save(vehicle));
    }

    public void deleteVehicle(Long id, Long requesterId, boolean isAdmin) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (!isAdmin && !vehicle.getOwnerId().equals(requesterId)) {
            throw new AccessDeniedException("You do not have permission to delete this vehicle");
        }

        vehicleRepository.deleteById(id);
    }
}
