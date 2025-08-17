package it.bm.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${expired-services.kafka.client-id:expired-services-id}")
    private String expiredServicesClientId;

    @Value("${upselling-service.kafka.client-id:upselling-service-id}")
    private String upsellingServiceClientId;

    @Value("${kafka.producer.acks:all}")
    private String producerAcks;

    @Value("${kafka.producer.retries:3}")
    private String producerRetries;

    @Value("${kafka.producer.backoff.interval:1000}")
    private long backoffInterval;

    @Value("${kafka.producer.max-attempts:3}")
    private long maxAttempts;


    @Bean("expiredServicesKafkaTemplate")
    public KafkaTemplate<String, Object> expiredServicesKafkaTemplate() {
        Map<String, Object> configProps = createBaseProducerConfig();
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, expiredServicesClientId);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(configProps);

        return new KafkaTemplate<>(factory);
    }

    @Bean("upsellingServiceKafkaTemplate")
    public KafkaTemplate<String, Object> upsellingServiceKafkaTemplate() {
        Map<String, Object> configProps = createBaseProducerConfig();
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, upsellingServiceClientId);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(configProps);

        return new KafkaTemplate<>(factory);
    }

    private Map<String, Object> createBaseProducerConfig() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        configProps.put(ProducerConfig.RETRIES_CONFIG, producerRetries);
        configProps.put(ProducerConfig.ACKS_CONFIG, producerAcks);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return configProps;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "email-upselling-service");
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Set ack mode to RECORD for retryable topics
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean("defaultRetryTopicKafkaTemplate")
    public KafkaTemplate<String, Object> kafkaTemplate() {
        Map<String, Object> configProps = createBaseProducerConfig();
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(factory);
    }

}