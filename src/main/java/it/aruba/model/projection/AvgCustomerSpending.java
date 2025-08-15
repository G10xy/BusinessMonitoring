package it.aruba.model.projection;

import java.math.BigDecimal;

public record AvgCustomerSpending(String customerId, BigDecimal avgAmount) {
    public AvgCustomerSpending(String customerId, Double avgAmount) {
        this(customerId, BigDecimal.valueOf(avgAmount));
    }
}
