import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FleetApiService } from '../../core/api/fleet-api.service';
import { errorMessage } from '../../core/api/error-message';
import { AlertSeverity, IncidentType, Trip } from '../../core/models/models';

/**
 * Reporte de incidentes sobre viajes activos. servicio-flota marca el viaje en INCIDENT, publica
 * incident.created en RabbitMQ y el TripEvent INCIDENT en Kafka (fleetops.viajes.eventos).
 */
@Component({
  selector: 'app-trips',
  imports: [ReactiveFormsModule],
  template: `
    <div class="grid-2">
      <section class="card">
        <h2>Reportar incidente</h2>
        <form [formGroup]="form" (ngSubmit)="report()" class="form">
          <label>Viaje
            <select formControlName="tripId">
              <option value="" disabled>Seleccione…</option>
              @for (t of trips(); track t.id) {
                <option [value]="t.id">{{ t.vehicle.plate }} · {{ t.origin.name }} → {{ t.destination.name }} ({{ t.status }})</option>
              }
            </select>
          </label>
          <div class="row">
            <label>Tipo
              <select formControlName="type">@for (x of types; track x) { <option [value]="x">{{ x }}</option> }</select>
            </label>
            <label>Severidad
              <select formControlName="severity">@for (x of severities; track x) { <option [value]="x">{{ x }}</option> }</select>
            </label>
          </div>
          <label>Título <input formControlName="title" placeholder="Falla mecánica en ruta" /></label>
          <label>Descripción <input formControlName="description" placeholder="Detalle del incidente" /></label>
          <button class="btn primary" [disabled]="form.invalid || saving()">Reportar</button>
        </form>
        @if (message()) { <div class="banner">{{ message() }}</div> }
      </section>

      <section class="card">
        <h2>Viajes activos</h2>
        <table>
          <thead><tr><th>Vehículo</th><th>Conductor</th><th>Ruta</th><th>Estado</th><th>Avance</th></tr></thead>
          <tbody>
            @for (t of trips(); track t.id) {
              <tr>
                <td>{{ t.vehicle.plate }}</td>
                <td>{{ t.driver.name }}</td>
                <td>{{ t.origin.name }} → {{ t.destination.name }}</td>
                <td><span class="status s-{{ t.status }}">{{ t.status }}</span></td>
                <td>{{ t.progress }}%</td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="empty">Sin viajes activos.</td></tr>
            }
          </tbody>
        </table>
      </section>
    </div>
  `,
})
export class TripsComponent implements OnInit {
  private readonly api = inject(FleetApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly types: IncidentType[] = ['MECHANICAL_FAILURE', 'ROUTE_DEVIATION', 'PROLONGED_STOP', 'GENERAL'];
  protected readonly severities: AlertSeverity[] = ['INFO', 'WARNING', 'CRITICAL'];
  protected readonly trips = signal<Trip[]>([]);
  protected readonly message = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly form = this.fb.nonNullable.group({
    tripId: ['', Validators.required],
    type: ['GENERAL' as IncidentType, Validators.required],
    severity: ['WARNING' as AlertSeverity, Validators.required],
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', Validators.required],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.trips().subscribe({ next: (t) => this.trips.set(t), error: (e) => this.message.set(errorMessage(e)) });
  }

  report(): void {
    const v = this.form.getRawValue();
    const trip = this.trips().find((t) => t.id === v.tripId);
    if (!trip) return;
    this.saving.set(true);
    this.api.reportIncident({ ...v, vehicleId: trip.vehicle.id }).subscribe({
      next: (i) => {
        this.message.set(`Incidente #${i.id} registrado; evento INCIDENT publicado en Kafka.`);
        this.form.reset();
        this.saving.set(false);
        this.load();
      },
      error: (e) => {
        this.message.set(errorMessage(e));
        this.saving.set(false);
      },
    });
  }
}
