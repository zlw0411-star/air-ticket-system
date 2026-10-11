package com.airline.ticket.flight;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlightVO {
    private Long id;
    private String flightNo;
    private String airline;
    private String departureCity;
    private String arrivalCity;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String aircraftType;
    private String status;
    private BigDecimal economyPrice;
    private Integer economyAvailable;
    private Integer economyTotal;
    private BigDecimal businessPrice;
    private Integer businessAvailable;
    private Integer businessTotal;
}
