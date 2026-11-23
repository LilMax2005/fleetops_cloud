import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

/** Layout principal: barra lateral de navegación + cabecera con el usuario de Entra ID. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <div class="brand"><span class="logo">F</span> FleetOps</div>
      <nav>
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Inicio</a>
        <a routerLink="/viajes" routerLinkActive="active">Viajes</a>
        <a routerLink="/flota" routerLinkActive="active">Flota y telemetría</a>
        <a routerLink="/analitica" routerLinkActive="active">Analítica</a>
        @if (auth.isAdmin()) {
          <div class="nav-section">Administración</div>
          <a routerLink="/admin/kafka" routerLinkActive="active">Kafka</a>
          <a routerLink="/admin/rabbitmq" routerLinkActive="active">RabbitMQ</a>
        }
      </nav>
      <div class="zone"><small>Centro de operaciones</small><strong>Zona Chile</strong></div>
    </aside>
    <div class="main">
      <header class="topbar">
        <div></div>
        <div class="user">
          <span class="avatar">{{ auth.initials() }}</span>
          <div>
            <strong>{{ auth.account()?.name }}</strong>
            <small>{{ auth.isAdmin() ? 'Administrador' : 'Operaciones' }}</small>
          </div>
          <button class="btn ghost" (click)="auth.logout()" title="Cerrar sesión">Salir</button>
        </div>
      </header>
      <main class="content"><router-outlet /></main>
    </div>
  `,
})
export class ShellComponent {
  protected readonly auth = inject(AuthService);
}
