package com.fleetops.monitoreo.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Consumer group "monitoreo-dlt-auditoria": registra cada mensaje que terminó en un DLT
 * (tópico origen, excepción y payload crudo) para poder investigarlo o reprocesarlo.
 */
@Component
public class DeadLetterAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterAuditConsumer.class);

    @KafkaListener(id = "monitoreo-dlt-listener", topicPattern = "fleetops\\..*\\.DLT",
            groupId = "monitoreo-dlt-auditoria",
            properties = {
                    "value.deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer",
                    "metadata.max.age.ms=30000"
            })
    public void onDeadLetter(ConsumerRecord<String, byte[]> record) {
        log.warn("DLT {} key={} origen={} excepcion={} payload={}", record.topic(), record.key(),
                header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC), header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                record.value() == null ? "null" : new String(record.value(), StandardCharsets.UTF_8));
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header h = record.headers().lastHeader(name);
        return h == null ? "-" : new String(h.value(), StandardCharsets.UTF_8);
    }
}
