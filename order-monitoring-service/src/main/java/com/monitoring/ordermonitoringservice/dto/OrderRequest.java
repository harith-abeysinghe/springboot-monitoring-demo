package com.monitoring.ordermonitoringservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OrderRequest {
    @NotBlank
    private String item;

    @Positive
    private double amount;
}

