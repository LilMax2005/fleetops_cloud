package com.fleetops.common.events;

/** Ciclo de vida de un viaje publicado en el tópico fleetops.viajes.eventos. */
public enum TripEventType {
    CREATED, STARTED, DELAYED, INCIDENT, COMPLETED, CANCELLED
}
