package it.bm.service;

import it.bm.kafka.ExpiredServicesKafkaProducer;
import it.bm.kafka.UpsellingServiceKafkaProducer;
import it.bm.model.kafka.ExpiredServicesDTO;
import it.bm.model.kafka.UpsellingServiceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final UpsellingServiceKafkaProducer upsellingServiceKafkaProducer;
    private final ExpiredServicesKafkaProducer expiredServicesKafkaProducer;

    @Async("notificationExecutor")
    @Retryable(
            retryFor = {Exception.class},
            maxAttemptsExpression = "${retry.maxAttempts:4}",
            backoff = @Backoff(
                    delayExpression = "${retry.initialDelay:1000}",
                    multiplierExpression = "${retry.multiplier:2.0}",
                    maxDelayExpression = "${retry.maxDelay:10000}"
            )
    )
    public void sendExpiredServicesNotification(ExpiredServicesDTO message) {
        try {
            log.info("Sending expired services notification for customer: {}", message.customerId());
            expiredServicesKafkaProducer.sendMessage(message);
        } catch (Exception e) {
            log.error("Failed to send expired services notification for customer: {}", message.customerId(), e);
            throw e;
        }
    }

    @Async("notificationExecutor")
    @Retryable(
            retryFor = {Exception.class},
            maxAttemptsExpression = "${retry.maxAttempts:4}",
            backoff = @Backoff(
                    delayExpression = "${retry.initialDelay:1000}",
                    multiplierExpression = "${retry.multiplier:2.0}",
                    maxDelayExpression = "${retry.maxDelay:10000}"
            )
    )
    public void sendUpsellingNotification(UpsellingServiceDTO message) {
        try {
            log.info("Sending upselling notification for customer: {}", message.customerId());
            upsellingServiceKafkaProducer.sendMessage(message);
        } catch (Exception e) {
            log.error("Failed to send upselling notification for customer: {}", message.customerId(), e);
            throw e;
        }
    }

    @Recover
    public void recoverExpiredServicesNotification(Exception ex, ExpiredServicesDTO message) {
        log.error("FINAL FAILURE: Could not send expired services notification for customer {} after all retries. Reason: {}",
                message.customerId(), ex.getMessage());
    }

    @Recover
    public void recoverUpsellingNotification(Exception ex, UpsellingServiceDTO message) {
        log.error("FINAL FAILURE: Could not send upselling notification for customer {} after all retries. Reason: {}",
                message.customerId(), ex.getMessage());
    }
}