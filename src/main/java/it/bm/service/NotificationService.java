package it.bm.service;

import it.bm.kafka.ExpiredServicesKafkaProducer;
import it.bm.kafka.UpsellingServiceKafkaProducer;
import it.bm.model.kafka.ExpiredServicesDTO;
import it.bm.model.kafka.UpsellingServiceDTO;
import it.bm.util.MDCUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final UpsellingServiceKafkaProducer upsellingServiceKafkaProducer;
    private final ExpiredServicesKafkaProducer expiredServicesKafkaProducer;

    @Async("notificationExecutor")
    @Retryable(
            retryFor = {Exception.class},
            maxAttemptsExpression = "${retry.maxAttempts}",
            backoff = @Backoff(
                    delayExpression = "${retry.initialDelay}",
                    multiplierExpression = "${retry.multiplier}",
                    maxDelayExpression = "${retry.maxDelay}"
            )
    )
    public void sendExpiredServicesNotification(ExpiredServicesDTO message) {
        Map<String, String> contextMap = MDCUtil.getCopyOfContextMap();
        try {
            MDCUtil.setContextMap(contextMap);
            log.info("Sending expired services notification for customer: {}", message.customerId());
            expiredServicesKafkaProducer.sendMessage(message);
        } catch (Exception e) {
            log.error("Failed to send expired services notification for customer: {}", message.customerId(), e);
            throw e;
        } finally {
            MDCUtil.clearContext();
        }
    }

    @Async("notificationExecutor")
    @Retryable(
            retryFor = {Exception.class},
            maxAttemptsExpression = "${retry.maxAttempts}",
            backoff = @Backoff(
                    delayExpression = "${retry.initialDelay}",
                    multiplierExpression = "${retry.multiplier}",
                    maxDelayExpression = "${retry.maxDelay}"
            )
    )
    public void sendUpsellingNotification(UpsellingServiceDTO message) {
        Map<String, String> contextMap = MDCUtil.getCopyOfContextMap();
        try {
            MDCUtil.setContextMap(contextMap);
            log.info("Sending upselling notification for customer: {}", message.customerId());
            upsellingServiceKafkaProducer.sendMessage(message);
        } catch (Exception e) {
            log.error("Failed to send upselling notification for customer: {}", message.customerId(), e);
            throw e;
        } finally {
            MDCUtil.clearContext();
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