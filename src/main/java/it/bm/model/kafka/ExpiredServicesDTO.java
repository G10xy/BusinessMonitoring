package it.bm.model.kafka;

public record ExpiredServicesDTO(String customerId, long numberOfExpiredServices) {
}
