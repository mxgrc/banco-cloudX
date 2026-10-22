import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api,Inscripcion,Page,message } from '../core/api';
@Component({selector:'app-enrollments',imports:[DatePipe,RouterLink],template:`
  <div class="page-heading"><div><p class="eyebrow">ELEGIDOS POR TI</p><h1>Mis <em>inscripciones.</em></h1><p>Tus beneficios inscritos, reunidos en un solo lugar.</p></div><a routerLink="/beneficios" class="button primary">Explorar beneficios ↗</a></div>
  @if(error()){<p class="notice error" role="alert">{{error()}}</p>}
  <div class="section-heading"><h2>{{data()?.total??0}} inscripciones</h2><button class="text-button" (click)="load()" [disabled]="busy()">Actualizar ↻</button></div>
  @if(busy()){<div class="loading" role="status">Consultando tus inscripciones…</div>} @else {
    <div class="enrollment-list">@for(item of data()?.contenido;track item.id){<article class="enrollment"><span class="ticket-icon" aria-hidden="true">◈</span><div><span class="eyebrow">BENEFICIO INSCRITO</span><h3>Tu inscripción está registrada</h3><p>Desde el {{item.fechaInscripcion | date:'dd/MM/yyyy'}} a las {{item.fechaInscripcion | date:'HH:mm'}}</p><details><summary>Ver referencia del beneficio</summary><code>{{item.beneficioId}}</code></details></div><span class="badge active">Inscrito</span></article>}
    @empty{<div class="empty"><h2>Tus próximos beneficios te esperan.</h2><p>Cuando te inscribas en uno, aparecerá aquí.</p><a routerLink="/beneficios" class="text-button">Descubrir beneficios ↗</a></div>}</div>
  }
  <div class="pagination"><button class="button secondary" [disabled]="page===0 || busy()" (click)="move(-1)">← Anterior</button><span>Página {{page+1}}</span><button class="button secondary" [disabled]="(page+1)*20 >= (data()?.total??0) || busy()" (click)="move(1)">Siguiente →</button></div>`})
export class EnrollmentsPage{
  private readonly api=inject(Api);readonly data=signal<Page<Inscripcion>|null>(null);readonly busy=signal(false);readonly error=signal('');page=0;
  constructor(){void this.load();}
  async load(){this.busy.set(true);this.error.set('');try{this.data.set(await this.api.get<Page<Inscripcion>>('/inscripciones/me?pagina='+this.page));}catch(e){this.data.set(null);this.error.set(message(e));}finally{this.busy.set(false);}}
  move(delta:number){this.page+=delta;void this.load();}
}
