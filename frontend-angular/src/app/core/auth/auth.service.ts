import { Injectable, computed, inject, signal } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { AccountInfo, EventType, InteractionStatus } from '@azure/msal-browser';
import { filter, firstValueFrom } from 'rxjs';
import { LOGIN_SCOPES } from './msal.config';

/** Estado de sesión expuesto como signals para los componentes. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);

  readonly account = signal<AccountInfo | null>(null);
  readonly ready = signal(false);
  readonly isLoggedIn = computed(() => this.account() !== null);
  readonly roles = computed(() => (this.account()?.idTokenClaims?.['roles'] as string[] | undefined) ?? []);
  readonly isAdmin = computed(() => this.roles().includes('FleetOps.Admin'));
  readonly initials = computed(() =>
    (this.account()?.name ?? '?')
      .split(' ')
      .slice(0, 2)
      .map((p) => p[0])
      .join('')
      .toUpperCase(),
  );

  constructor() {
    this.broadcast.msalSubject$
      .pipe(filter((e) => e.eventType === EventType.LOGIN_SUCCESS || e.eventType === EventType.LOGOUT_SUCCESS))
      .subscribe(() => this.refresh());
  }

  /** Procesa la respuesta del redirect de Entra ID y espera a que no haya interacción en curso. */
  async init(): Promise<void> {
    await firstValueFrom(this.msal.initialize());
    await firstValueFrom(this.msal.handleRedirectObservable());
    await firstValueFrom(this.broadcast.inProgress$.pipe(filter((s) => s === InteractionStatus.None)));
    this.refresh();
    this.ready.set(true);
  }

  login(): void {
    this.msal.loginRedirect({ scopes: LOGIN_SCOPES });
  }

  logout(): void {
    this.msal.logoutRedirect({ account: this.account() ?? undefined });
  }

  private refresh(): void {
    const instance = this.msal.instance;
    let active = instance.getActiveAccount();
    if (!active && instance.getAllAccounts().length > 0) {
      active = instance.getAllAccounts()[0];
      instance.setActiveAccount(active);
    }
    this.account.set(active);
  }
}
