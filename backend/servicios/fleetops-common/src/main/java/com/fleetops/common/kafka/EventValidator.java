package com.fleetops.common.kafka;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

/** Valida los DTO recibidos desde Kafka con las anotaciones jakarta.validation del record. */
public class EventValidator {

    private final Validator validator;

    public EventValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(T event) {
        if (event == null) {
            throw new InvalidEventException("Payload vacío (tombstone o JSON nulo)");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new InvalidEventException(event.getClass().getSimpleName() + " inválido: " + detail);
        }
        return event;
    }
}
