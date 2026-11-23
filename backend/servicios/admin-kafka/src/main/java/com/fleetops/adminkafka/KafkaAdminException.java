package com.fleetops.adminkafka;

public class KafkaAdminException extends RuntimeException {
    public KafkaAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
