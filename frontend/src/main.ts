import { bootstrapApplication } from '@angular/platform-browser';
import { provideAppInitializer, inject } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { App } from './app/app';
import { routes } from './app/routes';
import { AuthService } from './app/core/auth';
import { bearerInterceptor } from './app/core/interceptor';

bootstrapApplication(App, {providers:[
  provideRouter(routes),
  provideHttpClient(withInterceptors([bearerInterceptor])),
  provideAppInitializer(() => inject(AuthService).initialize())
]}).catch(() => {
  document.body.textContent = 'No se pudo iniciar Banco Cloud. Recarga la página.';
});
