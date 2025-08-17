package it.bm.kafka;

import it.bm.model.kafka.UpsellingServiceDTO;
import it.bm.service.EmailService;
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
            @Header(KafkaHeaders.OFFSET) Long offset
    ) throws MessagingException, MailException {
       log.info("Received message: {}", message.customerId());
       emailService.sendEmail(message);
    }


    @KafkaListener(
            topics = "${event.topic.email-upselling-service}" + ".DLT",
            groupId = "email-upselling-service-dlt-consumer-group",
            batch = "false",
            concurrency = "1"
    )
    public void upsellingServiceConsumerDLQ(@Payload UpsellingServiceDTO message) {
            log.warn("ATTENTION: received message in " + topicName + ".DLT meaning it was impossible to send email to " +
                    "the direction about upselling opportunity for customer {}", message.customerId());
        }


}
