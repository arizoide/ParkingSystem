package com.estapar.teste.ParkingSystem.repository;

import com.estapar.teste.ParkingSystem.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    Optional<ParkingSession> findByLicensePlateAndExitTimeIsNull(String licensePlate);
}