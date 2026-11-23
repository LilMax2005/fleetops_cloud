package com.fleetops.adminkafka;

import com.fleetops.adminkafka.KafkaAdminDtos.ConsumerGroupDetail;
import com.fleetops.adminkafka.KafkaAdminDtos.CreateTopicRequest;
import com.fleetops.adminkafka.KafkaAdminDtos.DeleteRecordsRequest;
import com.fleetops.adminkafka.KafkaAdminDtos.IncreasePartitionsRequest;
import com.fleetops.adminkafka.KafkaAdminDtos.ResetOffsetsRequest;
import com.fleetops.adminkafka.KafkaAdminDtos.TopicDescription;
import com.fleetops.adminkafka.KafkaAdminDtos.TopicSummary;
import com.fleetops.adminkafka.KafkaAdminDtos.UpdateConfigsRequest;
import jakarta.validation.Valid;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Requiere el app role FleetOps.Admin (regla /api/v1/admin/** del filtro JWT común). */
@RestController
@RequestMapping("/api/v1/admin/kafka")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/cluster")
    public Map<String, Object> cluster() {
        return service.cluster();
    }

    @GetMapping("/topics")
    public List<TopicSummary> topics(@RequestParam(defaultValue = "false") boolean includeInternal) {
        return service.listTopics(includeInternal);
    }

    @GetMapping("/topics/{name}")
    public TopicDescription topic(@PathVariable String name) {
        return service.describeTopic(name);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicDescription create(@Valid @RequestBody CreateTopicRequest request) {
        return service.createTopic(request);
    }

    @PutMapping("/topics/{name}/configs")
    public TopicDescription updateConfigs(@PathVariable String name, @RequestBody UpdateConfigsRequest request) {
        return service.updateConfigs(name, request);
    }

    @PostMapping("/topics/{name}/partitions")
    public TopicDescription partitions(@PathVariable String name, @Valid @RequestBody IncreasePartitionsRequest request) {
        return service.increasePartitions(name, request.totalPartitions());
    }

    @DeleteMapping("/topics/{name}")
    public Map<String, Object> delete(@PathVariable String name) {
        service.deleteTopic(name);
        return Map.of("topic", name, "deleted", true);
    }

    @PostMapping("/topics/{name}/delete-records")
    public Map<String, Object> deleteRecords(@PathVariable String name, @Valid @RequestBody DeleteRecordsRequest request) {
        service.deleteRecords(name, request.beforeOffsetByPartition());
        return Map.of("topic", name, "deletedBefore", request.beforeOffsetByPartition());
    }

    @GetMapping("/consumer-groups")
    public List<Map<String, String>> groups() {
        return service.listConsumerGroups();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupDetail group(@PathVariable String groupId) {
        return service.describeConsumerGroup(groupId);
    }

    @PostMapping("/consumer-groups/{groupId}/reset-offsets")
    public ConsumerGroupDetail reset(@PathVariable String groupId, @Valid @RequestBody ResetOffsetsRequest request) {
        return service.resetOffsets(groupId, request.topic(), request.strategy());
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public Map<String, Object> deleteGroup(@PathVariable String groupId) {
        service.deleteConsumerGroup(groupId);
        return Map.of("groupId", groupId, "deleted", true);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(KafkaAdminException.class)
    public ProblemDetail kafka(KafkaAdminException ex) {
        Throwable c = ex.getCause();
        HttpStatus status = switch (c) {
            case UnknownTopicOrPartitionException ignored -> HttpStatus.NOT_FOUND;
            case TopicExistsException ignored -> HttpStatus.CONFLICT;
            case GroupNotEmptyException ignored -> HttpStatus.CONFLICT;
            case InvalidPartitionsException ignored -> HttpStatus.BAD_REQUEST;
            case InvalidReplicationFactorException ignored -> HttpStatus.BAD_REQUEST;
            case null, default -> HttpStatus.BAD_GATEWAY;
        };
        return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    }
}
