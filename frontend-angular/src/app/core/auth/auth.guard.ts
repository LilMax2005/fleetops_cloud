import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Sin sesión -> /login. Rutas admin requieren el app role FleetOps.Admin. */
export const authGuard: CanActivateFn = async (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.ready()) {
    await auth.init();
  }
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }
  if (route.data?.['admin'] && !auth.isAdmin()) {
    return router.createUrlTree(['/'], { queryParams: { denied: 1 } });
  }
  return true;
};
