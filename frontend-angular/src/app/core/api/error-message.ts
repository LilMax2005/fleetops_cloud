import { HttpErrorResponse } from '@angular/common/http';

/** Mensaje legible para errores HTTP (ProblemDetail de Spring o respuesta 401/403 del filtro JWT). */
export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'No se pudo contactar la API (red o CORS).';
    if (err.status === 401) return 'Sesión expirada o token inválido (401).';
    if (err.status === 403) return 'No tiene permisos para esta operación (403).';
    return err.error?.detail ?? err.error?.error ?? `Error ${err.status} en ${err.url}`;
  }
  return String(err);
}
