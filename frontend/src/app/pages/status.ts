import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Api,Usuario,message } from '../core/api';
import { AuthService } from '../core/auth';
@Component({selector:'app-status',imports:[RouterLink,DatePipe],template:`
  <div class="page-heading"><div><p class="eyebrow">MI CUENTA</p><h1>Hola, {{auth.name()}}<span class="green-dot">.</span></h1><p>Este es el estado de tu acceso a los beneficios.</p></div><button class="button secondary" (click)="load()" [disabled]="busy()">Actualizar estado ↻</button></div>
  @if(error()) {<p class="notice error" role="alert">{{error()}}</p>}
  @if(busy()){<div class="loading" role="status">Consultando tu perfil…</div>} @else {
  <section class="status-card"><div class="status-art" aria-hidden="true"><span>{{profile()?.estado==='ACTIVO'?'✓':'◎'}}</span><p>TU CUENTA<br>BANCO CLOUD</p></div><div class="status-copy">
    @switch(profile()?.estado){
      @case('ACTIVO'){<span class="badge active">Acceso activo</span><h2>Todo listo para<br>tu próximo beneficio.</h2><p>Tu cuenta está aprobada. Explora los descuentos disponibles e inscríbete en los que te interesen.</p><a class="button primary" routerLink="/beneficios">Explorar beneficios ↗</a>}
      @case('PENDIENTE'){<span class="badge pending">En revisión</span><h2>Ya recibimos<br>tu solicitud.</h2><p>Un administrador debe aprobar tu acceso. Puedes volver aquí y actualizar el estado de tu solicitud.</p>}
      @case('INACTIVO'){<span class="badge inactive">Acceso desactivado</span><h2>Tu acceso está<br>en pausa.</h2><p>Contacta al administrador para solicitar la reactivación. Tus inscripciones se conservan.</p>}
      @default{<span class="badge">Primer acceso</span><h2>Empecemos<br>por tu perfil.</h2><p>Solicita acceso al portal. Cuando un administrador lo apruebe, podrás inscribirte en beneficios.</p><button class="button primary" (click)="register()" [disabled]="!!error()">Solicitar acceso ↗</button>}
    }
    @if(profile()){<p class="fine-print">Solicitud registrada el {{profile()!.fechaRegistro | date:'dd/MM/yyyy'}}</p>}
  </div></section>}
  <section class="info-strip"><span aria-hidden="true">◇</span><div><h3>Tu sesión, bajo control</h3><p>Puedes renovar tu sesión sin volver a introducir tu contraseña.</p></div><button class="button secondary" (click)="renew()" [disabled]="auth.refreshing()">{{auth.refreshing()?'Renovando…':'Renovar sesión'}}</button></section>
  @if(note()){<p class="notice success" role="status">{{note()}}</p>}`})
export class StatusPage {
  private readonly api=inject(Api); readonly auth=inject(AuthService);
  readonly profile=signal<Usuario|null>(null);readonly busy=signal(false);readonly error=signal('');readonly note=signal('');
  constructor(){void this.load();}
  async load(){this.busy.set(true);this.error.set('');try{this.profile.set(await this.api.get<Usuario>('/usuarios/me'));}catch(e){if(e instanceof HttpErrorResponse && e.status===404)this.profile.set(null);else this.error.set(message(e));}finally{this.busy.set(false);}}
  async register(){this.busy.set(true);this.error.set('');try{this.profile.set(await this.api.post<Usuario>('/usuarios/me'));}catch(e){this.error.set(message(e));}finally{this.busy.set(false);}}
  async renew(){this.note.set('');try{await this.auth.renew();this.note.set('Sesión renovada correctamente.');}catch(e){this.error.set(message(e));}}
}
