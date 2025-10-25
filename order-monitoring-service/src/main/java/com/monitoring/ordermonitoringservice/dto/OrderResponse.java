package com.monitoring.ordermonitoringservice.dto;

import com.monitoring.ordermonitoringservice.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String item;
    private double amount;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
