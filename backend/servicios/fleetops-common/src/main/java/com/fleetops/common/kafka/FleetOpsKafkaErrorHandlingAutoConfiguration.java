package com.fleetops.common.kafka;

import com.fleetops.common.events.KafkaTopics;
import jakarta.validation.Validator;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo de errores común para todos los @KafkaListener:
 * <ul>
 *   <li>Reintentos con backoff exponencial (1s, 2s, 4s) para fallas transitorias.</li>
 *   <li>Sin reintentos para payloads inválidos o no deserializables.</li>
 *   <li>Al agotar reintentos el registro se publica en &lt;tópico&gt;.DLT con headers de la excepción
 *       y queda registrado en el log, sin detener el consumidor.</li>
 * </ul>
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
public class FleetOpsKafkaErrorHandlingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FleetOpsKafkaErrorHandlingAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public EventValidator eventValidator(Validator validator) {
        return new EventValidator(validator);
    }

    /** Template dedicado al DLT: reenvía bytes crudos (deserialización fallida) u objetos como JSON. */
    private static KafkaTemplate<Object, Object> deadLetterTemplate(KafkaProperties props) {
        Map<String, Object> cfg = new LinkedHashMap<>(props.buildProducerProperties(null));
        cfg.put(ProducerConfig.CLIENT_ID_CONFIG, cfg.getOrDefault(ProducerConfig.CLIENT_ID_CONFIG, "fleetops") + "-dlt");
        Map<Class<?>, Serializer<?>> byType = new LinkedHashMap<>();
        byType.put(byte[].class, new ByteArraySerializer());
        byType.put(Object.class, new JsonSerializer<>());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(cfg, keySerializer(),
                new DelegatingByTypeSerializer(byType, true)));
    }

    @SuppressWarnings("unchecked")
    private static Serializer<Object> keySerializer() {
        return (Serializer<Object>) (Serializer<?>) new StringSerializer();
    }

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public DefaultErrorHandler kafkaErrorHandler(KafkaProperties kafkaProperties,
                                                 @Value("${fleetops.kafka.retry.max-attempts:3}") int maxRetries) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(deadLetterTemplate(kafkaProperties),
                // partición negativa = Kafka elige la partición del DLT (no exige el mismo número de particiones)
                (record, ex) -> new TopicPartition(record.topic() + KafkaTopics.DLT_SUFFIX, -1));

        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxAttempts(maxRetries);

        DefaultErrorHandler handler = new DefaultErrorHandler((record, ex) -> {
            log.error("Evento enviado a DLT topic={} partition={} offset={} key={} causa={}",
                    record.topic(), record.partition(), record.offset(), record.key(), rootMessage(ex));
            recoverer.accept(record, ex);
        }, backOff);
        handler.addNotRetryableExceptions(InvalidEventException.class, IllegalArgumentException.class);
        handler.setRetryListeners((record, ex, attempt) ->
                log.warn("Reintento {} topic={} offset={} causa={}", attempt, record.topic(), record.offset(), rootMessage(ex)));
        return handler;
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
