package com.airline.ticket.flight;

import com.airline.ticket.common.ApiResponse;
import com.airline.ticket.common.PageResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {
    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public ApiResponse<PageResult<FlightVO>> search(
            @RequestParam(required = false) String departureCity,
            @RequestParam(required = false) String arrivalCity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(flightService.search(departureCity, arrivalCity, date, page, size));
    }

    @GetMapping("/cities")
    public ApiResponse<List<String>> cities() {
        return ApiResponse.ok(flightService.cities());
    }

    @GetMapping("/{id}")
    public ApiResponse<FlightVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(flightService.detail(id));
    }
}
