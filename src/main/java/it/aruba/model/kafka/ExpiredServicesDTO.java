package it.aruba.model.kafka;

public record ExpiredServicesDTO(String customerId, long numberOfExpiredServices) {
}
