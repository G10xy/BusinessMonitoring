package it.bm.kafka;

import it.bm.model.kafka.ExpiredServicesDTO;
import it.bm.util.MDCUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static it.bm.util.Constant.CORRELATION_ID_HEADER_NAME;


@Component
public class ExpiredServicesKafkaProducer {
    @Value("${event.topic.expired-services}")
    private String topicExpiredServices;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ExpiredServicesKafkaProducer(@Qualifier("expiredServicesKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(ExpiredServicesDTO message) {
        String correlationId = MDCUtil.getCorrelationId();

        ProducerRecord<String, Object> record = new ProducerRecord<>(topicExpiredServices, message);
        if (correlationId != null) {
            record.headers().add(new RecordHeader(CORRELATION_ID_HEADER_NAME, correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record);
    }
}