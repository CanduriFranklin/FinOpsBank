package com.finopsbank.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);
    private static final String TOPIC = "finopsbank-transactions";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(String key, String message) {
        try {
            kafkaTemplate.send(TOPIC, key, message);
            log.info("📤 [KAFKA PUBLISH] Key: {} | Message: {}", key, message);
        } catch (Exception e) {
            log.error("❌ Error al publicar en Kafka: {}", e.getMessage());
        }
    }

    public void publishEvent(TransactionEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getAccountNumber(), event);
            log.info("📤 [KAFKA PUBLISH] Evento emitido a Kafka para la cuenta: {}", event.getAccountNumber());
        } catch (Exception e) {
            log.error("❌ Error al publicar en Kafka: {}", e.getMessage());
        }
    }
}