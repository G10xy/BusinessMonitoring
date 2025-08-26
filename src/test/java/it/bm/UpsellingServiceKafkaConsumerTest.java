package it.bm;

import it.bm.kafka.UpsellingServiceKafkaConsumer;
import it.bm.model.kafka.UpsellingServiceDTO;
import it.bm.service.EmailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpsellingServiceKafkaConsumerTest {

    @Mock
    private EmailService emailService;

    private UpsellingServiceKafkaConsumer upsellingServiceKafkaConsumer;

    private static final String TEST_TOPIC = "test-email-upselling-service";
    private static final Long TEST_OFFSET = 123L;

    @BeforeEach
    void setUp() {
        upsellingServiceKafkaConsumer = new UpsellingServiceKafkaConsumer(emailService);
        ReflectionTestUtils.setField(upsellingServiceKafkaConsumer, "topicName", TEST_TOPIC);
    }

    @Test
    void upsellingServiceConsumer_WithValidMessage_ProcessesSuccessfully() throws MessagingException, MailException {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C001", "hosting");
        byte[] correlationIdBytes = "test-correlation-123".getBytes(StandardCharsets.UTF_8);

        assertDoesNotThrow(() ->
                upsellingServiceKafkaConsumer.upsellingServiceConsumer(message, TEST_OFFSET, correlationIdBytes));

        verify(emailService).sendEmail(message);
        verify(emailService, times(1)).sendEmail(any(UpsellingServiceDTO.class));
    }

    @Test
    void upsellingServiceConsumer_WithNullCorrelationId_ProcessesSuccessfully() throws MessagingException, MailException {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C002", "email");
        byte[] nullCorrelationId = null;

        assertDoesNotThrow(() ->
                upsellingServiceKafkaConsumer.upsellingServiceConsumer(message, TEST_OFFSET, nullCorrelationId));

        verify(emailService).sendEmail(message);
        verify(emailService).sendEmail(argThat(dto ->
                "C002".equals(dto.customerId()) && "email".equals(dto.upsellingService())));
    }

    @Test
    void upsellingServiceConsumer_EmailServiceThrowsMessagingException_PropagatesException() throws MessagingException, MailException {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C003", "cloud");
        byte[] correlationIdBytes = "test-correlation-456".getBytes(StandardCharsets.UTF_8);
        MessagingException messagingException = new MessagingException("SMTP server unavailable");

        doThrow(messagingException).when(emailService).sendEmail(message);

        MessagingException thrownException = assertThrows(MessagingException.class, () ->
                upsellingServiceKafkaConsumer.upsellingServiceConsumer(message, TEST_OFFSET, correlationIdBytes));

        assertEquals("SMTP server unavailable", thrownException.getMessage());
        verify(emailService).sendEmail(message);
    }

    @Test
    void upsellingServiceConsumer_EmailServiceThrowsMailException_PropagatesException() throws MessagingException, MailException {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C004", "spid");
        byte[] correlationIdBytes = "mail-error-789".getBytes(StandardCharsets.UTF_8);
        MailException mailException = new MailException("Mail server connection failed") {};

        doThrow(mailException).when(emailService).sendEmail(message);

        MailException thrownException = assertThrows(MailException.class, () ->
                upsellingServiceKafkaConsumer.upsellingServiceConsumer(message, TEST_OFFSET, correlationIdBytes));

        assertEquals("Mail server connection failed", thrownException.getMessage());
        verify(emailService).sendEmail(message);
    }

    @Test
    void upsellingServiceConsumerDLQ_WithValidMessage_ProcessesWithoutException() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C005", "pec");
        byte[] correlationIdBytes = "dlt-correlation-999".getBytes(StandardCharsets.UTF_8);
        Long dltOffset = 456L;

        assertDoesNotThrow(() ->
                upsellingServiceKafkaConsumer.upsellingServiceConsumerDLQ(message, dltOffset, correlationIdBytes));

        verifyNoInteractions(emailService);
    }

    @Test
    void upsellingServiceConsumer_WithDifferentMessageTypes_ProcessesCorrectly() throws MessagingException, MailException {
        UpsellingServiceDTO hostingMessage = new UpsellingServiceDTO("C001", "hosting");
        UpsellingServiceDTO emailMessage = new UpsellingServiceDTO("C002", "email");
        UpsellingServiceDTO storageMessage = new UpsellingServiceDTO("C003", "spid");

        byte[] correlationId1 = "correlation-1".getBytes(StandardCharsets.UTF_8);
        byte[] correlationId2 = "correlation-2".getBytes(StandardCharsets.UTF_8);
        byte[] correlationId3 = "correlation-3".getBytes(StandardCharsets.UTF_8);

        upsellingServiceKafkaConsumer.upsellingServiceConsumer(hostingMessage, 100L, correlationId1);
        upsellingServiceKafkaConsumer.upsellingServiceConsumer(emailMessage, 101L, correlationId2);
        upsellingServiceKafkaConsumer.upsellingServiceConsumer(storageMessage, 102L, correlationId3);

        verify(emailService).sendEmail(hostingMessage);
        verify(emailService).sendEmail(emailMessage);
        verify(emailService).sendEmail(storageMessage);
        verify(emailService, times(3)).sendEmail(any(UpsellingServiceDTO.class));
    }

    @Test
    void upsellingServiceConsumer_VerifiesMessageContent() throws MessagingException, MailException {
        UpsellingServiceDTO specificMessage = new UpsellingServiceDTO("C009", "hosting");
        byte[] specificCorrelationId = "123-correlation-id".getBytes(StandardCharsets.UTF_8);

        upsellingServiceKafkaConsumer.upsellingServiceConsumer(specificMessage, 555L, specificCorrelationId);

        verify(emailService).sendEmail(argThat(dto ->
                "C009".equals(dto.customerId()) &&
                        "hosting".equals(dto.upsellingService())));
        verify(emailService, times(1)).sendEmail(any(UpsellingServiceDTO.class));
    }
}
