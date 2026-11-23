import { Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  template: `
    <div class="login">
      <div class="card login-card">
        <span class="logo big">F</span>
        <h1>FleetOps</h1>
        <p>Plataforma de gestión de flotas y operaciones logísticas</p>
        <button class="btn primary" (click)="auth.login()" [disabled]="!auth.ready()">
          Iniciar sesión con Microsoft
        </button>
        <small>Autenticación con Microsoft Entra ID (OAuth 2.0 / OpenID Connect + PKCE)</small>
      </div>
    </div>
  `,
})
export class LoginComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    if (!this.auth.ready()) {
      this.auth.init();
    }
    effect(() => {
      if (this.auth.isLoggedIn()) this.router.navigateByUrl('/');
    });
  }
}
