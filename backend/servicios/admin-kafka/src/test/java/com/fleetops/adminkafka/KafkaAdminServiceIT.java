package com.fleetops.adminkafka;

import com.fleetops.adminkafka.KafkaAdminDtos.CreateTopicRequest;
import com.fleetops.adminkafka.KafkaAdminDtos.UpdateConfigsRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba de integración contra un broker Kafka embebido: ciclo completo de administración de un tópico. */
class KafkaAdminServiceIT {

    static EmbeddedKafkaBroker broker;
    static AdminClient client;
    static KafkaAdminService service;

    @BeforeAll
    static void start() {
        broker = new EmbeddedKafkaKraftBroker(1, 1);
        broker.afterPropertiesSet();
        client = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString()));
        service = new KafkaAdminService(client);
    }

    @AfterAll
    static void stop() {
        client.close();
        broker.destroy();
    }

    @Test
    void creaModificaAmpliaYEliminaTopico() {
        var created = service.createTopic(new CreateTopicRequest("test.flota", 2, (short) 1,
                Map.of("retention.ms", "3600000", "cleanup.policy", "delete")));
        assertThat(created.partitions()).isEqualTo(2);
        assertThat(created.configs()).containsEntry("retention.ms", "3600000");

        var updated = service.updateConfigs("test.flota",
                new UpdateConfigsRequest(Map.of("cleanup.policy", "compact"), List.of("retention.ms")));
        assertThat(updated.configs()).containsEntry("cleanup.policy", "compact");
        assertThat(updated.configs().get("retention.ms")).isNotEqualTo("3600000");

        assertThat(service.increasePartitions("test.flota", 4).partitions()).isEqualTo(4);
        assertThatThrownBy(() -> service.increasePartitions("test.flota", 3)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.listTopics(false)).extracting(KafkaAdminDtos.TopicSummary::name).contains("test.flota");

        service.deleteTopic("test.flota");
        assertThat(service.cluster()).containsKey("brokers");
    }

    @Test
    void rechazaConfiguracionesNoPermitidas() {
        assertThatThrownBy(() -> service.createTopic(new CreateTopicRequest("test.x", 1, (short) 1,
                Map.of("unclean.leader.election.enable", "true")))).isInstanceOf(IllegalArgumentException.class);
    }
}
