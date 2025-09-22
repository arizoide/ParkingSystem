package com.estapar.teste.ParkingSystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookEventDTO {
    @JsonProperty("license_plate")
    private String licensePlate;

    @JsonProperty("entry_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    private LocalDateTime entryTime;

    @JsonProperty("exit_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    private LocalDateTime exitTime;

    private Double lat;
    private Double lng;

    @JsonProperty("event_type")
    private String eventType;
}