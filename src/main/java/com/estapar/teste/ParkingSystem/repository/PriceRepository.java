package com.estapar.teste.ParkingSystem.repository;

import com.estapar.teste.ParkingSystem.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
}