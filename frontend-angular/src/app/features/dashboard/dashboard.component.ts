import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FleetApiService } from '../../core/api/fleet-api.service';
import { errorMessage } from '../../core/api/error-message';
import { Alert, Trip, TripDetail, TripStatus } from '../../core/models/models';

type Filter = 'ALL' | TripStatus;

/** Panel de despacho sobre la API real de servicio-flota: viajes activos, detalle y alertas abiertas. */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, DecimalPipe],
  template: `
    @if (denied) {
      <div class="banner error">Su usuario no tiene el rol FleetOps.Admin.</div>
    }
    <div class="grid-3">
      <section class="card">
        <div class="card-head">
          <div><small class="eyebrow">Despacho</small><h2>Viajes activos</h2></div>
          <span class="badge">{{ trips().length }}</span>
        </div>
        <div class="filters">
          @for (f of filters; track f.value) {
            <button class="chip" [class.on]="filter() === f.value" (click)="filter.set(f.value)">{{ f.label }}</button>
          }
        </div>
        @if (error()) {
          <div class="empty error">{{ error() }}</div>
        }
        @for (t of visible(); track t.id) {
          <button class="list-item" [class.selected]="selected()?.id === t.id" (click)="select(t)">
            <strong>{{ t.vehicle.plate }}</strong> <span class="status s-{{ t.status }}">{{ t.status }}</span>
            <small>{{ t.origin.name }} → {{ t.destination.name }} · {{ t.progress }}%</small>
          </button>
        } @empty {
          @if (!error()) { <div class="empty">No hay viajes para este filtro.</div> }
        }
      </section>

      <section class="card">
        @if (selected(); as t) {
          <small class="eyebrow">Detalle</small>
          <h2>{{ t.id }}</h2>
          <dl class="kv">
            <dt>Conductor</dt><dd>{{ t.driver.name }}</dd>
            <dt>Vehículo</dt><dd>{{ t.vehicle.plate }} ({{ t.vehicle.id }})</dd>
            <dt>Ruta</dt><dd>{{ t.origin.name }} → {{ t.destination.name }}</dd>
            <dt>Estado</dt><dd><span class="status s-{{ t.status }}">{{ t.status }}</span></dd>
            <dt>Avance</dt><dd>{{ t.progress }}% · {{ t.travelledDistanceKm | number: '1.0-1' }} / {{ t.totalDistanceKm | number: '1.0-1' }} km</dd>
            <dt>Salida</dt><dd>{{ t.departureTime | date: 'short' }}</dd>
            <dt>ETA</dt><dd>{{ t.estimatedArrival | date: 'short' }}</dd>
            @if (t.lastPosition; as p) {
              <dt>Última posición</dt><dd>{{ p.latitude | number: '1.4-4' }}, {{ p.longitude | number: '1.4-4' }} · {{ p.speedKph }} km/h · {{ p.fuelPercent }}%</dd>
            }
          </dl>
        } @else {
          <div class="empty">Seleccione un viaje para ver su detalle.</div>
        }
      </section>

      <section class="card">
        <div class="card-head">
          <div><small class="eyebrow">Monitoreo</small><h2>Alertas abiertas</h2></div>
          <span class="badge warn">{{ alerts().length }}</span>
        </div>
        @for (a of alerts(); track a.id) {
          <div class="alert sev-{{ a.severity }}">
            <strong>{{ a.title }}</strong> <small>{{ a.type }} · {{ a.detectedAt | date: 'shortTime' }}</small>
            <p>{{ a.description }}</p>
            <button class="btn small" (click)="resolve(a)">Resolver</button>
          </div>
        } @empty {
          <div class="empty">{{ selected() ? 'Sin alertas abiertas para este viaje.' : 'Seleccione un viaje.' }}</div>
        }
      </section>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(FleetApiService);
  protected readonly denied = inject(ActivatedRoute).snapshot.queryParamMap.has('denied');

  protected readonly filters: { label: string; value: Filter }[] = [
    { label: 'Todos', value: 'ALL' },
    { label: 'En ruta', value: 'IN_TRANSIT' },
    { label: 'Con retraso', value: 'DELAYED' },
    { label: 'Incidente', value: 'INCIDENT' },
  ];
  protected readonly trips = signal<Trip[]>([]);
  protected readonly alerts = signal<Alert[]>([]);
  protected readonly filter = signal<Filter>('ALL');
  protected readonly selected = signal<TripDetail | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly visible = computed(() =>
    this.filter() === 'ALL' ? this.trips() : this.trips().filter((t) => t.status === this.filter()),
  );

  ngOnInit(): void {
    this.api.trips().subscribe({ next: (t) => this.trips.set(t), error: (e) => this.error.set(errorMessage(e)) });
  }

  select(t: Trip): void {
    this.api.trip(t.id).subscribe({
      next: (d) => {
        this.selected.set(d);
        this.alerts.set(d.alerts.filter((a) => a.status === 'OPEN'));
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  resolve(a: Alert): void {
    this.api.resolveAlert(a.id).subscribe({
      next: () => this.alerts.update((list) => list.filter((x) => x.id !== a.id)),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
