import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { from, switchMap, catchError, throwError } from 'rxjs';
import { AuthService } from './auth';
import { isApiRequest } from './policy';
import { environment } from '../../environments/environment';

export const bearerInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url, location.origin, environment.apiBase)) return next(request);
  const auth = inject(AuthService);
  return from(auth.token()).pipe(
    switchMap(token => next(request.clone({setHeaders:{Authorization:'Bearer '+token}}))),
    catchError(error => {
      if (error instanceof HttpErrorResponse && error.status === 401) auth.invalidate();
      // Nunca se repite automáticamente una escritura tras un error.
      return throwError(() => error);
    })
  );
};
