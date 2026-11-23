package com.fleetops.common.events;

/** Nombres de tópicos compartidos por productores y consumidores. */
public final class KafkaTopics {
    public static final String TRIP_EVENTS = "fleetops.viajes.eventos";
    public static final String TELEMETRY = "fleetops.telemetria.gps";
    public static final String DLT_SUFFIX = ".DLT";

    private KafkaTopics() {
    }
}
