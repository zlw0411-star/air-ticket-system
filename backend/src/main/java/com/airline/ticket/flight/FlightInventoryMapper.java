package com.airline.ticket.flight;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FlightInventoryMapper extends BaseMapper<FlightInventory> {
    @Select("SELECT * FROM flight_inventory WHERE flight_id = #{flightId} AND cabin_type = #{cabinType} FOR UPDATE")
    FlightInventory selectForUpdate(@Param("flightId") Long flightId, @Param("cabinType") String cabinType);

    @Update("""
            UPDATE flight_inventory
            SET available_seats = available_seats - #{qty}, version = version + 1
            WHERE flight_id = #{flightId} AND cabin_type = #{cabinType} AND available_seats >= #{qty}
            """)
    int lockSeats(@Param("flightId") Long flightId, @Param("cabinType") String cabinType, @Param("qty") int qty);

    @Update("""
            UPDATE flight_inventory
            SET available_seats = LEAST(total_seats, available_seats + #{qty}), version = version + 1
            WHERE flight_id = #{flightId} AND cabin_type = #{cabinType}
            """)
    int releaseSeats(@Param("flightId") Long flightId, @Param("cabinType") String cabinType, @Param("qty") int qty);
}
