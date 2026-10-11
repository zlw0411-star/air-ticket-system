package com.airline.ticket.order;

import com.airline.ticket.common.ApiResponse;
import com.airline.ticket.common.PageResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderService.OrderDetailVO> create(@RequestBody OrderService.CreateOrderReq req) {
        return ApiResponse.ok(orderService.create(req));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderService.OrderDetailVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.myOrders(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderService.OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderService.OrderDetailVO> pay(@PathVariable Long id) {
        return ApiResponse.ok(orderService.pay(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderService.OrderDetailVO> cancel(@PathVariable Long id) {
        return ApiResponse.ok(orderService.cancel(id));
    }
}
