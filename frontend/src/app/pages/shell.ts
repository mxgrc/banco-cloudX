import { Component, inject } from '@angular/core';
import { RouterLink,RouterLinkActive,RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth';
@Component({selector:'app-shell',imports:[RouterLink,RouterLinkActive,RouterOutlet],template:`
  <a class="skip-link" href="#main">Saltar al contenido</a>
  <div class="app-layout">
    <aside class="sidebar">
      <a class="brand" routerLink="/"><span class="brand-mark" aria-hidden="true">b.</span> banco cloud</a>
      <p class="sidebar-caption">{{auth.role()==='ADMIN'?'ADMINISTRACIÓN':'MI PORTAL'}}</p>
      <nav aria-label="Navegación principal">
        @if(auth.role()==='ADMIN') {
          <a routerLink="/admin/beneficios" routerLinkActive="selected"><span aria-hidden="true">◈</span> Beneficios</a>
          <a routerLink="/admin/clientes" routerLinkActive="selected"><span aria-hidden="true">◎</span> Clientes</a>
          <a routerLink="/admin/categorias" routerLinkActive="selected"><span aria-hidden="true">▦</span> Categorías</a>
        } @else {
          <a routerLink="/estado" routerLinkActive="selected"><span aria-hidden="true">◎</span> Mi perfil</a>
          <a routerLink="/beneficios" routerLinkActive="selected"><span aria-hidden="true">◈</span> Explorar beneficios</a>
          <a routerLink="/inscripciones" routerLinkActive="selected"><span aria-hidden="true">▤</span> Mis inscripciones</a>
        }
      </nav>
      <div class="sidebar-bottom"><span class="sidebar-flower" aria-hidden="true">✳</span><p>Un lugar para<br>tus próximos planes.</p><span class="sidebar-small">BANCO CLOUD / BENEFICIOS</span></div>
    </aside>
    <div class="workspace">
      <header class="topbar"><span class="breadcrumb">Banco Cloud <span>/</span> {{auth.role()==='ADMIN'?'Administración':'Beneficios y descuentos'}}</span>
        <div class="account"><span class="avatar" aria-hidden="true">{{auth.name().slice(0,1).toUpperCase()}}</span><span class="account-name">{{auth.name()}}<small>{{auth.role()==='ADMIN'?'Administrador':'Cliente'}}</small></span><button class="text-button" (click)="auth.logout()">Salir ↗</button></div>
      </header>
      @if(!auth.authenticated()) {<div class="notice error" role="alert">{{auth.error()}} <button class="text-button" (click)="auth.login()">Volver a ingresar</button></div>}
      <main id="main" class="page-content"><router-outlet /></main>
      <footer class="app-footer"><span>Banco Cloud · Tu portal de beneficios</span><span>Hecho para tu día a día.</span></footer>
    </div>
  </div>`})
export class Shell {readonly auth=inject(AuthService);}
