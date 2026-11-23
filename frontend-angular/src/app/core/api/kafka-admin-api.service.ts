import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { ConsumerGroupDetail, CreateTopic, Row, TopicDescription, TopicSummary } from '../models/models';

/** admin-kafka (APIM /fleetops-admin-kafka). Requiere el app role FleetOps.Admin. */
@Injectable({ providedIn: 'root' })
export class KafkaAdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.api.adminKafka}/api/v1/admin/kafka`;

  cluster() {
    return this.http.get<Row>(`${this.base}/cluster`);
  }

  topics() {
    return this.http.get<TopicSummary[]>(`${this.base}/topics`);
  }

  topic(name: string) {
    return this.http.get<TopicDescription>(`${this.base}/topics/${encodeURIComponent(name)}`);
  }

  createTopic(body: CreateTopic) {
    return this.http.post<TopicDescription>(`${this.base}/topics`, body);
  }

  updateConfigs(name: string, set: Record<string, string>, remove: string[] = []) {
    return this.http.put<TopicDescription>(`${this.base}/topics/${encodeURIComponent(name)}/configs`, { set, remove });
  }

  increasePartitions(name: string, totalPartitions: number) {
    return this.http.post<TopicDescription>(`${this.base}/topics/${encodeURIComponent(name)}/partitions`, {
      totalPartitions,
    });
  }

  deleteTopic(name: string) {
    return this.http.delete<Row>(`${this.base}/topics/${encodeURIComponent(name)}`);
  }

  groups() {
    return this.http.get<{ groupId: string; state: string }[]>(`${this.base}/consumer-groups`);
  }

  group(id: string) {
    return this.http.get<ConsumerGroupDetail>(`${this.base}/consumer-groups/${encodeURIComponent(id)}`);
  }

  resetOffsets(id: string, topic: string, strategy: 'earliest' | 'latest') {
    return this.http.post<ConsumerGroupDetail>(`${this.base}/consumer-groups/${encodeURIComponent(id)}/reset-offsets`, {
      topic,
      strategy,
    });
  }
}
