import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth';
import { Api, Usuario } from './api';

export const authGuard:CanActivateFn = () => inject(AuthService).authenticated() || inject(Router).createUrlTree(['/']);
export const roleGuard:CanActivateFn = route => {
  const auth=inject(AuthService), router=inject(Router);
  return auth.role() === route.data['role'] || router.createUrlTree(['/']);
};
export const activeGuard:CanActivateFn = async () => {
  const api=inject(Api),router=inject(Router);
  try { return (await api.get<Usuario>('/usuarios/me')).estado==='ACTIVO' || router.createUrlTree(['/estado']); }
  catch { return router.createUrlTree(['/estado']); }
};
