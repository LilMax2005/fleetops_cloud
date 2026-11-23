import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FleetApiService } from '../../core/api/fleet-api.service';
import { errorMessage } from '../../core/api/error-message';
import { Trip, VehiclePosition } from '../../core/models/models';

/**
 * Simulador de telemetria GPS sobre los viajes activos. POST /api/v1/telemetry -> RabbitMQ ->
 * servicio-flota procesa (posicion, alertas) -> Kafka fleetops.telemetria.gps -> servicio-monitoreo.
 */
@Component({
  selector: 'app-fleet',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  template: `
    <div class="grid-2">
      <section class="card">
        <h2>Vehículos en ruta</h2>
        <table>
          <thead><tr><th>Patente</th><th>Viaje</th><th>Última posición</th><th>Velocidad</th><th>Combustible</th><th>Hora</th></tr></thead>
          <tbody>
            @for (t of trips(); track t.id) {
              @let p = positions()[t.vehicle.id];
              <tr>
                <td>{{ t.vehicle.plate }}</td><td>{{ t.status }} · {{ t.progress }}%</td>
                <td>@if (p) { {{ p.latitude | number: '1.4-4' }}, {{ p.longitude | number: '1.4-4' }} } @else { — }</td>
                <td>{{ p ? (p.speedKph | number: '1.0-0') + ' km/h' : '—' }}</td>
                <td>{{ p ? p.fuelPercent + '%' : '—' }}</td>
                <td>{{ p ? (p.recordedAt | date: 'mediumTime') : '—' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="empty">Sin viajes activos.</td></tr>
            }
          </tbody>
        </table>
        <button class="btn" (click)="load()">Actualizar posiciones</button>
      </section>

      <section class="card">
        <h2>Simulador de telemetría (RabbitMQ → Kafka)</h2>
        <form [formGroup]="form" (ngSubmit)="send()" class="form">
          <label>Viaje
            <select formControlName="tripId">
              @for (t of trips(); track t.id) { <option [value]="t.id">{{ t.vehicle.plate }} · {{ t.origin.name }} → {{ t.destination.name }}</option> }
            </select>
          </label>
          <div class="row">
            <label>Latitud <input type="number" step="0.0001" formControlName="latitude" /></label>
            <label>Longitud <input type="number" step="0.0001" formControlName="longitude" /></label>
          </div>
          <div class="row">
            <label>Velocidad km/h <input type="number" formControlName="speedKph" /></label>
            <label>Combustible % <input type="number" formControlName="fuelPercent" /></label>
            <label>Odómetro km <input type="number" formControlName="odometerKm" /></label>
          </div>
          <div class="row">
            <button class="btn primary" [disabled]="form.invalid">Enviar lectura</button>
            <button type="button" class="btn" [disabled]="form.invalid" (click)="toggleAuto()">{{ auto() ? 'Detener ruta' : 'Simular ruta (1 lectura/2 s)' }}</button>
          </div>
        </form>
        <ul class="log">
          @for (l of log(); track $index) { <li>{{ l }}</li> }
        </ul>
      </section>
    </div>
  `,
})
export class FleetComponent implements OnInit, OnDestroy {
  private readonly api = inject(FleetApiService);
  private readonly fb = inject(FormBuilder);
  private timer?: ReturnType<typeof setInterval>;

  protected readonly trips = signal<Trip[]>([]);
  protected readonly positions = signal<Record<string, VehiclePosition>>({});
  protected readonly log = signal<string[]>([]);
  protected readonly auto = signal(false);
  protected readonly form = this.fb.nonNullable.group({
    tripId: ['', Validators.required],
    latitude: [-33.4489, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [-70.6693, [Validators.required, Validators.min(-180), Validators.max(180)]],
    speedKph: [70, [Validators.required, Validators.min(0)]],
    fuelPercent: [60, [Validators.required, Validators.min(0), Validators.max(100)]],
    odometerKm: [1000, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }

  load(): void {
    this.api.trips().subscribe({
      next: (trips) => {
        this.trips.set(trips);
        if (!this.form.value.tripId && trips.length) this.form.patchValue({ tripId: trips[0].id });
        trips.forEach((t) =>
          this.api.vehiclePosition(t.vehicle.id).subscribe({
            next: (p) => this.positions.update((m) => ({ ...m, [t.vehicle.id]: p })),
            error: () => undefined, // vehiculo aun sin posicion
          }),
        );
      },
      error: (e) => this.push(errorMessage(e)),
    });
  }

  send(): void {
    const v = this.form.getRawValue();
    const trip = this.trips().find((t) => t.id === v.tripId);
    if (!trip) return;
    const { tripId, ...reading } = v;
    this.api
      .sendTelemetry({ ...reading, eventId: crypto.randomUUID(), vehicleId: trip.vehicle.id, tripId,
        recordedAt: new Date().toISOString(), source: 'fleetops-angular' })
      .subscribe({
        next: (r) => this.push(`${trip.vehicle.plate} ${v.speedKph} km/h → ${r.status}`),
        error: (e) => this.push(errorMessage(e)),
      });
  }

  toggleAuto(): void {
    if (this.auto()) {
      clearInterval(this.timer);
      this.auto.set(false);
      this.load();
      return;
    }
    this.auto.set(true);
    this.timer = setInterval(() => {
      const v = this.form.getRawValue();
      this.form.patchValue({
        latitude: +(v.latitude + 0.0005).toFixed(4),
        longitude: +(v.longitude + 0.0008).toFixed(4),
        speedKph: Math.round(50 + Math.random() * 60),
        fuelPercent: Math.max(0, +(v.fuelPercent - 1.5).toFixed(1)),
        odometerKm: +(v.odometerKm + 0.1).toFixed(1),
      });
      this.send();
    }, 2000);
  }

  private push(line: string): void {
    this.log.update((l) => [`${new Date().toLocaleTimeString()} ${line}`, ...l].slice(0, 12));
  }
}
