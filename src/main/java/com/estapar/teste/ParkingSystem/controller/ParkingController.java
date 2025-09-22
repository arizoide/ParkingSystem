package com.estapar.teste.ParkingSystem.controller;

import com.estapar.teste.ParkingSystem.service.EventHandlerService;
import com.estapar.teste.ParkingSystem.service.FetcherService;
import com.estapar.teste.ParkingSystem.service.ParkingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingService parkingService;
    private final EventHandlerService eventHandlerService;

    private static final Logger logger = LoggerFactory.getLogger(ParkingController.class);

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhookEvent(@RequestBody JsonNode event) {
        try {
            ResponseEntity<String> response = eventHandlerService.handleWebhookEvent(event);
            logger.info("Webhook event handled successfully. Event type: {}", event.get("event_type").asText());
            return response;
        } catch (DateTimeParseException e) {
            logger.error("Error parsing datetime in webhook event", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid datetime format");
        } catch (Exception e) {
            logger.error("Error handling webhook event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @PostMapping("/plate-status")
    public ResponseEntity<?> getVehicleStatus(@RequestBody Map<String, String> request) {
        String licensePlate = request.get("license_plate");
        return parkingService.getVehicleStatus(licensePlate);
    }

    @PostMapping("/spot-status")
    public ResponseEntity<?> getSpotStatus(@RequestBody Map<String, Double> request) {
        double lat = request.get("lat");
        double lng = request.get("lng");
        return parkingService.getSpotStatus(lat, lng);
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenue(
            @RequestParam("date") String date,
            @RequestParam("sector") String sector) {
        return parkingService.getRevenue(date, sector);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(Exception e) {
        logger.error("Global exception handler: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
    }
}