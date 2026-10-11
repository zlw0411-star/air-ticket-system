package com.airline.ticket.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reschedule_record")
public class RescheduleRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long fromFlightId;
    private Long toFlightId;
    private String fromCabin;
    private String toCabin;
    private BigDecimal priceDiff;
    private LocalDateTime createdAt;
}
