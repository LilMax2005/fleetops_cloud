package com.fleetops.common.events;

/** Topología RabbitMQ definida en la EV2 (se mantiene sin cambios al incorporar Kafka). */
public final class RabbitNames {
    public static final String LOGS_EXCHANGE = "fleetops.logs.direct";
    public static final String EVENTS_EXCHANGE = "fleetops.events.topic";
    public static final String DLX_EXCHANGE = "fleetops.dlx.direct";

    public static final String LOGS_ALL_QUEUE = "fleetops.logs.all.queue";
    public static final String LOGS_ERRORS_QUEUE = "fleetops.logs.errors.queue";
    public static final String LOGS_SECURITY_QUEUE = "fleetops.logs.security.queue";
    public static final String ALERTS_QUEUE = "fleetops.alerts.queue";

    public static final String DLQ_SUFFIX = ".dlq";
    public static final String ALERT_ROUTING_PREFIX = "fleetops.alert.";

    private RabbitNames() {
    }

    /** fleetops.logs.all.queue -> fleetops.logs.all.dlq */
    public static String dlqOf(String queue) {
        return queue.replace(".queue", DLQ_SUFFIX);
    }
}
