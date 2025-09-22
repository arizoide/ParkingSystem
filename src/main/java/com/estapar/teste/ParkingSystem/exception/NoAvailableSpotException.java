package com.estapar.teste.ParkingSystem.exception;

public class NoAvailableSpotException extends RuntimeException {
    public NoAvailableSpotException(String message) {
        super(message);
    }
}