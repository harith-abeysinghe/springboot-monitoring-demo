package com.monitoring.ordermonitoringservice.controller;

import com.monitoring.ordermonitoringservice.dto.OrderRequest;
import com.monitoring.ordermonitoringservice.dto.OrderResponse;
import com.monitoring.ordermonitoringservice.model.Order;
import com.monitoring.ordermonitoringservice.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final MeterRegistry meterRegistry;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received create order request: item={}, amount={}", request.getItem(), request.getAmount());
        Order saved = orderService.submitOrder(request);
        OrderResponse resp = toResponse(saved);
        log.info("Returning received order id={} status={}", resp.getId(), resp.getStatus());
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable("id") Long id, @Valid @RequestBody OrderRequest request) {
        log.info("Received update for order id={}: item={}, amount={}", id, request.getItem(), request.getAmount());
        Order updated = orderService.updateOrder(id, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        OrderResponse resp = toResponse(updated);
        log.info("Returning updated order id={} createdAt={}", resp.getId(), resp.getCreatedAt());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Counter counter = meterRegistry.find("orders_received").counter();
        long count = counter != null ? (long) counter.count() : 0L;
        Map<String, Object> resp = new HashMap<>();
        resp.put("totalRequests", count);
        resp.put("timestamp", Instant.now().toString());
        log.info("Stats requested: totalRequests={}", count);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable("id") Long id) {
        return orderService.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getItem(), o.getAmount(), o.getStatus(), o.getCreatedAt());
    }
}
