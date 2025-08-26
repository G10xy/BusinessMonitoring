package it.bm;

import it.bm.kafka.ExpiredServicesKafkaProducer;
import it.bm.model.kafka.ExpiredServicesDTO;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ExpiredServicesKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ExpiredServicesKafkaProducer expiredServicesKafkaProducer;

    private static final String TEST_TOPIC = "test-expired-services-topic";

    @BeforeEach
    void setUp() {
        expiredServicesKafkaProducer = new ExpiredServicesKafkaProducer(kafkaTemplate);
        ReflectionTestUtils.setField(expiredServicesKafkaProducer, "topicExpiredServices", TEST_TOPIC);
    }

    @Test
    void sendMessage_WithValidMessage_SendsToKafka() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C001", 5);
        expiredServicesKafkaProducer.sendMessage(message);
        verify(kafkaTemplate).send((ProducerRecord<String, Object>) argThat(record -> {
            ProducerRecord<String, Object> producerRecord = (ProducerRecord<String, Object>) record;
            return TEST_TOPIC.equals(producerRecord.topic()) &&
                    message.equals(producerRecord.value()) &&
                    producerRecord.key() == null;
        }));
    }

    @Test
    void sendMessage_WithDifferentCustomerData_SendsCorrectMessage() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C001", 10);
        expiredServicesKafkaProducer.sendMessage(message);
        verify(kafkaTemplate).send((ProducerRecord<String, Object>) argThat(record -> {
            ProducerRecord<String, Object> producerRecord = (ProducerRecord<String, Object>) record;
            ExpiredServicesDTO sentMessage = (ExpiredServicesDTO) producerRecord.value();
            return "C001".equals(sentMessage.customerId()) &&
                    sentMessage.numberOfExpiredServices() == 10 &&
                    TEST_TOPIC.equals(producerRecord.topic());
        }));
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    void sendMessage_VerifiesKafkaTemplateInteraction() {
        ExpiredServicesDTO message = new ExpiredServicesDTO("C001", 3);
        expiredServicesKafkaProducer.sendMessage(message);
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
        verifyNoMoreInteractions(kafkaTemplate);
    }
}
