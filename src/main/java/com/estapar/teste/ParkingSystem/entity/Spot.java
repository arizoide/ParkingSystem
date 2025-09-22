package com.estapar.teste.ParkingSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "spot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double lat;
    private Double lng;

    @Column(nullable = false)
    private boolean occupied = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_session_id", nullable = true)
    private ParkingSession currentParkingSession;


    public Spot(Double lat, Double lng, Sector sector) {
        this.lat = lat;
        this.lng = lng;
        this.sector = sector;
    }
}