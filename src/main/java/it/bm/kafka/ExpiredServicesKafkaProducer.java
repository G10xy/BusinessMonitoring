package it.bm.kafka;

import it.bm.model.kafka.ExpiredServicesDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class ExpiredServicesKafkaProducer {
    @Value("${event.topic.expired-services}")
    private String topicExpiredServices;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ExpiredServicesKafkaProducer(@Qualifier("expiredServicesKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(ExpiredServicesDTO message) {
        log.info("Sending message about expired services for customer {}", message.customerId());
        kafkaTemplate.send(topicExpiredServices, message);
    }
}
