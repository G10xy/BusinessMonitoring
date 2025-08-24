package it.bm.kafka;

import it.bm.model.kafka.UpsellingServiceDTO;
import it.bm.service.EmailService;
import it.bm.util.MDCUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.mail.MailException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static it.bm.util.Constant.CORRELATION_ID_HEADER_NAME;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpsellingServiceKafkaConsumer {

    @Value("${event.topic.email-upselling-service}")
    private String topicName;
    private final EmailService emailService;

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 60000, multiplier = 2.0),
            dltTopicSuffix = ".DLT",
            include = {MessagingException.class, MailException.class}
    )
    @KafkaListener(
            topics = "${event.topic.email-upselling-service}",
            batch = "false",
            concurrency = "1",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void upsellingServiceConsumer(
            @Payload UpsellingServiceDTO message,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(value = CORRELATION_ID_HEADER_NAME, required = false) byte[] correlationIdBytes
    ) throws MessagingException, MailException {
        try {
            if (correlationIdBytes != null) {
                String correlationId = new String(correlationIdBytes, StandardCharsets.UTF_8);
                MDCUtil.setCorrelationId(correlationId);
            }
            log.info("Received message: {} at offset: {}", message.customerId(), offset);
            emailService.sendEmail(message);
        } finally {
            MDCUtil.clearContext();
        }
    }


    @KafkaListener(
            topics = "${event.topic.email-upselling-service}" + ".DLT",
            groupId = "email-upselling-service-dlt-consumer-group",
            batch = "false",
            concurrency = "1"
    )
    public void upsellingServiceConsumerDLQ(
            @Payload UpsellingServiceDTO message,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(value = CORRELATION_ID_HEADER_NAME, required = false) byte[] correlationIdBytes) {
        try {
            if (correlationIdBytes != null) {
                String correlationId = new String(correlationIdBytes, StandardCharsets.UTF_8);
                MDCUtil.setCorrelationId(correlationId);
            }
            log.warn("ATTENTION: received message in " + topicName + ".DLT  at offset {} meaning it was impossible to send email to " +
                    "the direction about upselling opportunity for customer {}", offset, message.customerId());
        } finally {
            MDCUtil.clearContext();
        }
    }
}