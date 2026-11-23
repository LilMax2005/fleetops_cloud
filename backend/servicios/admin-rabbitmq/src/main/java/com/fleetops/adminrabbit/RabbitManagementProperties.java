package com.fleetops.adminrabbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** API HTTP de RabbitMQ Management (puerto 15672), usada solo para listar recursos. */
@ConfigurationProperties(prefix = "fleetops.rabbit.management")
public record RabbitManagementProperties(String url, String username, String password, String vhost) {
    public RabbitManagementProperties {
        vhost = vhost == null ? "/" : vhost;
    }
}
