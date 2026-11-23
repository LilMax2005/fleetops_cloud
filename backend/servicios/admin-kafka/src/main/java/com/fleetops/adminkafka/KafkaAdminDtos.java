package com.fleetops.adminkafka;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

public final class KafkaAdminDtos {

    private KafkaAdminDtos() {
    }

    public record CreateTopicRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}") String name,
            @Min(1) @Max(100) int partitions,
            @Min(1) @Max(5) short replicationFactor,
            Map<String, String> configs) {
    }

    /** Configuraciones a fijar (SET); las claves en "remove" vuelven al valor por defecto del broker. */
    public record UpdateConfigsRequest(Map<String, String> set, List<String> remove) {
    }

    public record IncreasePartitionsRequest(@Min(1) int totalPartitions) {
    }

    public record ResetOffsetsRequest(@NotBlank String topic,
                                      @NotBlank @Pattern(regexp = "earliest|latest") String strategy) {
    }

    public record DeleteRecordsRequest(@NotEmpty Map<Integer, Long> beforeOffsetByPartition) {
    }

    public record PartitionInfo(int partition, int leader, List<Integer> replicas, List<Integer> isr) {
    }

    public record TopicDescription(String name, boolean internal, int partitions, int replicationFactor,
                                   List<PartitionInfo> partitionInfo, Map<String, String> configs) {
    }

    public record TopicSummary(String name, int partitions, int replicationFactor) {
    }

    public record GroupOffset(String topic, int partition, long committedOffset, long endOffset, long lag) {
    }

    public record GroupMember(String memberId, String clientId, String host, List<String> assignment) {
    }

    public record ConsumerGroupDetail(String groupId, String state, String coordinator, String assignor,
                                      List<GroupMember> members, List<GroupOffset> offsets, long totalLag) {
    }
}
