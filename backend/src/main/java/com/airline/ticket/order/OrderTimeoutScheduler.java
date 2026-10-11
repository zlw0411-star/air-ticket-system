package com.airline.ticket.order;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduler {
    private final OrderService orderService;

    public OrderTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60000)
    public void cancelExpired() {
        orderService.cancelExpiredOrders();
    }
}
