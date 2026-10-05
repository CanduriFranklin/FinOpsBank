package com.finopsbank.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaTransactionListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaTransactionListener.class);

    @KafkaListener(topics = "finopsbank-transactions", groupId = "finopsbank-group")
    public void listen(String message) {
        log.info("📥 [KAFKA CONSUME] Evento recibido desde Kafka: {}", message);
    }
}