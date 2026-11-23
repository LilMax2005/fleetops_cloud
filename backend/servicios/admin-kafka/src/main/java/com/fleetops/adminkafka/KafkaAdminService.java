package com.fleetops.adminkafka;

import com.fleetops.adminkafka.KafkaAdminDtos.ConsumerGroupDetail;
import com.fleetops.adminkafka.KafkaAdminDtos.CreateTopicRequest;
import com.fleetops.adminkafka.KafkaAdminDtos.GroupMember;
import com.fleetops.adminkafka.KafkaAdminDtos.GroupOffset;
import com.fleetops.adminkafka.KafkaAdminDtos.PartitionInfo;
import com.fleetops.adminkafka.KafkaAdminDtos.TopicDescription;
import com.fleetops.adminkafka.KafkaAdminDtos.TopicSummary;
import com.fleetops.adminkafka.KafkaAdminDtos.UpdateConfigsRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/** Capa de gestión de la plataforma Kafka sobre AdminClient: tópicos, configuraciones y consumer groups. */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final long TIMEOUT_S = 15;

    /** Configuraciones de tópico que el administrador permite modificar. */
    static final Set<String> ALLOWED_CONFIGS = Set.of("retention.ms", "retention.bytes", "cleanup.policy",
            "min.insync.replicas", "segment.ms", "segment.bytes", "compression.type", "max.message.bytes",
            "delete.retention.ms", "min.compaction.lag.ms", "message.timestamp.type");

    private final AdminClient admin;

    public KafkaAdminService(AdminClient admin) {
        this.admin = admin;
    }

    // ---------- Cluster ----------
    public Map<String, Object> cluster() {
        DescribeClusterResult r = admin.describeCluster();
        Node controller = await(r.controller());
        Collection<Node> nodes = await(r.nodes());
        return Map.of("clusterId", await(r.clusterId()),
                "controller", controller.idString(),
                "brokers", nodes.stream().sorted(Comparator.comparingInt(Node::id))
                        .map(n -> Map.of("id", n.id(), "host", n.host(), "port", n.port())).toList());
    }

    // ---------- Tópicos ----------
    public List<TopicSummary> listTopics(boolean includeInternal) {
        Set<String> names = await(admin.listTopics(new org.apache.kafka.clients.admin.ListTopicsOptions()
                .listInternal(includeInternal)).names());
        return await(admin.describeTopics(names).allTopicNames()).values().stream()
                .map(d -> new TopicSummary(d.name(), d.partitions().size(),
                        d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size()))
                .sorted(Comparator.comparing(TopicSummary::name))
                .toList();
    }

    public TopicDescription describeTopic(String name) {
        var d = await(admin.describeTopics(List.of(name)).allTopicNames()).get(name);
        ConfigResource res = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config cfg = await(admin.describeConfigs(List.of(res)).all()).get(res);
        Map<String, String> configs = cfg.entries().stream()
                .filter(e -> e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG || ALLOWED_CONFIGS.contains(e.name()))
                .collect(Collectors.toMap(ConfigEntry::name, e -> e.value() == null ? "" : e.value(), (a, b) -> a, java.util.TreeMap::new));
        List<PartitionInfo> parts = d.partitions().stream().map(p -> new PartitionInfo(p.partition(),
                p.leader() == null ? -1 : p.leader().id(),
                p.replicas().stream().map(Node::id).toList(),
                p.isr().stream().map(Node::id).toList())).toList();
        return new TopicDescription(name, d.isInternal(), parts.size(),
                parts.isEmpty() ? 0 : parts.get(0).replicas().size(), parts, configs);
    }

    public TopicDescription createTopic(CreateTopicRequest r) {
        Map<String, String> configs = r.configs() == null ? Map.of() : r.configs();
        validateKeys(configs.keySet());
        NewTopic topic = new NewTopic(r.name(), r.partitions(), r.replicationFactor()).configs(configs);
        await(admin.createTopics(List.of(topic)).all());
        log.info("Tópico creado {} partitions={} rf={} configs={}", r.name(), r.partitions(), r.replicationFactor(), configs);
        return describeTopic(r.name());
    }

    public TopicDescription updateConfigs(String name, UpdateConfigsRequest r) {
        List<AlterConfigOp> ops = new ArrayList<>();
        if (r.set() != null) {
            validateKeys(r.set().keySet());
            r.set().forEach((k, v) -> ops.add(new AlterConfigOp(new ConfigEntry(k, v), AlterConfigOp.OpType.SET)));
        }
        if (r.remove() != null) {
            validateKeys(r.remove());
            r.remove().forEach(k -> ops.add(new AlterConfigOp(new ConfigEntry(k, null), AlterConfigOp.OpType.DELETE)));
        }
        if (ops.isEmpty()) {
            throw new IllegalArgumentException("No se indicaron configuraciones a modificar");
        }
        ConfigResource res = new ConfigResource(ConfigResource.Type.TOPIC, name);
        await(admin.incrementalAlterConfigs(Map.of(res, ops)).all());
        log.info("Tópico {} configuraciones actualizadas set={} remove={}", name, r.set(), r.remove());
        return describeTopic(name);
    }

    public TopicDescription increasePartitions(String name, int total) {
        int current = describeTopic(name).partitions();
        if (total <= current) {
            throw new IllegalArgumentException("Kafka solo permite aumentar particiones (actual: " + current + ")");
        }
        await(admin.createPartitions(Map.of(name, NewPartitions.increaseTo(total))).all());
        log.info("Tópico {} particiones {} -> {}", name, current, total);
        // la metadata se propaga de forma asíncrona entre brokers: espera hasta verla aplicada
        TopicDescription d = describeTopic(name);
        for (int i = 0; i < 10 && d.partitions() < total; i++) {
            sleep(300);
            d = describeTopic(name);
        }
        return d;
    }

    public void deleteTopic(String name) {
        if (name.startsWith("__")) {
            throw new IllegalArgumentException("No se pueden eliminar tópicos internos: " + name);
        }
        await(admin.deleteTopics(List.of(name)).all());
        log.info("Tópico eliminado {}", name);
    }

    /** Elimina registros anteriores a un offset (p. ej. limpiar un DLT ya revisado). */
    public void deleteRecords(String topic, Map<Integer, Long> beforeOffsetByPartition) {
        Map<TopicPartition, RecordsToDelete> req = new LinkedHashMap<>();
        beforeOffsetByPartition.forEach((p, o) -> req.put(new TopicPartition(topic, p), RecordsToDelete.beforeOffset(o)));
        await(admin.deleteRecords(req).all());
    }

    // ---------- Consumer groups ----------
    public List<Map<String, String>> listConsumerGroups() {
        Collection<ConsumerGroupListing> groups = await(admin.listConsumerGroups().all());
        return groups.stream()
                .sorted(Comparator.comparing(ConsumerGroupListing::groupId))
                .map(g -> Map.of("groupId", g.groupId(),
                        "state", g.state().map(Enum::name).orElse("UNKNOWN")))
                .toList();
    }

    public ConsumerGroupDetail describeConsumerGroup(String groupId) {
        ConsumerGroupDescription d = await(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        Map<TopicPartition, OffsetAndMetadata> committed =
                await(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetSpec> latestReq = committed.keySet().stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> ends = latestReq.isEmpty()
                ? Map.of() : await(admin.listOffsets(latestReq).all());

        List<GroupOffset> offsets = committed.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> {
                    long end = ends.containsKey(e.getKey()) ? ends.get(e.getKey()).offset() : e.getValue().offset();
                    return new GroupOffset(e.getKey().topic(), e.getKey().partition(), e.getValue().offset(), end,
                            Math.max(0, end - e.getValue().offset()));
                })
                .sorted(Comparator.comparing(GroupOffset::topic).thenComparingInt(GroupOffset::partition))
                .toList();
        List<GroupMember> members = d.members().stream().map(m -> new GroupMember(m.consumerId(), m.clientId(), m.host(),
                m.assignment().topicPartitions().stream().map(TopicPartition::toString).sorted().toList())).toList();
        return new ConsumerGroupDetail(groupId, d.state().name(),
                d.coordinator() == null ? "-" : d.coordinator().idString(), d.partitionAssignor(), members, offsets,
                offsets.stream().mapToLong(GroupOffset::lag).sum());
    }

    /** Reinicia offsets de un grupo (el grupo debe estar sin consumidores activos, regla de Kafka). */
    public ConsumerGroupDetail resetOffsets(String groupId, String topic, String strategy) {
        var desc = await(admin.describeTopics(List.of(topic)).allTopicNames()).get(topic);
        OffsetSpec spec = "earliest".equals(strategy) ? OffsetSpec.earliest() : OffsetSpec.latest();
        Map<TopicPartition, OffsetSpec> req = desc.partitions().stream()
                .collect(Collectors.toMap(p -> new TopicPartition(topic, p.partition()), p -> spec));
        Map<TopicPartition, OffsetAndMetadata> target = await(admin.listOffsets(req).all()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new OffsetAndMetadata(e.getValue().offset())));
        await(admin.alterConsumerGroupOffsets(groupId, target).all());
        log.info("Offsets de {} en {} reiniciados a {}", groupId, topic, strategy);
        return describeConsumerGroup(groupId);
    }

    public void deleteConsumerGroup(String groupId) {
        await(admin.deleteConsumerGroups(List.of(groupId)).all());
        log.info("Consumer group eliminado {}", groupId);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void validateKeys(Collection<String> keys) {
        List<String> invalid = keys.stream().filter(k -> !ALLOWED_CONFIGS.contains(k)).toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Configuraciones no permitidas: " + invalid + ". Permitidas: " + ALLOWED_CONFIGS);
        }
    }

    private static <T> T await(org.apache.kafka.common.KafkaFuture<T> future) {
        try {
            return future.get(TIMEOUT_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminException("Operación interrumpida", e);
        } catch (ExecutionException e) {
            throw new KafkaAdminException(e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new KafkaAdminException("Timeout esperando respuesta del clúster Kafka", e);
        }
    }
}
