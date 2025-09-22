package com.estapar.teste.ParkingSystem.repository;

import com.estapar.teste.ParkingSystem.entity.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpotRepository extends JpaRepository<Spot, Long> {
    Spot findByLatAndLng(double lat, double lng);
    List<Spot> findBySectorName(String sectorName);
    long countBySectorNameAndOccupied(String sectorName, boolean occupied);

    @Query("SELECT s FROM Spot s WHERE s.occupied = false ORDER BY s.id ASC")
    Optional<Spot> findFirstAvailableSpot();

    Optional<Spot> findFirstByOccupiedFalse();
}