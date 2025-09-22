package com.estapar.teste.ParkingSystem.service;

import com.estapar.teste.ParkingSystem.dto.WebhookEventDTO;
import com.estapar.teste.ParkingSystem.entity.ParkingSession;
import com.estapar.teste.ParkingSystem.entity.Sector;
import com.estapar.teste.ParkingSystem.entity.Spot;
import com.estapar.teste.ParkingSystem.repository.ParkingSessionRepository;
import com.estapar.teste.ParkingSystem.repository.SectorRepository;
import com.estapar.teste.ParkingSystem.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebhookEventService {

    private final SpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final SectorRepository sectorRepository;

    public void processEvent(WebhookEventDTO event) {
        switch (event.getEventType()) {
            case "ENTRY":
                handleEntry(event);
                break;
            case "PARKED":
                handleParked(event);
                break;
            case "EXIT":
                handleExit(event);
                break;
        }
    }

    private void handleEntry(WebhookEventDTO event) {
        ParkingSession session = new ParkingSession();
        session.setLicensePlate(event.getLicensePlate());
        session.setEntryTime(event.getEntryTime());
        sessionRepository.save(session);
    }

    private void handleParked(WebhookEventDTO event) {
        Optional<Spot> optSpot = Optional.ofNullable(spotRepository.findByLatAndLng(event.getLat(), event.getLng()));
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(event.getLicensePlate());

        if (optSpot.isPresent() && optSession.isPresent()) {
            Spot spot = optSpot.get();
            ParkingSession session = optSession.get();

            spot.setOccupied(true);
            spot.setCurrentParkingSession(session);
            spotRepository.save(spot);
            session.setSpot(spot);
            sessionRepository.save(session);
        }
    }

    private void handleExit(WebhookEventDTO event) {
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(event.getLicensePlate());
        if (optSession.isPresent()) {
            ParkingSession session = optSession.get();
            session.setExitTime(event.getExitTime());

            Spot spot = session.getSpot();
            if (spot != null) {
                spot.setOccupied(false);
                spot.setCurrentParkingSession(null);
                spotRepository.save(spot);
            }
            Duration duration = Duration.between(session.getEntryTime(), event.getExitTime());
            session.setDuration(duration);
            double price = calculatePrice(session);
            session.setPrice(price);
            sessionRepository.save(session);
        }
    }

    private double calculatePrice(ParkingSession session) {
        long minutesParked = session.getDuration().toMinutes();
        Sector sector = session.getSpot().getSector();

        if (sector == null) {
            return 0.0;
        }

        double basePrice = sector.getBasePrice();
        double dynamicPrice = applyDynamicPricing(basePrice, sector.getName());
        return dynamicPrice * (minutesParked / 60.0);
    }

    private double applyDynamicPricing(double basePrice, String sectorName) {
        Sector sector = sectorRepository.findByName(sectorName);
        if (sector == null) {
            return basePrice;
        }
        long occupiedSpots = spotRepository.countBySectorNameAndOccupied(sectorName, true);
        double occupancyRate = (double) occupiedSpots / sector.getMaxCapacity();

        if (occupancyRate < 0.25) {
            return basePrice * 0.9;
        } else if (occupancyRate <= 0.50) {
            return basePrice;
        } else if (occupancyRate <= 0.75) {
            return basePrice * 1.1;
        } else {
            return basePrice * 1.25;
        }
    }
}

