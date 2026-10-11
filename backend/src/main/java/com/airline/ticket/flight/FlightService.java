package com.airline.ticket.flight;

import com.airline.ticket.common.BusinessException;
import com.airline.ticket.common.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FlightService {
    private final FlightMapper flightMapper;
    private final FlightInventoryMapper inventoryMapper;

    public FlightService(FlightMapper flightMapper, FlightInventoryMapper inventoryMapper) {
        this.flightMapper = flightMapper;
        this.inventoryMapper = inventoryMapper;
    }

    public PageResult<FlightVO> search(String departureCity, String arrivalCity, LocalDate date, int page, int size) {
        LambdaQueryWrapper<Flight> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(departureCity)) {
            wrapper.eq(Flight::getDepartureCity, departureCity);
        }
        if (StringUtils.hasText(arrivalCity)) {
            wrapper.eq(Flight::getArrivalCity, arrivalCity);
        }
        if (date != null) {
            wrapper.ge(Flight::getDepartureTime, date.atStartOfDay())
                    .lt(Flight::getDepartureTime, date.plusDays(1).atStartOfDay());
        }
        wrapper.orderByAsc(Flight::getDepartureTime);
        Page<Flight> result = flightMapper.selectPage(new Page<>(page, size), wrapper);
        List<FlightVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getTotal(), records);
    }

    public FlightVO detail(Long id) {
        Flight flight = requireFlight(id);
        return toVO(flight);
    }

    public List<String> cities() {
        return flightMapper.selectCities();
    }

    public Flight requireFlight(Long id) {
        Flight flight = flightMapper.selectById(id);
        if (flight == null) {
            throw new BusinessException("航班不存在");
        }
        return flight;
    }

    public FlightInventory requireInventory(Long flightId, String cabinType) {
        FlightInventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<FlightInventory>()
                .eq(FlightInventory::getFlightId, flightId)
                .eq(FlightInventory::getCabinType, cabinType));
        if (inventory == null) {
            throw new BusinessException("舱位不存在");
        }
        return inventory;
    }

    public void lockSeats(Long flightId, String cabinType, int qty) {
        int updated = inventoryMapper.lockSeats(flightId, cabinType, qty);
        if (updated == 0) {
            throw new BusinessException("余票不足");
        }
    }

    public void releaseSeats(Long flightId, String cabinType, int qty) {
        inventoryMapper.releaseSeats(flightId, cabinType, qty);
    }

    public FlightVO create(FlightSaveReq req) {
        if (!StringUtils.hasText(req.flightNo()) || req.departureTime() == null || req.arrivalTime() == null
                || req.economyPrice() == null || req.economySeats() == null
                || req.businessPrice() == null || req.businessSeats() == null) {
            throw new BusinessException(400, "航班信息不完整");
        }
        Flight flight = new Flight();
        apply(flight, req);
        flight.setStatus("SCHEDULED");
        flightMapper.insert(flight);
        insertInventory(flight.getId(), "ECONOMY", req.economyPrice(), req.economySeats());
        insertInventory(flight.getId(), "BUSINESS", req.businessPrice(), req.businessSeats());
        return toVO(flight);
    }

    public FlightVO update(Long id, FlightSaveReq req) {
        Flight flight = requireFlight(id);
        apply(flight, req);
        flightMapper.updateById(flight);
        updateInventory(id, "ECONOMY", req.economyPrice(), req.economySeats());
        updateInventory(id, "BUSINESS", req.businessPrice(), req.businessSeats());
        return toVO(flight);
    }

    public void updateStatus(Long id, String status) {
        Flight flight = requireFlight(id);
        flight.setStatus(status);
        flightMapper.updateById(flight);
    }

    public PageResult<FlightVO> adminList(int page, int size) {
        Page<Flight> result = flightMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Flight>().orderByDesc(Flight::getDepartureTime));
        return new PageResult<>(result.getTotal(), result.getRecords().stream().map(this::toVO).toList());
    }

    private void apply(Flight flight, FlightSaveReq req) {
        if (req.flightNo() != null) {
            flight.setFlightNo(req.flightNo());
        }
        if (req.airline() != null) {
            flight.setAirline(req.airline());
        }
        if (req.departureCity() != null) {
            flight.setDepartureCity(req.departureCity());
        }
        if (req.arrivalCity() != null) {
            flight.setArrivalCity(req.arrivalCity());
        }
        if (req.departureAirport() != null) {
            flight.setDepartureAirport(req.departureAirport());
        }
        if (req.arrivalAirport() != null) {
            flight.setArrivalAirport(req.arrivalAirport());
        }
        if (req.departureTime() != null) {
            flight.setDepartureTime(req.departureTime());
        }
        if (req.arrivalTime() != null) {
            flight.setArrivalTime(req.arrivalTime());
        }
        if (req.aircraftType() != null) {
            flight.setAircraftType(req.aircraftType());
        }
    }

    private void insertInventory(Long flightId, String cabin, BigDecimal price, Integer seats) {
        FlightInventory inventory = new FlightInventory();
        inventory.setFlightId(flightId);
        inventory.setCabinType(cabin);
        inventory.setPrice(price);
        inventory.setTotalSeats(seats);
        inventory.setAvailableSeats(seats);
        inventory.setVersion(0);
        inventoryMapper.insert(inventory);
    }

    private void updateInventory(Long flightId, String cabin, BigDecimal price, Integer seats) {
        if (price == null && seats == null) {
            return;
        }
        FlightInventory inventory = requireInventory(flightId, cabin);
        if (price != null) {
            inventory.setPrice(price);
        }
        if (seats != null) {
            int sold = inventory.getTotalSeats() - inventory.getAvailableSeats();
            inventory.setTotalSeats(seats);
            inventory.setAvailableSeats(Math.max(0, seats - sold));
        }
        inventoryMapper.updateById(inventory);
    }

    private FlightVO toVO(Flight flight) {
        Map<String, FlightInventory> inventoryMap = inventoryMapper.selectList(new LambdaQueryWrapper<FlightInventory>()
                        .eq(FlightInventory::getFlightId, flight.getId()))
                .stream().collect(Collectors.toMap(FlightInventory::getCabinType, i -> i));
        FlightVO vo = new FlightVO();
        vo.setId(flight.getId());
        vo.setFlightNo(flight.getFlightNo());
        vo.setAirline(flight.getAirline());
        vo.setDepartureCity(flight.getDepartureCity());
        vo.setArrivalCity(flight.getArrivalCity());
        vo.setDepartureAirport(flight.getDepartureAirport());
        vo.setArrivalAirport(flight.getArrivalAirport());
        vo.setDepartureTime(flight.getDepartureTime());
        vo.setArrivalTime(flight.getArrivalTime());
        vo.setAircraftType(flight.getAircraftType());
        vo.setStatus(flight.getStatus());
        FlightInventory economy = inventoryMap.get("ECONOMY");
        FlightInventory business = inventoryMap.get("BUSINESS");
        if (economy != null) {
            vo.setEconomyPrice(economy.getPrice());
            vo.setEconomyAvailable(economy.getAvailableSeats());
            vo.setEconomyTotal(economy.getTotalSeats());
        }
        if (business != null) {
            vo.setBusinessPrice(business.getPrice());
            vo.setBusinessAvailable(business.getAvailableSeats());
            vo.setBusinessTotal(business.getTotalSeats());
        }
        return vo;
    }

    public record FlightSaveReq(
            String flightNo,
            String airline,
            String departureCity,
            String arrivalCity,
            String departureAirport,
            String arrivalAirport,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            String aircraftType,
            BigDecimal economyPrice,
            Integer economySeats,
            BigDecimal businessPrice,
            Integer businessSeats) {}
}
