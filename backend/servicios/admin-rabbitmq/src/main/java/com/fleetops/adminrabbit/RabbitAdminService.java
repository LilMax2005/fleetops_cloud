package com.fleetops.adminrabbit;

import com.fleetops.adminrabbit.RabbitAdminDtos.BindingRequest;
import com.fleetops.adminrabbit.RabbitAdminDtos.ExchangeRequest;
import com.fleetops.adminrabbit.RabbitAdminDtos.QueueRequest;
import com.fleetops.common.events.RabbitNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Operaciones de administración: creación/eliminación con AmqpAdmin (protocolo AMQP)
 * y listados con la API HTTP de Management.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {
    };

    private final AmqpAdmin admin;
    private final RestClient management;
    private final String vhost;

    public RabbitAdminService(AmqpAdmin admin, RabbitManagementProperties props, RestClient.Builder builder) {
        this.admin = admin;
        this.vhost = props.vhost();
        this.management = builder.baseUrl(props.url())
                .defaultHeaders(h -> h.setBasicAuth(props.username(), props.password()))
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    // ---------- Exchanges ----------
    public void createExchange(ExchangeRequest r) {
        ExchangeBuilder builder = new ExchangeBuilder(r.name(), r.type()).durable(r.durable() == null || r.durable());
        if (Boolean.TRUE.equals(r.autoDelete())) {
            builder.autoDelete();
        }
        Exchange ex = builder.build();
        admin.declareExchange(ex);
        log.info("Exchange creado {} ({})", r.name(), r.type());
    }

    public boolean deleteExchange(String name) {
        if (name.isBlank() || name.startsWith("amq.")) {
            throw new IllegalArgumentException("No se pueden eliminar exchanges del sistema: " + name);
        }
        boolean deleted = admin.deleteExchange(name);
        log.info("Exchange eliminado {} -> {}", name, deleted);
        return deleted;
    }

    // ---------- Colas ----------
    public Map<String, Object> createQueue(QueueRequest r) {
        QueueBuilder qb = (r.durable() == null || r.durable()) ? QueueBuilder.durable(r.name()) : QueueBuilder.nonDurable(r.name());
        if (Boolean.TRUE.equals(r.quorum())) {
            qb.quorum();
        }
        if (r.messageTtlMs() != null) {
            qb.ttl(r.messageTtlMs());
        }
        if (r.maxLength() != null) {
            qb.maxLength(r.maxLength());
        }
        String dlqName = null;
        if (Boolean.TRUE.equals(r.withDlq())) {
            dlqName = r.name().endsWith(".queue") ? RabbitNames.dlqOf(r.name()) : r.name() + RabbitNames.DLQ_SUFFIX;
            admin.declareExchange(ExchangeBuilder.directExchange(RabbitNames.DLX_EXCHANGE).durable(true).build());
            admin.declareQueue(QueueBuilder.durable(dlqName).build());
            admin.declareBinding(new Binding(dlqName, Binding.DestinationType.QUEUE, RabbitNames.DLX_EXCHANGE, dlqName, null));
            qb.deadLetterExchange(RabbitNames.DLX_EXCHANGE).deadLetterRoutingKey(dlqName);
        }
        Queue queue = qb.build();
        admin.declareQueue(queue);
        log.info("Cola creada {} args={} dlq={}", r.name(), queue.getArguments(), dlqName);
        return dlqName == null ? Map.of("queue", r.name()) : Map.of("queue", r.name(), "dlq", dlqName);
    }

    public boolean deleteQueue(String name, boolean ifUnused, boolean ifEmpty) {
        if (ifUnused || ifEmpty) {
            admin.deleteQueue(name, ifUnused, ifEmpty);
            return true;
        }
        return admin.deleteQueue(name);
    }

    public void purgeQueue(String name) {
        admin.purgeQueue(name, false);
    }

    public Properties queueInfo(String name) {
        return admin.getQueueProperties(name);
    }

    // ---------- Bindings ----------
    public void createBinding(BindingRequest r) {
        admin.declareBinding(new Binding(r.queue(), Binding.DestinationType.QUEUE, r.exchange(),
                r.routingKey() == null ? "" : r.routingKey(), null));
        log.info("Binding creado {} -[{}]-> {}", r.exchange(), r.routingKey(), r.queue());
    }

    public void deleteBinding(BindingRequest r) {
        admin.removeBinding(new Binding(r.queue(), Binding.DestinationType.QUEUE, r.exchange(),
                r.routingKey() == null ? "" : r.routingKey(), null));
        log.info("Binding eliminado {} -[{}]-> {}", r.exchange(), r.routingKey(), r.queue());
    }

    // ---------- Listados (Management API) ----------
    public List<Map<String, Object>> listQueues() {
        return pick(get("/api/queues/{vhost}", vhost), "name", "type", "durable", "state", "messages", "messages_ready",
                "messages_unacknowledged", "consumers", "arguments", "node");
    }

    public List<Map<String, Object>> listExchanges() {
        return pick(get("/api/exchanges/{vhost}", vhost), "name", "type", "durable", "auto_delete", "internal");
    }

    public List<Map<String, Object>> listBindings() {
        return pick(get("/api/bindings/{vhost}", vhost), "source", "destination", "destination_type", "routing_key");
    }

    public List<Map<String, Object>> clusterNodes() {
        return pick(get("/api/nodes"), "name", "running", "type", "uptime", "mem_used");
    }

    /** El vhost va como variable de URI para que "/" se codifique una sola vez (%2F). */
    private List<Map<String, Object>> get(String path, Object... vars) {
        return management.get().uri(path, vars).retrieve().body(LIST);
    }

    private static List<Map<String, Object>> pick(List<Map<String, Object>> rows, String... keys) {
        return rows == null ? List.of() : rows.stream().map(row -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            for (String k : keys) {
                if (row.containsKey(k)) {
                    m.put(k, row.get(k));
                }
            }
            return m;
        }).toList();
    }
}
