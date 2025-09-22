package com.estapar.teste.ParkingSystem.exception;

public class TicketNotFoundException extends RuntimeException{
    public TicketNotFoundException(String message){
        super(message);
    }
}