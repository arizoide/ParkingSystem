package com.estapar.teste.ParkingSystem.repository;

import com.estapar.teste.ParkingSystem.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
}