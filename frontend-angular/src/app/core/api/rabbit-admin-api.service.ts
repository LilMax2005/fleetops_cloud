import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Row } from '../models/models';

/** admin-rabbitmq (APIM /fleetops-admin-rabbitmq). Requiere el app role FleetOps.Admin. */
@Injectable({ providedIn: 'root' })
export class RabbitAdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.api.adminRabbit}/api/v1/admin/rabbitmq`;

  nodes() {
    return this.http.get<Row[]>(`${this.base}/nodes`);
  }

  queues() {
    return this.http.get<Row[]>(`${this.base}/queues`);
  }

  exchanges() {
    return this.http.get<Row[]>(`${this.base}/exchanges`);
  }

  bindings() {
    return this.http.get<Row[]>(`${this.base}/bindings`);
  }

  createQueue(body: { name: string; durable: boolean; withDlq: boolean; messageTtlMs?: number; quorum?: boolean }) {
    return this.http.post<Row>(`${this.base}/queues`, body);
  }

  deleteQueue(name: string) {
    return this.http.delete<Row>(`${this.base}/queues/${encodeURIComponent(name)}`);
  }

  createExchange(body: { name: string; type: string; durable: boolean }) {
    return this.http.post<Row>(`${this.base}/exchanges`, body);
  }

  deleteExchange(name: string) {
    return this.http.delete<Row>(`${this.base}/exchanges/${encodeURIComponent(name)}`);
  }

  createBinding(body: { exchange: string; queue: string; routingKey: string }) {
    return this.http.post<Row>(`${this.base}/bindings`, body);
  }

  deleteBinding(b: { exchange: string; queue: string; routingKey: string }) {
    const params = new HttpParams().set('exchange', b.exchange).set('queue', b.queue).set('routingKey', b.routingKey);
    return this.http.delete<Row>(`${this.base}/bindings`, { params });
  }
}
