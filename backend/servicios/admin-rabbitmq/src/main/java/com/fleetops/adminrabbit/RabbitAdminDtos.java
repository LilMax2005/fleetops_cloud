package com.fleetops.adminrabbit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public final class RabbitAdminDtos {

    private RabbitAdminDtos() {
    }

    public record ExchangeRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]+") String name,
            @NotBlank @Pattern(regexp = "direct|topic|fanout|headers") String type,
            Boolean durable,
            Boolean autoDelete) {
    }

    /** withDlq=true crea además &lt;nombre&gt;.dlq enlazada al exchange fleetops.dlx.direct. */
    public record QueueRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]+") String name,
            Boolean durable,
            Boolean withDlq,
            @Positive Integer messageTtlMs,
            @Positive Integer maxLength,
            Boolean quorum) {
    }

    public record BindingRequest(
            @NotBlank String exchange,
            @NotBlank String queue,
            String routingKey) {
    }
}
