package com.estapar.teste.ParkingSystem.exception;

public class SectorNotFoundException extends RuntimeException {
    public SectorNotFoundException(String message) {
        super(message);
    }
}