package it.bm;

import it.bm.kafka.ExpiredServicesKafkaProducer;
import it.bm.kafka.UpsellingServiceKafkaProducer;
import it.bm.model.kafka.ExpiredServicesDTO;
import it.bm.model.kafka.UpsellingServiceDTO;
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
class UpsellingServiceKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UpsellingServiceKafkaProducer upsellingServiceKafkaProducer;

    private static final String TEST_TOPIC = "test-upselling-service-topic";

    @BeforeEach
    void setUp() {
        upsellingServiceKafkaProducer = new UpsellingServiceKafkaProducer(kafkaTemplate);
        ReflectionTestUtils.setField(upsellingServiceKafkaProducer, "topicUpsellingService", TEST_TOPIC);
    }

    @Test
    void sendMessage_WithValidMessage_SendsToKafka() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("CUST001", "hosting");

        upsellingServiceKafkaProducer.sendMessage(message);

        verify(kafkaTemplate).send((ProducerRecord<String, Object>) argThat(record -> {
            ProducerRecord<String, Object> producerRecord = (ProducerRecord<String, Object>) record;
            return TEST_TOPIC.equals(producerRecord.topic()) &&
                    message.equals(producerRecord.value()) &&
                    producerRecord.key() == null;
        }));
    }

    @Test
    void sendMessage_WithDifferentServiceData_SendsCorrectMessage() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C001", "cloud");
        upsellingServiceKafkaProducer.sendMessage(message);

        verify(kafkaTemplate).send((ProducerRecord<String, Object>) argThat(record -> {
            ProducerRecord<String, Object> producerRecord = (ProducerRecord<String, Object>) record;
            UpsellingServiceDTO sentMessage = (UpsellingServiceDTO) producerRecord.value();
            return "C001".equals(sentMessage.customerId()) &&
                    "cloud".equals(sentMessage.upsellingService()) &&
                    TEST_TOPIC.equals(producerRecord.topic());
        }));
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    void sendMessage_VerifiesKafkaTemplateInteraction() {
        UpsellingServiceDTO message = new UpsellingServiceDTO("C001", "pec");

        upsellingServiceKafkaProducer.sendMessage(message);

        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
        verifyNoMoreInteractions(kafkaTemplate);
    }
}
