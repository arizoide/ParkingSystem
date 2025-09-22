package com.estapar.teste.ParkingSystem.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RevenueResponseDTO {
    private double amount;
    private String currency = "BRL";
    private LocalDateTime timestamp;
}