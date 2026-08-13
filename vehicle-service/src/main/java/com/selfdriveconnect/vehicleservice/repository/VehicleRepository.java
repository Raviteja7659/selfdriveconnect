package com.selfdriveconnect.vehicleservice.repository;

import com.selfdriveconnect.vehicleservice.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByCityIgnoreCaseAndStatus(String city, Vehicle.VehicleStatus status);

    List<Vehicle> findByOwnerId(Long ownerId);

    List<Vehicle> findByStatus(Vehicle.VehicleStatus status);

    boolean existsByRegistrationNumber(String registrationNumber);
}
