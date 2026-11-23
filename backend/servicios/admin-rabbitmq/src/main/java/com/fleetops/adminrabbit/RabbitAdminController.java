package com.fleetops.adminrabbit;

import com.fleetops.adminrabbit.RabbitAdminDtos.BindingRequest;
import com.fleetops.adminrabbit.RabbitAdminDtos.ExchangeRequest;
import com.fleetops.adminrabbit.RabbitAdminDtos.QueueRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Requiere el app role FleetOps.Admin (regla /api/v1/admin/** del filtro JWT común). */
@RestController
@RequestMapping("/api/v1/admin/rabbitmq")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @GetMapping("/nodes")
    public List<Map<String, Object>> nodes() {
        return service.clusterNodes();
    }

    @GetMapping("/exchanges")
    public List<Map<String, Object>> exchanges() {
        return service.listExchanges();
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createExchange(@Valid @RequestBody ExchangeRequest request) {
        service.createExchange(request);
        return Map.of("exchange", request.name(), "type", request.type());
    }

    @DeleteMapping("/exchanges/{name}")
    public Map<String, Object> deleteExchange(@PathVariable String name) {
        return Map.of("exchange", name, "deleted", service.deleteExchange(name));
    }

    @GetMapping("/queues")
    public List<Map<String, Object>> queues() {
        return service.listQueues();
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<Properties> queue(@PathVariable String name) {
        Properties p = service.queueInfo(name);
        return p == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(p);
    }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createQueue(@Valid @RequestBody QueueRequest request) {
        return service.createQueue(request);
    }

    @DeleteMapping("/queues/{name}")
    public Map<String, Object> deleteQueue(@PathVariable String name,
                                           @RequestParam(defaultValue = "false") boolean ifUnused,
                                           @RequestParam(defaultValue = "false") boolean ifEmpty) {
        return Map.of("queue", name, "deleted", service.deleteQueue(name, ifUnused, ifEmpty));
    }

    @PostMapping("/queues/{name}/purge")
    public Map<String, Object> purge(@PathVariable String name) {
        service.purgeQueue(name);
        return Map.of("queue", name, "purged", true);
    }

    @GetMapping("/bindings")
    public List<Map<String, Object>> bindings() {
        return service.listBindings();
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public BindingRequest createBinding(@Valid @RequestBody BindingRequest request) {
        service.createBinding(request);
        return request;
    }

    @DeleteMapping("/bindings")
    public Map<String, Object> deleteBinding(@RequestParam String exchange, @RequestParam String queue,
                                             @RequestParam(defaultValue = "") String routingKey) {
        service.deleteBinding(new BindingRequest(exchange, queue, routingKey));
        return Map.of("exchange", exchange, "queue", queue, "routingKey", routingKey, "deleted", true);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    public ProblemDetail management(org.springframework.web.client.RestClientException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "RabbitMQ Management no disponible: " + ex.getMessage());
    }

    @ExceptionHandler(org.springframework.amqp.AmqpException.class)
    public ProblemDetail amqp(org.springframework.amqp.AmqpException ex) {
        Throwable root = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, root.getMessage());
    }
}
