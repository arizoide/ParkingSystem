package com.estapar.teste.ParkingSystem.service;

import com.estapar.teste.ParkingSystem.entity.Sector;
import com.estapar.teste.ParkingSystem.entity.Spot;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class FetcherService {
    private final ParkingService parkingService;

    public FetcherService(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGarageDataOnStartup() {
        fetchAndSaveGarageData();
    }

    private void fetchAndSaveGarageData() {
        String garageUrl = parkingService.getSimulatorUrl() + "/garage";
        try {
            ResponseEntity<String> response = parkingService.getRestTemplate().getForEntity(garageUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = parkingService.getObjectMapper().readTree(response.getBody());
                JsonNode garageData = root.get("garage");
                JsonNode spotsData = root.get("spots");

                parkingService.getSectorRepository().deleteAll();
                parkingService.getSpotRepository().deleteAll();

                for (JsonNode sectorNode : garageData) {
                    Sector sector = parkingService.getObjectMapper().treeToValue(sectorNode, Sector.class);
                    parkingService.getSectorRepository().save(sector);
                }

                for (JsonNode spotNode : spotsData) {
                    Spot spot = parkingService.getObjectMapper().treeToValue(spotNode, Spot.class);
                    parkingService.getSpotRepository().save(spot);
                }
                System.out.println("Dados da garagem inicializados com sucesso.");
            } else {
                System.err.println("Falha ao obter dados da garagem do simulador: " + response.getStatusCode());
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar dados da garagem: " + e.getMessage());
        }
    }
}