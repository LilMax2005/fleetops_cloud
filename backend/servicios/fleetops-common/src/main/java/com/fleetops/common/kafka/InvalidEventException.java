package com.fleetops.common.kafka;

/** Payload que no cumple el contrato del DTO. No se reintenta: va directo al DLT. */
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
