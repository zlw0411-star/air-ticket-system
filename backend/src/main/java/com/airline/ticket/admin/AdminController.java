package com.airline.ticket.admin;

import com.airline.ticket.common.ApiResponse;
import com.airline.ticket.common.PageResult;
import com.airline.ticket.flight.FlightService;
import com.airline.ticket.flight.FlightVO;
import com.airline.ticket.order.OrderService;
import com.airline.ticket.user.SysUser;
import com.airline.ticket.user.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService userService;
    private final FlightService flightService;
    private final OrderService orderService;

    public AdminController(UserService userService, FlightService flightService, OrderService orderService) {
        this.userService = userService;
        this.flightService = flightService;
        this.orderService = orderService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<SysUser>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(userService.adminList(keyword, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> userStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        userService.updateStatus(id, req.enabled());
        return ApiResponse.ok();
    }

    @GetMapping("/flights")
    public ApiResponse<PageResult<FlightVO>> flights(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(flightService.adminList(page, size));
    }

    @PostMapping("/flights")
    public ApiResponse<FlightVO> createFlight(@RequestBody FlightService.FlightSaveReq req) {
        return ApiResponse.ok(flightService.create(req));
    }

    @PutMapping("/flights/{id}")
    public ApiResponse<FlightVO> updateFlight(@PathVariable Long id, @RequestBody FlightService.FlightSaveReq req) {
        return ApiResponse.ok(flightService.update(id, req));
    }

    @PutMapping("/flights/{id}/status")
    public ApiResponse<Void> flightStatus(@PathVariable Long id, @RequestBody FlightStatusReq req) {
        flightService.updateStatus(id, req.status());
        return ApiResponse.ok();
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<OrderService.OrderDetailVO>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.adminOrders(status, page, size));
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        return ApiResponse.ok(orderService.statistics());
    }

    public record StatusReq(Boolean enabled) {}
    public record FlightStatusReq(String status) {}
}
