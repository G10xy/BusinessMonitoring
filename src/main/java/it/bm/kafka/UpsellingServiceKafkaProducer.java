package it.bm.kafka;

import it.bm.model.kafka.UpsellingServiceDTO;
import it.bm.util.MDCUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static it.bm.util.Constant.CORRELATION_ID_HEADER_NAME;

@Component
public class UpsellingServiceKafkaProducer {
    @Value("${event.topic.email-upselling-service}")
    private String topicUpsellingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UpsellingServiceKafkaProducer(@Qualifier("upsellingServiceKafkaTemplate")  KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(UpsellingServiceDTO message) {
        String correlationId = MDCUtil.getCorrelationId();

        ProducerRecord<String, Object> record = new ProducerRecord<>(topicUpsellingService, message);
        if (correlationId != null) {
            record.headers().add(new RecordHeader(CORRELATION_ID_HEADER_NAME, correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record);
    }
}
