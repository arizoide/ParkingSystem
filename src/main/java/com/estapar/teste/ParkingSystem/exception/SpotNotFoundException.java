package com.estapar.teste.ParkingSystem.exception;

public class SpotNotFoundException extends RuntimeException {
    public SpotNotFoundException(String message) {
        super(message);
    }
}
