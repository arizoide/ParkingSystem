package com.estapar.teste.ParkingSystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="vehicle")
@Getter
@Setter
public class Vehicle {

    @Id
    private String licensePlate;

    public Vehicle(String licensePlate, LocalDateTime entryTime) {
        this.licensePlate = licensePlate;
        // this.entryTime = entryTime;
    }
}

