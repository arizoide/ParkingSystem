package com.estapar.teste.ParkingSystem.service;

import com.estapar.teste.ParkingSystem.entity.ParkingSession;
import com.estapar.teste.ParkingSystem.entity.Sector;
import com.estapar.teste.ParkingSystem.entity.Spot;
import com.estapar.teste.ParkingSystem.exception.SectorFullException;
import com.estapar.teste.ParkingSystem.repository.ParkingSessionRepository;
import com.estapar.teste.ParkingSystem.repository.PriceRepository;
import com.estapar.teste.ParkingSystem.repository.SectorRepository;
import com.estapar.teste.ParkingSystem.repository.SpotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ParkingService {

    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PriceRepository priceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${simulator.url}")
    private String simulatorUrl;

    public ParkingService(SectorRepository sectorRepository, SpotRepository spotRepository, ParkingSessionRepository sessionRepository, PriceRepository priceRepository, RestTemplate restTemplate, ObjectMapper objectMapper) { // Adicionado priceRepository ao construtor
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
        this.sessionRepository = sessionRepository;
        this.priceRepository = priceRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGarageDataOnStartup() {
        fetchAndSaveGarageData();
    }

    private void fetchAndSaveGarageData() {
        String garageUrl = simulatorUrl + "/garage";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(garageUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode garageData = root.get("garage");
                JsonNode spotsData = root.get("spots");

                sectorRepository.deleteAll();
                spotRepository.deleteAll();

                for (JsonNode sectorNode : garageData) {
                    Sector sector = objectMapper.treeToValue(sectorNode, Sector.class);
                    sectorRepository.save(sector);
                }

                for (JsonNode spotNode : spotsData) {
                    Spot spot = objectMapper.treeToValue(spotNode, Spot.class);
                    spotRepository.save(spot);
                }
                System.out.println("Dados da garagem inicializados com sucesso.");
            } else {
                System.err.println("Falha ao obter dados da garagem do simulador: " + response.getStatusCode());
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar dados da garagem: " + e.getMessage());
        }
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
        Optional<Spot> spot = spotRepository.findFirstAvailableSpot();
        if (spot.isPresent()) {
            ParkingSession session = new ParkingSession();
            session.setLicensePlate(licensePlate);
            session.setEntryTime(entryTime);
            session.setSpot(spot.get());
            sessionRepository.save(session);
            System.out.println("Veículo " + licensePlate + " entrou às " + entryTime);
        } else {
            System.out.println("Setor Lotado");
            throw new SectorFullException("Setor Lotado"); // Lança a exceção SectorFullException
        }
    }

    private void handleParked(String licensePlate, double lat, double lng) {
        Optional<Spot> optSpot = Optional.ofNullable(spotRepository.findByLatAndLng(lat, lng));
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(licensePlate);

        if (optSpot.isPresent() && optSession.isPresent()) {
            Spot spot = optSpot.get();
            ParkingSession session = optSession.get();
            if (spot.isOccupied()) {
                System.out.println("Vaga já ocupada: " + spot.getId());
                return;
            }
            spot.setOccupied(true);
            spot.setCurrentParkingSession(session);
            spotRepository.save(spot);
            session.setSpot(spot);
            sessionRepository.save(session);
            System.out.println("Veículo " + licensePlate + " estacionou na vaga " + spot.getId() + ".");
        } else {
            System.out.println("Não foi possível estacionar " + licensePlate + " na vaga (vaga não encontrada ou ocupada).");
        }
    }

    void handleExit(String licensePlate, LocalDateTime exitTime) {
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(licensePlate);
        if (optSession.isPresent()) {
            ParkingSession session = optSession.get();
            session.setExitTime(exitTime);

            Spot spot = session.getSpot();
            if (spot != null) {
                spot.setOccupied(false);
                spot.setCurrentParkingSession(null);
                spotRepository.save(spot);
            }
            Duration duration = Duration.between(session.getEntryTime(), exitTime);
            session.setDuration(duration);
            double price = calculatePrice(session);
            session.setPrice(price);
            sessionRepository.save(session);

            System.out.println("Veículo " + licensePlate + " saiu às " + exitTime + ", preço: " + price + ".");
        } else {
            System.out.println("Veículo " + licensePlate + " não encontrado ou não estava estacionado.");
        }
    }

    /**
     * Calculate the price of a parking session.
     *
     * @param session a parking session
     * @return the price of the parking session
     */
    private double calculatePrice(ParkingSession session) {
        // If the parking session is missing some required information, return 0.0
        if (session.getEntryTime() == null || session.getExitTime() == null || session.getSpot() == null) {
            return 0.0;
        }

        // Calculate the duration of the parking session in minutes
        long minutesParked = session.getDuration().toMinutes();

        // Get the sector of the parking spot
        Sector sector = session.getSpot().getSector();

        // If the sector is missing, return 0.0
        if (sector == null) {
            return 0.0;
        }

        // Calculate the base price of the parking session
        double basePrice = sector.getBasePrice();

        // Apply dynamic pricing to the base price
        double dynamicPrice = applyDynamicPricing(basePrice, sector.getName());

        // Return the price of the parking session
        return dynamicPrice * (minutesParked / 60.0);
    }

    /**
     * Apply dynamic pricing to a parking session based on the occupancy rate of the sector.
     *
     * @param basePrice  the base price of the parking session
     * @param sectorName the name of the sector
     * @return the price of the parking session with dynamic pricing applied
     */
    private double applyDynamicPricing(double basePrice, String sectorName) {
        // Get the sector by name
        Sector sector = sectorRepository.findByName(sectorName);
        if (sector == null) {
            // If the sector is not found, return the base price
            return basePrice;
        }

        // Calculate the occupancy rate of the sector
        long occupiedSpots = spotRepository.countBySectorNameAndOccupied(sectorName, true);
        double occupancyRate = (double) occupiedSpots / sector.getMaxCapacity();

        // Apply dynamic pricing based on the occupancy rate
        if (occupancyRate < 0.25) {
            // If the occupancy rate is below 25%, apply a 10% discount
            return basePrice * 0.9;
        } else if (occupancyRate <= 0.50) {
            // If the occupancy rate is between 25% and 50%, do not apply any discount
            return basePrice;
        } else if (occupancyRate <= 0.75) {
            // If the occupancy rate is between 50% and 75%, apply a 10% surcharge
            return basePrice * 1.1;
        } else {
            // If the occupancy rate is above 75%, apply a 25% surcharge
            return basePrice * 1.25;
        }
    }

    /**
     * Apply dynamic pricing based on the occupancy rate of the sector.
     * If the occupancy rate is below 25%, apply a 10% discount.
     * If the occupancy rate is between 25% and 50%, do not apply any discount.
     * If the occupancy rate is between 50% and 75%, apply a 10% surcharge.
     * If the occupancy rate is above 75%, apply a 25% surcharge.
     * @param licensePlate the license plate of the vehicle
     * @return the status of the vehicle with the given license plate
     */
    public ResponseEntity<?> getVehicleStatus(String licensePlate) {
        // Find the parking session with the given license plate
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(licensePlate);
        if (optSession.isEmpty()) {
            // If the parking session is not found, return a 404 response with an error message
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Veículo não encontrado"));
        }

        // Get the parking session
        ParkingSession session = optSession.get();

        // Calculate the time parked by the vehicle
        String timeParked = session.getDuration() != null
                ? String.format("%02d:%02d:%02d", session.getDuration().toHours(), session.getDuration().toMinutesPart(), session.getDuration().toSecondsPart())
                : "N/A";

        // Create the response map
        Map<String, Object> response = new HashMap<>();
        response.put("license_plate", session.getLicensePlate());
        response.put("price_until_now", session.getPrice() != null ? session.getPrice() : 0.00);
        response.put("entry_time", session.getEntryTime() != null ? session.getEntryTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        response.put("time_parked", timeParked);

        // Return the response
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the status of a parking spot given its latitude and longitude.
     * @param lat the latitude of the parking spot
     * @param lng the longitude of the parking spot
     * @return a response entity containing the status of the parking spot
     */
    public ResponseEntity<?> getSpotStatus(double lat, double lng) {
        // Find the parking spot with the given latitude and longitude
        Optional<Spot> optSpot = Optional.ofNullable(spotRepository.findByLatAndLng(lat, lng));
        if (optSpot.isEmpty()) {
            // If the parking spot is not found, return a 404 response with an error message
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Vaga não encontrada"));
        }

        // Get the parking spot
        Spot spot = optSpot.get();

        // Find the current parking session associated with the parking spot
        Optional<ParkingSession> optSession = (spot.getCurrentParkingSession() != null) ? Optional.of(spot.getCurrentParkingSession()) : Optional.empty();

        // Calculate the time parked by the vehicle
        String timeParked = optSession.isPresent() && spot.isOccupied()
                ? String.format("%02d:%02d:%02d", optSession.get().getDuration().toHours(), optSession.get().getDuration().toMinutesPart(), optSession.get().getDuration().toSecondsPart())
                : "N/A";

        // Create the response map
        Map<String, Object> response = new HashMap<>();
        response.put("occupied", spot.isOccupied());
        response.put("entry_time", optSession.isPresent() ? optSession.get().getEntryTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        response.put("time_parked", timeParked);

        // Return the response
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the total revenue of a sector for a given date.
     * @param dateStr the date in the format AAAA-MM-DD
     * @param sectorName the name of the sector
     * @return a response entity containing the total revenue of the sector for the given date
     */
    public ResponseEntity<?> getRevenue(String dateStr, String sectorName) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate date;
        try {
            // Parse the date string to a LocalDate object
            date = LocalDate.parse(dateStr, dateFormatter);
        } catch (Exception e) {
            // Return a bad request response if the date string is invalid
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de data inválido (AAAA-MM-DD)"));
        }

        // Find the sector by name
        Sector sector = sectorRepository.findByName(sectorName);
        if (sector == null) {
            // Return a not found response if the sector is not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Setor não encontrado"));
        }

        // Find all parking sessions that exited on the given date and are associated with the given sector
        List<ParkingSession> sessions = sessionRepository.findAll().stream()
                .filter(s -> s.getExitTime() != null &&
                        s.getExitTime().toLocalDate().isEqual(date) &&
                        s.getSpot() != null &&
                        s.getSpot().getSector().getName().equals(sectorName) &&
                        s.getPrice() != null)
                .collect(Collectors.toList());

        // Calculate the total revenue of the sector for the given date
        double totalRevenue = sessions.stream().mapToDouble(ParkingSession::getPrice).sum();

        // Create the response map
        Map<String, Object> response = new HashMap<>();
        response.put("amount", totalRevenue);
        response.put("currency", "BRL");
        response.put("timestamp", LocalDateTime.now(java.time.Clock.systemDefaultZone()).format(DateTimeFormatter.ISO_DATE_TIME));

        // Return the response
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the first available spot in the database.
     * @return an Optional containing the first available spot, or an empty Optional if no available spots are found
     */
    public Optional<Spot> findFirstAvailableSpot() {
        // Find the first available spot in the database
        // A spot is considered available if it is not occupied
        return spotRepository.findFirstByOccupiedFalse();
    }
}



