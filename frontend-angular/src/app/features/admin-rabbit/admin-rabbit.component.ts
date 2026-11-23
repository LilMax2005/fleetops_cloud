import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FleetApiService } from '../../core/api/fleet-api.service';
import { RabbitAdminApiService } from '../../core/api/rabbit-admin-api.service';
import { errorMessage } from '../../core/api/error-message';
import { Row } from '../../core/models/models';

/** Consola del microservicio admin-rabbitmq: colas, exchanges y bindings (+ prueba de publicación de logs). */
@Component({
  selector: 'app-admin-rabbit',
  imports: [ReactiveFormsModule],
  template: `
    @if (message()) { <div class="banner">{{ message() }}</div> }
    <section class="card">
      <div class="card-head"><h2>Nodos del clúster</h2><button class="btn small" (click)="load()">Actualizar</button></div>
      <div class="filters">
        @for (n of nodes(); track n['name']) { <span class="chip" [class.on]="n['running']">{{ n['name'] }} · {{ n['running'] ? 'running' : 'down' }}</span> }
      </div>
    </section>
    <div class="grid-2">
      <section class="card">
        <h2>Colas</h2>
        <table>
          <thead><tr><th>Nombre</th><th>Tipo</th><th>Mensajes</th><th>Consumidores</th><th>DLX</th><th></th></tr></thead>
          <tbody>
            @for (q of queues(); track q['name']) {
              <tr>
                <td>{{ q['name'] }}</td><td>{{ q['type'] }}</td><td>{{ q['messages'] ?? 0 }}</td><td>{{ q['consumers'] ?? 0 }}</td>
                <td>{{ $any(q['arguments'])?.['x-dead-letter-routing-key'] ?? '' }}</td>
                <td><button class="btn small danger" (click)="deleteQueue(q['name'] + '')">Eliminar</button></td>
              </tr>
            }
          </tbody>
        </table>
        <form [formGroup]="queueForm" (ngSubmit)="createQueue()" class="form inline">
          <input formControlName="name" placeholder="fleetops.nueva.queue" />
          <input formControlName="messageTtlMs" type="number" placeholder="TTL ms (opcional)" />
          <label class="check"><input type="checkbox" formControlName="withDlq" /> con DLQ</label>
          <label class="check"><input type="checkbox" formControlName="quorum" /> quorum</label>
          <button class="btn primary" [disabled]="queueForm.invalid">Crear cola</button>
        </form>
      </section>
      <section class="card">
        <h2>Exchanges</h2>
        <table>
          <thead><tr><th>Nombre</th><th>Tipo</th><th>Durable</th><th></th></tr></thead>
          <tbody>
            @for (x of exchanges(); track x['name']) {
              <tr>
                <td>{{ x['name'] || '(default)' }}</td><td>{{ x['type'] }}</td><td>{{ x['durable'] }}</td>
                <td>@if (x['name'] && !$any(x['name']).startsWith('amq.')) {
                  <button class="btn small danger" (click)="deleteExchange(x['name'] + '')">Eliminar</button> }</td>
              </tr>
            }
          </tbody>
        </table>
        <form [formGroup]="exchangeForm" (ngSubmit)="createExchange()" class="form inline">
          <input formControlName="name" placeholder="fleetops.nuevo.exchange" />
          <select formControlName="type"><option>direct</option><option>topic</option><option>fanout</option><option>headers</option></select>
          <button class="btn primary" [disabled]="exchangeForm.invalid">Crear exchange</button>
        </form>
      </section>
    </div>
    <div class="grid-2">
      <section class="card">
        <h2>Bindings</h2>
        <table>
          <thead><tr><th>Exchange</th><th>Routing key</th><th>Cola</th><th></th></tr></thead>
          <tbody>
            @for (b of bindings(); track $index) {
              @if (b['source']) {
                <tr><td>{{ b['source'] }}</td><td>{{ b['routing_key'] }}</td><td>{{ b['destination'] }}</td>
                  <td><button class="btn small danger" (click)="deleteBinding(b)">Eliminar</button></td></tr>
              }
            }
          </tbody>
        </table>
        <form [formGroup]="bindingForm" (ngSubmit)="createBinding()" class="form inline">
          <input formControlName="exchange" placeholder="exchange" />
          <input formControlName="routingKey" placeholder="routing key" />
          <input formControlName="queue" placeholder="cola" />
          <button class="btn primary" [disabled]="bindingForm.invalid">Crear binding</button>
        </form>
      </section>
      <section class="card">
        <h2>Probar enrutamiento (EV2)</h2>
        <p>Publica en <code>fleetops.logs.direct</code>. Incluya <code>[FAIL]</code> en el texto para forzar el envío a la DLQ.</p>
        <form [formGroup]="logForm" (ngSubmit)="publishLog()" class="form inline">
          <select formControlName="level"><option>INFO</option><option>WARNING</option><option>ERROR</option><option>SECURITY</option></select>
          <input formControlName="message" placeholder="Mensaje" />
          <button class="btn primary" [disabled]="logForm.invalid">Publicar</button>
        </form>
      </section>
    </div>
  `,
})
export class AdminRabbitComponent implements OnInit {
  private readonly api = inject(RabbitAdminApiService);
  private readonly fleet = inject(FleetApiService);
  private readonly fb = inject(FormBuilder);

  protected readonly nodes = signal<Row[]>([]);
  protected readonly queues = signal<Row[]>([]);
  protected readonly exchanges = signal<Row[]>([]);
  protected readonly bindings = signal<Row[]>([]);
  protected readonly message = signal<string | null>(null);
  private readonly name = [Validators.required, Validators.pattern(/^[a-zA-Z0-9._-]+$/)];
  protected readonly queueForm = this.fb.group({
    name: ['', this.name],
    messageTtlMs: [null as number | null],
    withDlq: [true],
    quorum: [false],
  });
  protected readonly exchangeForm = this.fb.nonNullable.group({ name: ['', this.name], type: ['direct'] });
  protected readonly bindingForm = this.fb.nonNullable.group({
    exchange: ['', Validators.required],
    routingKey: [''],
    queue: ['', Validators.required],
  });
  protected readonly logForm = this.fb.nonNullable.group({ level: ['INFO'], message: ['', Validators.required] });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.nodes().subscribe({ next: (r) => this.nodes.set(r), error: (e) => this.fail(e) });
    this.api.queues().subscribe({ next: (r) => this.queues.set(r), error: (e) => this.fail(e) });
    this.api.exchanges().subscribe({ next: (r) => this.exchanges.set(r) });
    this.api.bindings().subscribe({ next: (r) => this.bindings.set(r) });
  }

  createQueue(): void {
    const f = this.queueForm.getRawValue();
    this.api
      .createQueue({ name: f.name!, durable: true, withDlq: !!f.withDlq, quorum: !!f.quorum, messageTtlMs: f.messageTtlMs ?? undefined })
      .subscribe({ next: (r) => this.ok(`Cola creada ${JSON.stringify(r)}`), error: (e) => this.fail(e) });
  }

  deleteQueue(name: string): void {
    if (confirm(`¿Eliminar la cola ${name}?`)) this.api.deleteQueue(name).subscribe({ next: () => this.ok(`Cola ${name} eliminada`), error: (e) => this.fail(e) });
  }

  createExchange(): void {
    this.api.createExchange({ ...this.exchangeForm.getRawValue(), durable: true }).subscribe({
      next: () => this.ok('Exchange creado'),
      error: (e) => this.fail(e),
    });
  }

  deleteExchange(name: string): void {
    if (confirm(`¿Eliminar el exchange ${name}?`)) this.api.deleteExchange(name).subscribe({ next: () => this.ok(`Exchange ${name} eliminado`), error: (e) => this.fail(e) });
  }

  createBinding(): void {
    this.api.createBinding(this.bindingForm.getRawValue()).subscribe({ next: () => this.ok('Binding creado'), error: (e) => this.fail(e) });
  }

  deleteBinding(b: Row): void {
    this.api
      .deleteBinding({ exchange: String(b['source']), queue: String(b['destination']), routingKey: String(b['routing_key'] ?? '') })
      .subscribe({ next: () => this.ok('Binding eliminado'), error: (e) => this.fail(e) });
  }

  publishLog(): void {
    this.fleet.publishLog({ ...this.logForm.getRawValue(), simulateFailure: false }).subscribe({
      next: (r) => this.ok(`${r} (exchange fleetops.logs.direct)`),
      error: (e) => this.fail(e),
    });
  }

  private ok(msg: string): void {
    this.message.set(msg);
    this.load();
  }

  private fail(e: unknown): void {
    this.message.set(errorMessage(e));
  }
}
