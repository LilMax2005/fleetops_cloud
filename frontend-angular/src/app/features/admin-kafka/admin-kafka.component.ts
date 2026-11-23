import { KeyValuePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { KafkaAdminApiService } from '../../core/api/kafka-admin-api.service';
import { errorMessage } from '../../core/api/error-message';
import { ConsumerGroupDetail, Row, TopicDescription, TopicSummary } from '../../core/models/models';

/** Consola del microservicio admin-kafka: tópicos, configuraciones y consumer groups. */
@Component({
  selector: 'app-admin-kafka',
  imports: [ReactiveFormsModule, FormsModule, KeyValuePipe],
  template: `
    @if (message()) { <div class="banner">{{ message() }}</div> }
    <div class="grid-2">
      <section class="card">
        <div class="card-head"><h2>Tópicos</h2><button class="btn small" (click)="load()">Actualizar</button></div>
        @if (cluster(); as c) { <small>Clúster {{ c['clusterId'] }} · controller {{ c['controller'] }} · brokers {{ $any(c['brokers']).length }}</small> }
        <table>
          <thead><tr><th>Nombre</th><th>Particiones</th><th>Réplicas</th><th></th></tr></thead>
          <tbody>
            @for (t of topics(); track t.name) {
              <tr [class.selected]="selected()?.name === t.name">
                <td><a href="" (click)="$event.preventDefault(); open(t.name)">{{ t.name }}</a></td>
                <td>{{ t.partitions }}</td><td>{{ t.replicationFactor }}</td>
                <td><button class="btn small danger" (click)="remove(t.name)">Eliminar</button></td>
              </tr>
            }
          </tbody>
        </table>
        <h3>Crear tópico</h3>
        <form [formGroup]="createForm" (ngSubmit)="create()" class="form">
          <div class="row">
            <label>Nombre <input formControlName="name" placeholder="fleetops.nuevo.topico" /></label>
            <label>Particiones <input type="number" formControlName="partitions" /></label>
            <label>Réplicas <input type="number" formControlName="replicationFactor" /></label>
          </div>
          <div class="row">
            <label>retention.ms <input formControlName="retention" /></label>
            <label>cleanup.policy
              <select formControlName="cleanup"><option>delete</option><option>compact</option><option>compact,delete</option></select>
            </label>
            <label>min.insync.replicas <input type="number" formControlName="minIsr" /></label>
          </div>
          <button class="btn primary" [disabled]="createForm.invalid">Crear</button>
        </form>
      </section>

      <section class="card">
        @if (selected(); as t) {
          <h2>{{ t.name }}</h2>
          <small>{{ t.partitions }} particiones · RF {{ t.replicationFactor }}</small>
          <table>
            <thead><tr><th>Partición</th><th>Líder</th><th>Réplicas</th><th>ISR</th></tr></thead>
            <tbody>
              @for (p of t.partitionInfo; track p.partition) {
                <tr><td>{{ p.partition }}</td><td>{{ p.leader }}</td><td>{{ p.replicas.join(', ') }}</td><td>{{ p.isr.join(', ') }}</td></tr>
              }
            </tbody>
          </table>
          <h3>Configuraciones</h3>
          <table>
            @for (c of t.configs | keyvalue; track c.key) {
              <tr><td>{{ c.key }}</td><td>{{ c.value }}</td></tr>
            }
          </table>
          <div class="form inline">
            <select [(ngModel)]="cfgKey">
              @for (k of editableKeys; track k) { <option [value]="k">{{ k }}</option> }
            </select>
            <input [(ngModel)]="cfgValue" placeholder="nuevo valor" />
            <button class="btn" (click)="setConfig(t.name)">Aplicar</button>
            <button class="btn ghost" (click)="resetConfig(t.name)">Restablecer</button>
          </div>
          <div class="form inline">
            <input type="number" [(ngModel)]="newPartitions" placeholder="total particiones" />
            <button class="btn" (click)="addPartitions(t.name)">Aumentar particiones</button>
          </div>
        } @else {
          <div class="empty">Seleccione un tópico para ver particiones y configuración.</div>
        }
      </section>
    </div>

    <section class="card">
      <div class="card-head"><h2>Consumer groups</h2></div>
      <div class="filters">
        @for (g of groups(); track g.groupId) {
          <button class="chip" [class.on]="group()?.groupId === g.groupId" (click)="openGroup(g.groupId)">{{ g.groupId }} · {{ g.state }}</button>
        }
      </div>
      @if (group(); as g) {
        <p>Estado <strong>{{ g.state }}</strong> · assignor {{ g.assignor || '—' }} · coordinador {{ g.coordinator }} · lag total <strong>{{ g.totalLag }}</strong></p>
        <div class="grid-2">
          <table>
            <thead><tr><th>Miembro</th><th>Host</th><th>Particiones asignadas</th></tr></thead>
            <tbody>
              @for (m of g.members; track m.memberId) { <tr><td>{{ m.clientId }}</td><td>{{ m.host }}</td><td>{{ m.assignment.join(', ') }}</td></tr> }
              @empty { <tr><td colspan="3" class="empty">Sin miembros activos.</td></tr> }
            </tbody>
          </table>
          <table>
            <thead><tr><th>Tópico</th><th>P</th><th>Commit</th><th>Fin</th><th>Lag</th></tr></thead>
            <tbody>
              @for (o of g.offsets; track o.topic + o.partition) {
                <tr><td>{{ o.topic }}</td><td>{{ o.partition }}</td><td>{{ o.committedOffset }}</td><td>{{ o.endOffset }}</td><td>{{ o.lag }}</td></tr>
              }
            </tbody>
          </table>
        </div>
        <div class="form inline">
          <select [(ngModel)]="resetTopic">
            @for (t of topics(); track t.name) { <option [value]="t.name">{{ t.name }}</option> }
          </select>
          <button class="btn" (click)="reset(g.groupId, 'earliest')">Reiniciar a earliest</button>
          <button class="btn" (click)="reset(g.groupId, 'latest')">Saltar a latest</button>
          <small>El grupo debe estar sin consumidores activos.</small>
        </div>
      }
    </section>
  `,
})
export class AdminKafkaComponent implements OnInit {
  private readonly api = inject(KafkaAdminApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly editableKeys = ['retention.ms', 'cleanup.policy', 'min.insync.replicas', 'segment.ms', 'compression.type', 'max.message.bytes', 'retention.bytes'];
  protected readonly topics = signal<TopicSummary[]>([]);
  protected readonly cluster = signal<Row | null>(null);
  protected readonly selected = signal<TopicDescription | null>(null);
  protected readonly groups = signal<{ groupId: string; state: string }[]>([]);
  protected readonly group = signal<ConsumerGroupDetail | null>(null);
  protected readonly message = signal<string | null>(null);
  protected cfgKey = 'retention.ms';
  protected cfgValue = '';
  protected newPartitions?: number;
  protected resetTopic = '';
  protected readonly createForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]+$/)]],
    partitions: [3, [Validators.required, Validators.min(1)]],
    replicationFactor: [3, [Validators.required, Validators.min(1), Validators.max(3)]],
    retention: ['604800000'],
    cleanup: ['delete'],
    minIsr: [2],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.cluster().subscribe({ next: (c) => this.cluster.set(c), error: (e) => this.fail(e) });
    this.api.topics().subscribe({ next: (t) => this.topics.set(t), error: (e) => this.fail(e) });
    this.api.groups().subscribe({ next: (g) => this.groups.set(g), error: (e) => this.fail(e) });
  }

  open(name: string): void {
    this.api.topic(name).subscribe({ next: (t) => this.selected.set(t), error: (e) => this.fail(e) });
  }

  create(): void {
    const f = this.createForm.getRawValue();
    this.api
      .createTopic({
        name: f.name,
        partitions: f.partitions,
        replicationFactor: f.replicationFactor,
        configs: { 'retention.ms': f.retention, 'cleanup.policy': f.cleanup, 'min.insync.replicas': String(f.minIsr) },
      })
      .subscribe({ next: (t) => this.done(`Tópico ${t.name} creado`, t), error: (e) => this.fail(e) });
  }

  remove(name: string): void {
    if (!confirm(`¿Eliminar el tópico ${name}? Se pierden sus mensajes.`)) return;
    this.api.deleteTopic(name).subscribe({ next: () => this.done(`Tópico ${name} eliminado`, null), error: (e) => this.fail(e) });
  }

  setConfig(name: string): void {
    this.api.updateConfigs(name, { [this.cfgKey]: this.cfgValue }).subscribe({
      next: (t) => this.done(`${this.cfgKey}=${this.cfgValue} aplicado`, t),
      error: (e) => this.fail(e),
    });
  }

  resetConfig(name: string): void {
    this.api.updateConfigs(name, {}, [this.cfgKey]).subscribe({
      next: (t) => this.done(`${this.cfgKey} restablecido al valor del broker`, t),
      error: (e) => this.fail(e),
    });
  }

  addPartitions(name: string): void {
    if (!this.newPartitions) return;
    this.api.increasePartitions(name, this.newPartitions).subscribe({
      next: (t) => this.done(`${name} ahora tiene ${t.partitions} particiones`, t),
      error: (e) => this.fail(e),
    });
  }

  openGroup(id: string): void {
    this.api.group(id).subscribe({ next: (g) => this.group.set(g), error: (e) => this.fail(e) });
  }

  reset(id: string, strategy: 'earliest' | 'latest'): void {
    if (!this.resetTopic) return;
    this.api.resetOffsets(id, this.resetTopic, strategy).subscribe({
      next: (g) => {
        this.group.set(g);
        this.message.set(`Offsets de ${id} reiniciados a ${strategy}`);
      },
      error: (e) => this.fail(e),
    });
  }

  private done(msg: string, topic: TopicDescription | null): void {
    this.message.set(msg);
    this.selected.set(topic);
    this.api.topics().subscribe((t) => this.topics.set(t));
  }

  private fail(e: unknown): void {
    this.message.set(errorMessage(e));
  }
}
