import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent) },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'viajes', loadComponent: () => import('./features/trips/trips.component').then((m) => m.TripsComponent) },
      { path: 'flota', loadComponent: () => import('./features/fleet/fleet.component').then((m) => m.FleetComponent) },
      { path: 'analitica', loadComponent: () => import('./features/analytics/analytics.component').then((m) => m.AnalyticsComponent) },
      {
        path: 'admin/kafka',
        data: { admin: true },
        loadComponent: () => import('./features/admin-kafka/admin-kafka.component').then((m) => m.AdminKafkaComponent),
      },
      {
        path: 'admin/rabbitmq',
        data: { admin: true },
        loadComponent: () => import('./features/admin-rabbit/admin-rabbit.component').then((m) => m.AdminRabbitComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
