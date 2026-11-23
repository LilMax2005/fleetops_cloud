import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AnalyticsApiService } from '../../core/api/analytics-api.service';
import { errorMessage } from '../../core/api/error-message';
import { AnalyticsSummary, TripMetric, VehicleStats } from '../../core/models/models';

/** Analítica en streaming calculada por servicio-monitoreo a partir de los dos tópicos Kafka. */
@Component({
  selector: 'app-analytics',
  imports: [DatePipe],
  template: `
    @if (error()) { <div class="banner error">{{ error() }}</div> }
    @if (summary(); as s) {
      <div class="kpis">
        <div class="kpi"><small>Viajes monitoreados</small><strong>{{ s.tripsTracked }}</strong></div>
        <div class="kpi"><small>En ruta</small><strong>{{ s.tripsEnRoute }}</strong></div>
        <div class="kpi"><small>Retrasos</small><strong>{{ s.totalDelays }}</strong></div>
        <div class="kpi"><small>Incidentes</small><strong>{{ s.totalIncidents }}</strong></div>
        <div class="kpi"><small>Duración media</small><strong>{{ s.avgTripDurationMinutes }} min</strong></div>
        <div class="kpi"><small>Lecturas GPS</small><strong>{{ s.telemetryReadings }}</strong></div>
        <div class="kpi"><small>Alertas generadas</small><strong>{{ s.alertsRaised }}</strong></div>
      </div>
    }
    <div class="grid-2">
      <section class="card">
        <h2>Vehículos (tópico fleetops.telemetria.gps)</h2>
        <table>
          <thead><tr><th>Vehículo</th><th>Lecturas</th><th>Vel. media</th><th>Vel. máx</th><th>Combustible</th><th>Alertas</th><th>Última</th></tr></thead>
          <tbody>
            @for (v of vehicles(); track v.vehicleId) {
              <tr><td>{{ v.vehicleId }}</td><td>{{ v.readings }}</td><td>{{ v.avgSpeedKmh }}</td><td>{{ v.maxSpeedKmh }}</td>
                <td>{{ v.lastFuelLevel ?? '—' }}</td><td>{{ v.alertsRaised }}</td><td>{{ v.lastReadingAt | date: 'mediumTime' }}</td></tr>
            } @empty { <tr><td colspan="7" class="empty">Sin lecturas aún.</td></tr> }
          </tbody>
        </table>
      </section>
      <section class="card">
        <h2>Viajes (tópico fleetops.viajes.eventos)</h2>
        <table>
          <thead><tr><th>Viaje</th><th>Vehículo</th><th>Estado</th><th>Eventos</th><th>Retrasos</th><th>Incidentes</th><th>Duración</th></tr></thead>
          <tbody>
            @for (t of trips(); track t.tripId) {
              <tr><td>{{ t.tripId }}</td><td>{{ t.vehiclePlate ?? t.vehicleId }}</td><td><span class="status s-{{ t.status }}">{{ t.status }}</span></td>
                <td>{{ t.events }}</td><td>{{ t.delays }}</td><td>{{ t.incidents }}</td><td>{{ t.durationMinutes ?? '—' }}</td></tr>
            } @empty { <tr><td colspan="7" class="empty">Sin eventos aún.</td></tr> }
          </tbody>
        </table>
      </section>
    </div>
  `,
})
export class AnalyticsComponent implements OnInit, OnDestroy {
  private readonly api = inject(AnalyticsApiService);
  private timer?: ReturnType<typeof setInterval>;
  protected readonly summary = signal<AnalyticsSummary | null>(null);
  protected readonly vehicles = signal<VehicleStats[]>([]);
  protected readonly trips = signal<TripMetric[]>([]);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
    this.timer = setInterval(() => this.load(), 5000); // refresco "casi en tiempo real"
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }

  private load(): void {
    forkJoin({ s: this.api.summary(), v: this.api.vehicles(), t: this.api.trips() }).subscribe({
      next: ({ s, v, t }) => {
        this.summary.set(s);
        this.vehicles.set(v);
        this.trips.set(t);
        this.error.set(null);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
