package com.airline.ticket.flight;

import com.airline.ticket.common.BusinessException;
import org.apache.ibatis.annotations.Select;
import org.mockito.InOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.*;

class FlightServiceInventoryTest {
    private FlightMapper flights;
    private FlightInventoryMapper inventoryMapper;
    private FlightService service;
    private Flight flight;

    @BeforeEach
    void setUp() {
        flights = mock(FlightMapper.class);
        inventoryMapper = mock(FlightInventoryMapper.class);
        service = new FlightService(flights, inventoryMapper);
        flight = new Flight();
        flight.setId(7L);
        when(flights.selectById(7L)).thenReturn(flight);
        when(inventoryMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void rejectsCapacityBelowOccupiedSeatsAndAllowsExactBoundary() {
        FlightInventory economy = inventory("ECONOMY", 100, 20);
        when(inventoryMapper.selectForUpdate(7L, "ECONOMY")).thenReturn(economy);

        assertThrows(BusinessException.class, () -> service.update(7L, request(50, null, null, null)));
        verify(inventoryMapper, never()).updateById(economy);

        service.update(7L, request(80, null, null, null));
        assertEquals(80, economy.getTotalSeats());
        assertEquals(0, economy.getAvailableSeats());
    }

    @Test
    void capacityIncreasePreservesOccupiedSeatsAndPriceOnlyEditUsesLockedCurrentRow() {
        FlightInventory economy = inventory("ECONOMY", 100, 20);
        when(inventoryMapper.selectForUpdate(7L, "ECONOMY")).thenReturn(economy);

        service.update(7L, request(150, null, new BigDecimal("250.00"), null));
        assertEquals(150, economy.getTotalSeats());
        assertEquals(70, economy.getAvailableSeats());

        // The value returned by the locking read reflects a booking that completed
        // before this update acquired the row lock; price edits preserve that value.
        economy.setAvailableSeats(69);
        service.update(7L, request(null, null, new BigDecimal("275.00"), null));
        assertEquals(69, economy.getAvailableSeats());
        assertEquals(new BigDecimal("275.00"), economy.getPrice());
        verify(inventoryMapper, times(2)).selectForUpdate(7L, "ECONOMY");
    }

    @Test
    void changedCabinsAreLockedAndWrittenBeforeTheFlightRow() {
        FlightInventory economy = inventory("ECONOMY", 100, 20);
        FlightInventory business = inventory("BUSINESS", 100, 30);
        when(inventoryMapper.selectForUpdate(7L, "ECONOMY")).thenReturn(economy);
        when(inventoryMapper.selectForUpdate(7L, "BUSINESS")).thenReturn(business);

        service.update(7L, request(90, 95, null, null));

        InOrder writes = inOrder(inventoryMapper, flights);
        writes.verify(inventoryMapper).selectForUpdate(7L, "ECONOMY");
        writes.verify(inventoryMapper).selectForUpdate(7L, "BUSINESS");
        writes.verify(inventoryMapper).updateById(economy);
        writes.verify(inventoryMapper).updateById(business);
        writes.verify(flights).updateById(flight);
    }

    @Test
    void secondCabinFailureUsesTransactionAndInventoryReadUsesForUpdate() throws Exception {
        when(inventoryMapper.selectForUpdate(7L, "ECONOMY")).thenReturn(inventory("ECONOMY", 100, 20));
        when(inventoryMapper.selectForUpdate(7L, "BUSINESS")).thenReturn(inventory("BUSINESS", 100, 10));

        assertThrows(BusinessException.class, () -> service.update(7L, request(80, 80, null, null)));
        Method update = FlightService.class.getMethod("update", Long.class, FlightService.FlightSaveReq.class);
        assertNotNull(update.getAnnotation(Transactional.class));
        String sql = FlightInventoryMapper.class.getMethod("selectForUpdate", Long.class, String.class)
                .getAnnotation(Select.class).value()[0];
        assertTrue(sql.toUpperCase().contains("FOR UPDATE"));
        verify(inventoryMapper).selectForUpdate(7L, "ECONOMY");
        verify(inventoryMapper).selectForUpdate(7L, "BUSINESS");
        verify(inventoryMapper, never()).updateById(any(FlightInventory.class));
        verify(flights, never()).updateById(any(Flight.class));
    }

    private static FlightInventory inventory(String cabin, int total, int available) {
        FlightInventory inventory = new FlightInventory();
        inventory.setFlightId(7L);
        inventory.setCabinType(cabin);
        inventory.setTotalSeats(total);
        inventory.setAvailableSeats(available);
        inventory.setPrice(new BigDecimal("200.00"));
        return inventory;
    }

    private static FlightService.FlightSaveReq request(Integer economySeats, Integer businessSeats,
                                                        BigDecimal economyPrice, BigDecimal businessPrice) {
        return new FlightService.FlightSaveReq(null, null, null, null, null, null,
                null, null, null, economyPrice, economySeats, businessPrice, businessSeats);
    }
}
