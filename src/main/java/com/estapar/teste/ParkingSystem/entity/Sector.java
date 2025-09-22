package com.estapar.teste.ParkingSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "sector")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double basePrice;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private LocalTime openHour;

    @Column(nullable = false)
    private LocalTime closeHour;

    @Column(nullable = false)
    private Integer durationLimitMinutes;

    public Sector(String name, Double basePrice, Integer maxCapacity, LocalTime openHour, LocalTime closeHour, Integer durationLimitMinutes) {
        this.name = name;
        this.basePrice = basePrice;
        this.maxCapacity = maxCapacity;
        this.openHour = openHour;
        this.closeHour = closeHour;
        this.durationLimitMinutes = durationLimitMinutes;
    }
}