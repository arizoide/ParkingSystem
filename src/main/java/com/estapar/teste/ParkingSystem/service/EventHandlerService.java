package com.estapar.teste.ParkingSystem.service;

import com.estapar.teste.ParkingSystem.entity.ParkingSession;
import com.estapar.teste.ParkingSystem.entity.Spot;
import com.estapar.teste.ParkingSystem.exception.SectorFullException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class EventHandlerService {

    private final ParkingService parkingService;

    public EventHandlerService(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    public ResponseEntity<String> handleWebhookEvent(@RequestBody JsonNode event) {
        String eventType = event.get("event_type").asText();
        String licensePlate = event.get("license_plate").asText();

        switch (eventType) {
            case "ENTRY":
                LocalDateTime entryTime = LocalDateTime.parse(event.get("entry_time").asText(), DateTimeFormatter.ISO_DATE_TIME);
                handleEntry(licensePlate, entryTime);
                break;
            case "PARKED":
                double lat = event.get("lat").asDouble();
                double lng = event.get("lng").asDouble();
                handleParked(licensePlate, lat, lng);
                break;
            case "EXIT":
                LocalDateTime exitTime = LocalDateTime.parse(event.get("exit_time").asText(), DateTimeFormatter.ISO_DATE_TIME);
                handleExit(licensePlate, exitTime);
                break;
            default:
                System.out.println("Tipo de evento webhook desconhecido: " + eventType);
                return ResponseEntity.badRequest().body("Tipo de evento desconhecido");
        }
        return ResponseEntity.ok("Evento " + eventType + " processado.");
    }

    void handleEntry(String licensePlate, LocalDateTime entryTime) {
        Optional<Spot> spot = parkingService.getSpotRepository().findFirstAvailableSpot();
        if (spot.isPresent()) {
            ParkingSession session = new ParkingSession();
            session.setLicensePlate(licensePlate);
            session.setEntryTime(entryTime);
            session.setSpot(spot.get());
            parkingService.getSessionRepository().save(session);
            System.out.println("Veículo " + licensePlate + " entrou às " + entryTime);
        } else {
            System.out.println("Setor Lotado");
            throw new SectorFullException("Setor Lotado"); // Lança a exceção SectorFullException
        }
    }

    void handleParked(String licensePlate, double lat, double lng) {
        Optional<Spot> optSpot = Optional.ofNullable(parkingService.getSpotRepository().findByLatAndLng(lat, lng));
        Optional<ParkingSession> optSession = parkingService.getSessionRepository().findByLicensePlateAndExitTimeIsNull(licensePlate);

        if (optSpot.isPresent() && optSession.isPresent()) {
            Spot spot = optSpot.get();
            ParkingSession session = optSession.get();
            if (spot.isOccupied()) {
                System.out.println("Vaga já ocupada: " + spot.getId());
                return;
            }
            spot.setOccupied(true);
            spot.setCurrentParkingSession(session);
            parkingService.getSpotRepository().save(spot);
            session.setSpot(spot);
            parkingService.getSessionRepository().save(session);
            System.out.println("Veículo " + licensePlate + " estacionou na vaga " + spot.getId() + ".");
        } else {
            System.out.println("Não foi possível estacionar " + licensePlate + " na vaga (vaga não encontrada ou ocupada).");
        }
    }

    void handleExit(String licensePlate, LocalDateTime exitTime) {
        Optional<ParkingSession> optSession = parkingService.getSessionRepository().findByLicensePlateAndExitTimeIsNull(licensePlate);
        if (optSession.isPresent()) {
            ParkingSession session = optSession.get();
            session.setExitTime(exitTime);

            Spot spot = session.getSpot();
            if (spot != null) {
                spot.setOccupied(false);
                spot.setCurrentParkingSession(null);
                parkingService.getSpotRepository().save(spot);
            }
            Duration duration = Duration.between(session.getEntryTime(), exitTime);
            session.setDuration(duration);
            double price = parkingService.calculatePrice(session);
            session.setPrice(price);
            parkingService.getSessionRepository().save(session);

            System.out.println("Veículo " + licensePlate + " saiu às " + exitTime + ", preço: " + price + ".");
        } else {
            System.out.println("Veículo " + licensePlate + " não encontrado ou não estava estacionado.");
        }
    }
}