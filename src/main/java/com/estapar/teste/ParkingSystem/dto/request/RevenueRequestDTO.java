package com.estapar.teste.ParkingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RevenueRequestDTO {
    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Sector is required")
    private String sector;
}