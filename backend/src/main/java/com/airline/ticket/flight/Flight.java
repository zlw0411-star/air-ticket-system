package com.airline.ticket.flight;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("flight")
public class Flight {
    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
