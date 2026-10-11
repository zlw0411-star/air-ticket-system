package com.airline.ticket.order;

import com.airline.ticket.common.BusinessException;
import com.airline.ticket.common.PageResult;
import com.airline.ticket.flight.Flight;
import com.airline.ticket.flight.FlightInventory;
import com.airline.ticket.flight.FlightService;
import com.airline.ticket.flight.FlightVO;
import com.airline.ticket.security.AuthUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class OrderService {
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[0-9]{17}[0-9Xx]$");
    private final OrderMapper orderMapper;
    private final TicketMapper ticketMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final RescheduleRecordMapper rescheduleRecordMapper;
    private final FlightService flightService;
    private final int payTimeoutMinutes;

    public OrderService(OrderMapper orderMapper,
                        TicketMapper ticketMapper,
                        RefundRecordMapper refundRecordMapper,
                        RescheduleRecordMapper rescheduleRecordMapper,
                        FlightService flightService,
                        @Value("${app.order.pay-timeout-minutes}") int payTimeoutMinutes) {
        this.orderMapper = orderMapper;
        this.ticketMapper = ticketMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.rescheduleRecordMapper = rescheduleRecordMapper;
        this.flightService = flightService;
        this.payTimeoutMinutes = payTimeoutMinutes;
    }

    @Transactional
    public OrderDetailVO create(CreateOrderReq req) {
        if (req.passengers() == null || req.passengers().isEmpty()) {
            throw new BusinessException("至少填写一名乘机人");
        }
        if (req.passengers().size() > 5) {
            throw new BusinessException("单次最多购买 5 张机票");
        }
        for (PassengerReq passenger : req.passengers()) {
            if (passenger == null || passenger.name() == null || passenger.name().trim().isEmpty()) {
                throw new BusinessException("乘机人姓名不能为空");
            }
            if (passenger.idCard() == null || !ID_CARD_PATTERN.matcher(passenger.idCard()).matches()) {
                throw new BusinessException("乘机人身份证号必须为18位有效格式");
            }
        }
        Flight flight = flightService.requireFlight(req.flightId());
        if (!"SCHEDULED".equals(flight.getStatus())) {
            throw new BusinessException("该航班不可购票");
        }
        if (flight.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("航班已起飞，无法购票");
        }
        String cabin = normalizeCabin(req.cabinType());
        FlightInventory inventory = flightService.requireInventory(req.flightId(), cabin);
        flightService.lockSeats(req.flightId(), cabin, req.passengers().size());

        AuthUser authUser = currentUser();
        BigDecimal total = inventory.getPrice().multiply(BigDecimal.valueOf(req.passengers().size()));
        TicketOrder order = new TicketOrder();
        order.setOrderNo(nextNo("AT"));
        order.setUserId(authUser.getId());
        order.setTotalAmount(total);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setStatus("PENDING_PAYMENT");
        order.setPayDeadline(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        orderMapper.insert(order);

        int seat = 1;
        for (PassengerReq passenger : req.passengers()) {
            Ticket ticket = new Ticket();
            ticket.setTicketNo(nextNo("TN"));
            ticket.setOrderId(order.getId());
            ticket.setUserId(authUser.getId());
            ticket.setFlightId(flight.getId());
            ticket.setPassengerName(passenger.name());
            ticket.setPassengerIdNo(passenger.idCard());
            ticket.setPassengerPhone(passenger.phone());
            ticket.setCabinType(cabin);
            ticket.setSeatNo(cabin.charAt(0) + String.format("%02d", seat++));
            ticket.setPrice(inventory.getPrice());
            ticket.setStatus("PENDING");
            ticketMapper.insert(ticket);
        }
        return detail(order.getId());
    }

    @Transactional
    public OrderDetailVO pay(Long orderId) {
        TicketOrder order = requireOwnOrder(orderId);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException("当前订单不可支付");
        }
        if (order.getPayDeadline() != null && order.getPayDeadline().isBefore(LocalDateTime.now())) {
            cancelInternal(order, "支付超时自动取消");
            throw new BusinessException("订单已超时，请重新下单");
        }
        order.setStatus("PAID");
        order.setPaidAmount(order.getTotalAmount());
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);
        List<Ticket> tickets = ticketsOf(orderId);
        for (Ticket ticket : tickets) {
            ticket.setStatus("ISSUED");
            ticketMapper.updateById(ticket);
        }
        return detail(orderId);
    }

    @Transactional
    public OrderDetailVO cancel(Long orderId) {
        TicketOrder order = requireOwnOrder(orderId);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException("仅待支付订单可取消");
        }
        cancelInternal(order, "用户取消");
        return detail(orderId);
    }

    @Transactional
    public OrderDetailVO refund(Long ticketId) {
        Ticket ticket = requireOwnTicket(ticketId);
        if (!"ISSUED".equals(ticket.getStatus()) && !"RESCHEDULED".equals(ticket.getStatus())) {
            throw new BusinessException("当前机票不可退票");
        }
        Flight flight = flightService.requireFlight(ticket.getFlightId());
        if (flight.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("航班已起飞，无法退票");
        }
        BigDecimal refundAmount = calcRefund(ticket.getPrice(), flight.getDepartureTime());
        ticket.setStatus("REFUNDED");
        ticketMapper.updateById(ticket);
        flightService.releaseSeats(ticket.getFlightId(), ticket.getCabinType(), 1);

        RefundRecord record = new RefundRecord();
        record.setTicketId(ticket.getId());
        record.setOrderId(ticket.getOrderId());
        record.setRefundAmount(refundAmount);
        record.setReason("乘客申请退票");
        refundRecordMapper.insert(record);

        TicketOrder order = orderMapper.selectById(ticket.getOrderId());
        order.setPaidAmount(order.getPaidAmount().subtract(refundAmount).max(BigDecimal.ZERO));
        orderMapper.updateById(order);
        refreshOrderAfterTicketChange(ticket.getOrderId());
        return detail(ticket.getOrderId());
    }

    @Transactional
    public OrderDetailVO reschedule(Long ticketId, Long targetFlightId, String cabinType) {
        Ticket ticket = requireOwnTicket(ticketId);
        if (!"ISSUED".equals(ticket.getStatus()) && !"RESCHEDULED".equals(ticket.getStatus())) {
            throw new BusinessException("当前机票不可改签");
        }
        Flight oldFlight = flightService.requireFlight(ticket.getFlightId());
        if (Duration.between(LocalDateTime.now(), oldFlight.getDepartureTime()).toMinutes() < 120) {
            throw new BusinessException("起飞前 2 小时内不可改签");
        }
        Flight newFlight = flightService.requireFlight(targetFlightId);
        if (!"SCHEDULED".equals(newFlight.getStatus())) {
            throw new BusinessException("目标航班不可改签");
        }
        if (newFlight.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("目标航班已起飞");
        }
        String newCabin = normalizeCabin(cabinType);
        FlightInventory newInventory = flightService.requireInventory(targetFlightId, newCabin);
        flightService.lockSeats(targetFlightId, newCabin, 1);
        flightService.releaseSeats(ticket.getFlightId(), ticket.getCabinType(), 1);

        BigDecimal diff = newInventory.getPrice().subtract(ticket.getPrice());
        RescheduleRecord record = new RescheduleRecord();
        record.setTicketId(ticket.getId());
        record.setFromFlightId(ticket.getFlightId());
        record.setToFlightId(targetFlightId);
        record.setFromCabin(ticket.getCabinType());
        record.setToCabin(newCabin);
        record.setPriceDiff(diff);
        rescheduleRecordMapper.insert(record);

        if (ticket.getOriginalFlightId() == null) {
            ticket.setOriginalFlightId(ticket.getFlightId());
        }
        ticket.setFlightId(targetFlightId);
        ticket.setCabinType(newCabin);
        ticket.setPrice(newInventory.getPrice());
        ticket.setStatus("RESCHEDULED");
        ticketMapper.updateById(ticket);

        TicketOrder order = orderMapper.selectById(ticket.getOrderId());
        List<Ticket> tickets = ticketsOf(order.getId());
        BigDecimal total = tickets.stream().map(Ticket::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            order.setPaidAmount(order.getPaidAmount().add(diff));
        } else {
            order.setPaidAmount(order.getPaidAmount().add(diff).max(BigDecimal.ZERO));
        }
        orderMapper.updateById(order);
        return detail(order.getId());
    }

    public PageResult<OrderDetailVO> myOrders(int page, int size) {
        AuthUser authUser = currentUser();
        Page<TicketOrder> result = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getUserId, authUser.getId())
                        .orderByDesc(TicketOrder::getCreatedAt));
        List<OrderDetailVO> records = result.getRecords().stream().map(o -> detail(o.getId())).toList();
        return new PageResult<>(result.getTotal(), records);
    }

    public OrderDetailVO detail(Long orderId) {
        TicketOrder order = requireOwnOrAdmin(orderId);
        List<Ticket> tickets = ticketsOf(orderId);
        List<TicketVO> ticketVOS = tickets.stream().map(this::toTicketVO).toList();
        return new OrderDetailVO(order, ticketVOS);
    }

    public PageResult<OrderDetailVO> adminOrders(String status, int page, int size) {
        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(TicketOrder::getStatus, status);
        }
        wrapper.orderByDesc(TicketOrder::getCreatedAt);
        Page<TicketOrder> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        List<OrderDetailVO> records = result.getRecords().stream()
                .map(o -> new OrderDetailVO(o, ticketsOf(o.getId()).stream().map(this::toTicketVO).toList()))
                .toList();
        return new PageResult<>(result.getTotal(), records);
    }

    public Map<String, Object> statistics() {
        List<TicketOrder> paid = orderMapper.selectList(new LambdaQueryWrapper<TicketOrder>()
                .in(TicketOrder::getStatus, List.of("PAID", "REFUNDED")));
        BigDecimal revenue = paid.stream().map(TicketOrder::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long todayCount = paid.stream().filter(o -> o.getPaidAt() != null && o.getPaidAt().toLocalDate().equals(LocalDateTime.now().toLocalDate())).count();
        BigDecimal todayRevenue = paid.stream()
                .filter(o -> o.getPaidAt() != null && o.getPaidAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .map(TicketOrder::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Ticket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>().in(Ticket::getStatus, List.of("ISSUED", "RESCHEDULED")));
        Map<Long, Long> routeCount = tickets.stream().collect(Collectors.groupingBy(Ticket::getFlightId, Collectors.counting()));
        List<Map<String, Object>> hotRoutes = new ArrayList<>();
        routeCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    FlightVO flight = flightService.detail(e.getKey());
                    hotRoutes.add(Map.of(
                            "flightNo", flight.getFlightNo(),
                            "route", flight.getDepartureCity() + " → " + flight.getArrivalCity(),
                            "count", e.getValue()));
                });
        return Map.of(
                "orderCount", paid.size(),
                "revenue", revenue,
                "todayOrderCount", todayCount,
                "todayRevenue", todayRevenue,
                "hotRoutes", hotRoutes
        );
    }

    @Transactional
    public void cancelExpiredOrders() {
        List<TicketOrder> expired = orderMapper.selectList(new LambdaQueryWrapper<TicketOrder>()
                .eq(TicketOrder::getStatus, "PENDING_PAYMENT")
                .lt(TicketOrder::getPayDeadline, LocalDateTime.now()));
        for (TicketOrder order : expired) {
            cancelInternal(order, "支付超时自动取消");
        }
    }

    private void cancelInternal(TicketOrder order, String reason) {
        List<Ticket> tickets = ticketsOf(order.getId());
        for (Ticket ticket : tickets) {
            if ("PENDING".equals(ticket.getStatus())) {
                flightService.releaseSeats(ticket.getFlightId(), ticket.getCabinType(), 1);
                ticket.setStatus("CANCELLED");
                ticketMapper.updateById(ticket);
            }
        }
        order.setStatus("CANCELLED");
        orderMapper.updateById(order);
    }

    private void refreshOrderAfterTicketChange(Long orderId) {
        List<Ticket> tickets = ticketsOf(orderId);
        boolean allRefunded = tickets.stream().allMatch(t -> "REFUNDED".equals(t.getStatus()) || "CANCELLED".equals(t.getStatus()));
        TicketOrder order = orderMapper.selectById(orderId);
        if (allRefunded) {
            order.setStatus("REFUNDED");
            orderMapper.updateById(order);
        }
    }

    private BigDecimal calcRefund(BigDecimal price, LocalDateTime departureTime) {
        long hours = Duration.between(LocalDateTime.now(), departureTime).toHours();
        if (hours >= 24) {
            return price;
        }
        if (hours >= 2) {
            return price.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
        }
        return price.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
    }

    private TicketOrder requireOwnOrder(Long orderId) {
        TicketOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        AuthUser user = currentUser();
        if (!order.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "不能操作他人订单");
        }
        return order;
    }

    private TicketOrder requireOwnOrAdmin(Long orderId) {
        return requireOwnOrder(orderId);
    }

    private Ticket requireOwnTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException("机票不存在");
        }
        AuthUser user = currentUser();
        if (!ticket.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "不能操作他人机票");
        }
        return ticket;
    }

    private List<Ticket> ticketsOf(Long orderId) {
        return ticketMapper.selectList(new LambdaQueryWrapper<Ticket>().eq(Ticket::getOrderId, orderId));
    }

    private TicketVO toTicketVO(Ticket ticket) {
        FlightVO flight = flightService.detail(ticket.getFlightId());
        return new TicketVO(ticket, flight);
    }

    private AuthUser currentUser() {
        return (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String normalizeCabin(String cabinType) {
        if ("BUSINESS".equalsIgnoreCase(cabinType)) {
            return "BUSINESS";
        }
        return "ECONOMY";
    }

    private String nextNo(String prefix) {
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    public record PassengerReq(String name, String idCard, String phone) {}
    public record CreateOrderReq(Long flightId, String cabinType, List<PassengerReq> passengers) {}
    public record TicketVO(Ticket ticket, FlightVO flight) {}
    public record OrderDetailVO(TicketOrder order, List<TicketVO> tickets) {}
}
