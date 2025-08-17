package it.bm.kafka;

import it.bm.model.kafka.UpsellingServiceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UpsellingServiceKafkaProducer {
    @Value("${event.topic.email-upselling-service}")
    private String topicUpsellingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UpsellingServiceKafkaProducer(@Qualifier("upsellingServiceKafkaTemplate")  KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(UpsellingServiceDTO message) {
        log.info("Sending message about upselling opportunity for customer {}", message.customerId());
        kafkaTemplate.send(topicUpsellingService, message);
    }
}
