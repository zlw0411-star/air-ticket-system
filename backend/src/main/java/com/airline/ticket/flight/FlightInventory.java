package com.airline.ticket.flight;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("flight_inventory")
public class FlightInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long flightId;
    private String cabinType;
    private BigDecimal price;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer version;
}
