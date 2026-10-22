import { Component, inject, effect } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth';
@Component({selector:'app-landing',template:`
  <div class="landing">
    <header class="landing-nav"><a class="brand" href="/"><span class="brand-mark" aria-hidden="true">b.</span> banco cloud<span class="brand-dot">®</span></a><span class="nav-caption">TU PORTAL DE BENEFICIOS</span></header>
    <main class="landing-main">
      <section class="landing-copy"><p class="eyebrow"><span class="little-dot"></span> MÁS PARA TU DÍA A DÍA</p>
        <h1>Lo bueno de ser<br>parte está <em>aquí.</em></h1>
        <p class="landing-description">Descubre tus beneficios, encuentra nuevas oportunidades y disfruta tus descuentos. Todo en un solo lugar.</p>
        @if (auth.error()) {<p class="notice error" role="alert">{{auth.error()}}</p>}
        @if (auth.authenticated() && !auth.role()) {
          <p class="notice error" role="alert">Tu cuenta no tiene un perfil habilitado para este portal. Contacta al administrador.</p>
          <button class="button primary" (click)="auth.logout()">Cerrar sesión</button>
        } @else {<button class="button primary large" (click)="auth.login()">Ingresar a mi cuenta <span aria-hidden="true">↗</span></button>}
        <p class="secure-caption"><span aria-hidden="true">◇</span> Acceso seguro con tu cuenta Banco Cloud</p>
      </section>
      <div class="landing-art" aria-hidden="true">
        <div class="orbit orbit-one"></div><div class="orbit orbit-two"></div>
        <div class="art-label">PEQUEÑOS PLANES.<br>GRANDES BENEFICIOS.</div>
        <div class="membership"><div class="card-brand">banco cloud <span>↗</span></div><div class="card-chip"></div><p>Más posibilidades.<br>Todos los días.</p><div class="card-bottom"><span>BENEFICIOS / CLOUD</span><span>✳</span></div></div>
        <span class="art-sticker">Hecho<br>para ti.</span>
        <span class="art-coordinate">01 — TU PRÓXIMO PLAN EMPIEZA AQUÍ</span>
      </div>
    </main>
    <footer class="landing-footer"><span>Banco Cloud · Gestión de beneficios</span><span>Tu acceso. Tus beneficios. Tu momento.</span></footer>
  </div>`})
export class Landing {
  readonly auth=inject(AuthService); private readonly router=inject(Router);
  constructor(){effect(()=>{if(this.auth.authenticated() && this.auth.role())void this.router.navigateByUrl(this.auth.role()==='ADMIN'?'/admin/beneficios':'/estado');});}
}
