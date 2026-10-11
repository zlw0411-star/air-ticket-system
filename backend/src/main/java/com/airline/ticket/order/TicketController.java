package com.airline.ticket.order;

import com.airline.ticket.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final OrderService orderService;

    public TicketController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{id}/refund")
    public ApiResponse<OrderService.OrderDetailVO> refund(@PathVariable Long id) {
        return ApiResponse.ok(orderService.refund(id));
    }

    @PostMapping("/{id}/reschedule")
    public ApiResponse<OrderService.OrderDetailVO> reschedule(@PathVariable Long id, @RequestBody RescheduleReq req) {
        return ApiResponse.ok(orderService.reschedule(id, req.targetFlightId(), req.cabinType()));
    }

    public record RescheduleReq(Long targetFlightId, String cabinType) {}
}
