package com.monitoring.ordermonitoringservice.service;

import com.monitoring.ordermonitoringservice.dto.OrderRequest;
import com.monitoring.ordermonitoringservice.model.Order;
import com.monitoring.ordermonitoringservice.model.OrderStatus;
import com.monitoring.ordermonitoringservice.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MeterRegistry meterRegistry;

    private Counter ordersReceivedCounter;
    private Counter ordersSuccessCounter;
    private Counter ordersFailedCounter;
    private Timer processingTimer;
    private final Random random = new Random();

    @PostConstruct
    public void initMetrics() {
        this.ordersReceivedCounter = meterRegistry.counter("orders_received");
        this.ordersSuccessCounter = meterRegistry.counter("orders_success");
        this.ordersFailedCounter = meterRegistry.counter("orders_failed");
        this.processingTimer = Timer.builder("order_processing_duration")
                .description("Order processing latency")
                .publishPercentiles(0.5, 0.95)
                .register(meterRegistry);
        log.info("OrderService metrics initialized: orders_received, orders_success, orders_failed, order_processing_duration");
    }

    @Transactional
    public Order processOrder(Order order) {
        log.info("Starting processing order id={} item={} amount={}", order.getId(), order.getItem(), order.getAmount());
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Simulated processing delay between 500ms and 2000ms
            long delayMs = 500 + random.nextInt(1501);
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Order processing interrupted for id={}", order.getId());
            }

            // 10% chance to fail
            boolean failed = random.nextInt(10) == 0;
            if (failed) {
                order.setStatus(OrderStatus.FAILED);
                ordersFailedCounter.increment();
                log.info("Order processing resulted in FAILED status for id={}", order.getId());
            } else {
                order.setStatus(OrderStatus.PROCESSED);
                ordersSuccessCounter.increment();
                log.info("Order processing resulted in PROCESSED status for id={}", order.getId());
            }

            Order saved = orderRepository.save(order);
            log.info("Order saved with id={}", saved.getId());
            return saved;
        } finally {
            sample.stop(processingTimer);
            log.info("Recorded metrics for order processing id={}; processing time recorded", order.getId());
        }
    }

    public Order submitOrder(OrderRequest request) {
        Order order = new Order();
        order.setItem(request.getItem());
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.RECEIVED);
        order.setCreatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        ordersReceivedCounter.increment();
        log.info("Order received and persisted with id={}", saved.getId());

        processOrderAsync(saved.getId());
        return saved;
    }

    @Async("orderProcessorExecutor")
    public void processOrderAsync(Long orderId) {
        try {
            Optional<Order> opt = orderRepository.findById(orderId);
            opt.ifPresent(this::processOrder);
        } catch (Exception ex) {
            log.error("Async processing failed for orderId={}", orderId, ex);
        }
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order updateOrder(Long id, OrderRequest request) {
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) {
            return null;
        }
        Order order = opt.get();
        order.setItem(request.getItem());
        order.setAmount(request.getAmount());
        Order saved = orderRepository.save(order);
        log.info("Order updated with id={}", saved.getId());
        return saved;
    }
}
