package com.airline.ticket.flight;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FlightMapper extends BaseMapper<Flight> {
    @Select("SELECT DISTINCT departure_city FROM flight UNION SELECT DISTINCT arrival_city FROM flight")
    List<String> selectCities();
}
