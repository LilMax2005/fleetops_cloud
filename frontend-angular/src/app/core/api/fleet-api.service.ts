import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Alert, CreateIncident, Telemetry, Trip, TripDetail, TripStatus, VehiclePosition } from '../models/models';

/** servicio-flota original (ver fleetops-openapi.json). En Azure pasa por APIM; el token lo agrega MsalInterceptor. */
@Injectable({ providedIn: 'root' })
export class FleetApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.api.flota}/api/v1`;

  /** Viajes activos (no completados ni cancelados). */
  trips(status?: TripStatus) {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Trip[]>(`${this.base}/trips`, { params });
  }

  trip(id: string) {
    return this.http.get<TripDetail>(`${this.base}/trips/${id}`);
  }

  tripAlerts(id: string, status?: 'OPEN' | 'RESOLVED') {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Alert[]>(`${this.base}/trips/${id}/alerts`, { params });
  }

  resolveAlert(id: number) {
    return this.http.patch<Alert>(`${this.base}/alerts/${id}/status`, { status: 'RESOLVED' });
  }

  vehiclePosition(vehicleId: string) {
    return this.http.get<VehiclePosition>(`${this.base}/vehicles/${vehicleId}/position`);
  }

  /** RabbitMQ (fleetops.telemetry.queue) -> procesamiento -> Kafka fleetops.telemetria.gps. */
  sendTelemetry(body: Telemetry) {
    return this.http.post<{ eventId: string; status: string; duplicate: boolean }>(`${this.base}/telemetry`, body);
  }

  /** Marca el viaje en INCIDENT y publica el TripEvent INCIDENT en Kafka. */
  reportIncident(body: CreateIncident) {
    return this.http.post<{ id: number }>(`${this.base}/incidents`, body);
  }

  /** Productor de logs RabbitMQ (direct exchange fleetops.logs.direct). */
  publishLog(body: { level: string; message: string; simulateFailure?: boolean }) {
    return this.http.post(`${environment.api.flota}/api/logs`, body, { responseType: 'text' });
  }
}
