package it.bm;

import it.bm.kafka.ExpiredServicesKafkaProducer;
import it.bm.kafka.UpsellingServiceKafkaProducer;
import it.bm.model.kafka.ExpiredServicesDTO;
import it.bm.model.kafka.UpsellingServiceDTO;
import it.bm.service.NotificationService;
import it.bm.util.MDCUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static it.bm.util.Constant.CORRELATION_ID_HEADER_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UpsellingServiceKafkaProducer upsellingServiceKafkaProducer;

    @Mock
    private ExpiredServicesKafkaProducer expiredServicesKafkaProducer;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(upsellingServiceKafkaProducer, expiredServicesKafkaProducer);
    }

    @Test
    void sendExpiredServicesNotificationSuccessfully() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C001", 3);
        assertDoesNotThrow(() -> notificationService.sendExpiredServicesNotification(message));
        verify(expiredServicesKafkaProducer).sendMessage(message);
    }

    @Test
    void sendExpiredServicesNotificationWithValidMessage() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C001", 5);
        notificationService.sendExpiredServicesNotification(message);
        verify(expiredServicesKafkaProducer, times(1)).sendMessage(message);
        verify(expiredServicesKafkaProducer).sendMessage(argThat(msg ->
                "C001".equals(msg.customerId()) && msg.numberOfExpiredServices() == 5));
    }

    @Test
    void sendExpiredServicesNotificationThrowsExceptionPropagatesException() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C001", 3);
        RuntimeException kafkaException = new RuntimeException("Kafka producer failed");
        doThrow(kafkaException).when(expiredServicesKafkaProducer).sendMessage(message);

        RuntimeException thrownException = assertThrows(RuntimeException.class, () ->
                notificationService.sendExpiredServicesNotification(message));

        assertEquals("Kafka producer failed", thrownException.getMessage());
        verify(expiredServicesKafkaProducer).sendMessage(message);
    }

    @Test
    void sendExpiredServicesNotification_MultipleMessages() {
        ExpiredServicesDTO message1 = new ExpiredServicesDTO("C001", 2);
        ExpiredServicesDTO message2 = new ExpiredServicesDTO("C002", 4);

        notificationService.sendExpiredServicesNotification(message1);
        notificationService.sendExpiredServicesNotification(message2);

        verify(expiredServicesKafkaProducer).sendMessage(message1);
        verify(expiredServicesKafkaProducer).sendMessage(message2);
        verify(expiredServicesKafkaProducer, times(2)).sendMessage(any(ExpiredServicesDTO.class));
    }


    @Test
    void sendUpsellingNotification_WithValidMessage() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C009", "email");
        notificationService.sendUpsellingNotification(message);
        verify(upsellingServiceKafkaProducer, times(1)).sendMessage(message);
        verify(upsellingServiceKafkaProducer).sendMessage(argThat(msg ->
                "C009".equals(msg.customerId()) && "email".equals(msg.upsellingService())));
    }

    @Test
    void sendUpsellingNotification_ThrowsException_PropagatesException() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C009", "hosting");
        RuntimeException kafkaException = new RuntimeException("Kafka upselling producer failed");
        doThrow(kafkaException).when(upsellingServiceKafkaProducer).sendMessage(message);

        RuntimeException thrownException = assertThrows(RuntimeException.class, () ->
                notificationService.sendUpsellingNotification(message));

        assertEquals("Kafka upselling producer failed", thrownException.getMessage());
        verify(upsellingServiceKafkaProducer).sendMessage(message);
    }

    @Test
    void sendUpsellingNotification_MultipleMessages() {
        UpsellingServiceDTO message1 = new UpsellingServiceDTO("C009", "cloud");
        UpsellingServiceDTO message2 = new UpsellingServiceDTO("C008", "pec");

        notificationService.sendUpsellingNotification(message1);
        notificationService.sendUpsellingNotification(message2);

        verify(upsellingServiceKafkaProducer).sendMessage(message1);
        verify(upsellingServiceKafkaProducer).sendMessage(message2);
        verify(upsellingServiceKafkaProducer, times(2)).sendMessage(any(UpsellingServiceDTO.class));
    }

    @Test
    void recoverExpiredServicesNotification_DoesNotThrowException() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C009", 5);
        Exception exception = new RuntimeException("Final failure after retries");

        assertDoesNotThrow(() -> notificationService.recoverExpiredServicesNotification(exception, message));

        verifyNoInteractions(expiredServicesKafkaProducer);
        verifyNoInteractions(upsellingServiceKafkaProducer);
    }

    @Test
    void recoverUpsellingNotification_DoesNotThrowException() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C009", "email");
        Exception exception = new RuntimeException("Final failure after retries");

        assertDoesNotThrow(() -> notificationService.recoverUpsellingNotification(exception, message));

        verifyNoInteractions(expiredServicesKafkaProducer);
        verifyNoInteractions(upsellingServiceKafkaProducer);
    }


    @Test
    void bothNotificationTypes_IndependentExecution() {
        ExpiredServicesDTO expiredMessage = new ExpiredServicesDTO("C001", 2);
        UpsellingServiceDTO upsellingMessage = new UpsellingServiceDTO("C009", "hosting");

        notificationService.sendExpiredServicesNotification(expiredMessage);
        notificationService.sendUpsellingNotification(upsellingMessage);

        verify(expiredServicesKafkaProducer).sendMessage(expiredMessage);
        verify(upsellingServiceKafkaProducer).sendMessage(upsellingMessage);
    }

    @Test
    void recoverMethodsHandle_DifferentExceptionTypes() {
        ExpiredServicesDTO expiredMessage = new ExpiredServicesDTO("C001", 5);
        UpsellingServiceDTO upsellingMessage = new UpsellingServiceDTO("C009", "pec");

        IllegalStateException illegalStateException = new IllegalStateException("Invalid state");
        NullPointerException nullPointerException = new NullPointerException("Null reference");

        assertDoesNotThrow(() -> notificationService.recoverExpiredServicesNotification(illegalStateException, expiredMessage));
        assertDoesNotThrow(() -> notificationService.recoverUpsellingNotification(nullPointerException, upsellingMessage));
    }

    @Test
    void sendExpiredServicesNotificationMDCContextHandling() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C001", 2);
        Map<String, String> originalContext = new HashMap<>();
        originalContext.put(CORRELATION_ID_HEADER_NAME, "test-correlation-id");

        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(MDCUtil::getCopyOfContextMap).thenReturn(originalContext);

            notificationService.sendExpiredServicesNotification(message);

            mdcUtilMock.verify(MDCUtil::getCopyOfContextMap);
            mdcUtilMock.verify(() -> MDCUtil.setContextMap(originalContext));
            mdcUtilMock.verify(MDCUtil::clearContext);
            verify(expiredServicesKafkaProducer).sendMessage(message);
        }
    }

    @Test
    void sendUpsellingNotification_MDCContextHandling() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C009", "cloud");
        Map<String, String> originalContext = new HashMap<>();
        originalContext.put(CORRELATION_ID_HEADER_NAME, "test-correlation-id");

        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(MDCUtil::getCopyOfContextMap).thenReturn(originalContext);

            notificationService.sendUpsellingNotification(message);

            mdcUtilMock.verify(MDCUtil::getCopyOfContextMap);
            mdcUtilMock.verify(() -> MDCUtil.setContextMap(originalContext));
            mdcUtilMock.verify(MDCUtil::clearContext);
            verify(upsellingServiceKafkaProducer).sendMessage(message);
        }
    }

    @Test
    void sendExpiredServicesNotification_ClearsContextEvenWhenExceptionThrown() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C001", 8);
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put(CORRELATION_ID_HEADER_NAME, "test-correlation-id");

        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(MDCUtil::getCopyOfContextMap).thenReturn(contextMap);
            doThrow(new RuntimeException("Producer error")).when(expiredServicesKafkaProducer).sendMessage(message);

            assertThrows(RuntimeException.class, () ->
                    notificationService.sendExpiredServicesNotification(message));

            mdcUtilMock.verify(() -> MDCUtil.setContextMap(contextMap));
            mdcUtilMock.verify(MDCUtil::clearContext);
        }
    }

    @Test
    void sendUpsellingNotification_ClearsContextEvenWhenExceptionThrown() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C009", "spid");
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put(CORRELATION_ID_HEADER_NAME, "test-correlation-id");

        try (MockedStatic<MDCUtil> mdcUtilMock = mockStatic(MDCUtil.class)) {
            mdcUtilMock.when(MDCUtil::getCopyOfContextMap).thenReturn(contextMap);
            doThrow(new RuntimeException("Producer error")).when(upsellingServiceKafkaProducer).sendMessage(message);

            assertThrows(RuntimeException.class, () ->
                    notificationService.sendUpsellingNotification(message));

            mdcUtilMock.verify(() -> MDCUtil.setContextMap(contextMap));
            mdcUtilMock.verify(MDCUtil::clearContext);
        }
    }
}
