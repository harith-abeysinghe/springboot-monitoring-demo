package com.monitoring.ordermonitoringservice.repository;

import com.monitoring.ordermonitoringservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
