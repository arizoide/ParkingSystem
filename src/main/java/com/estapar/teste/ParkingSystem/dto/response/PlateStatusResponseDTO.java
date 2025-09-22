package com.estapar.teste.ParkingSystem.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
public class PlateStatusResponseDTO {
    private String licensePlate;
    private double priceUntilNow;
    private LocalDateTime entryTime;
   // private LocalDateTime timeParked;
    private Duration timeParked;

}