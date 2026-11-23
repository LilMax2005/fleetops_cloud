import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AnalyticsSummary, TripMetric, VehicleStats } from '../models/models';

/** servicio-monitoreo (APIM /fleetops-monitoreo): resultados de la analítica en streaming. */
@Injectable({ providedIn: 'root' })
export class AnalyticsApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.api.monitoreo}/api/v1/analytics`;

  summary() {
    return this.http.get<AnalyticsSummary>(`${this.base}/summary`);
  }

  vehicles() {
    return this.http.get<VehicleStats[]>(`${this.base}/vehicles`);
  }

  trips() {
    return this.http.get<TripMetric[]>(`${this.base}/trips`);
  }
}
