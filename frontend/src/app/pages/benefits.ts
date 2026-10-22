import { Component, inject, signal } from '@angular/core';
import { Api,Beneficio,Page,message } from '../core/api';
@Component({selector:'app-benefits',template:`
  <div class="page-heading"><div><p class="eyebrow">DESCUBRE MÁS</p><h1>Tu próximo <em>beneficio.</em></h1><p>Elige un descuento y hazlo parte de tus planes.</p></div><span class="count-label">{{data()?.total ?? 0}} disponibles</span></div>
  <section class="benefit-banner"><div><span class="eyebrow">POR SER PARTE</span><h2>Más oportunidades.<br>Más para disfrutar.</h2></div><span aria-hidden="true">✳</span><p>Explora. Elige.<br>Inscríbete.</p></section>
  @if(error()){<p class="notice error" role="alert">{{error()}}</p>} @if(note()){<p class="notice success" role="status">{{note()}}</p>}
  <div class="section-heading"><h2>Beneficios disponibles</h2><button class="text-button" (click)="load()" [disabled]="busy()">Actualizar ↻</button></div>
  @if(busy()){<div class="loading" role="status">Buscando beneficios…</div>}
  @else {<div class="benefit-grid">@for(b of data()?.contenido;track b.id;let i=$index){
    <article class="benefit-card"><div class="benefit-art" [class.alt]="i%2===1"><span class="art-mini">BANCO CLOUD</span><div class="discount">{{b.descuento}}<span>%</span></div><span class="art-shape" aria-hidden="true">✳</span><span class="discount-label">DE DESCUENTO</span></div>
      <div class="benefit-body"><span class="badge active">Disponible</span><h3>{{b.nombre}}</h3><p>{{b.descripcion}}</p><button class="button secondary full" (click)="selected.set(b)" [disabled]="working()">Inscribirme <span aria-hidden="true">↗</span></button></div>
    </article>
  } @empty {<div class="empty"><h3>Aún no hay beneficios disponibles.</h3><p>Vuelve pronto para descubrir nuevas oportunidades.</p></div>}</div>}
  <div class="pagination"><button class="button secondary" [disabled]="page===0 || busy()" (click)="move(-1)">← Anterior</button><span>Página {{page+1}}</span><button class="button secondary" [disabled]="(page+1)*20 >= (data()?.total??0) || busy()" (click)="move(1)">Siguiente →</button></div>
  @if(selected()){<div class="confirm-panel" role="region" aria-label="Confirmar inscripción"><div><span class="eyebrow">CONFIRMA TU ELECCIÓN</span><h2>{{selected()!.nombre}}</h2><p>Se agregará este beneficio a tus inscripciones.</p></div><button class="button secondary" (click)="selected.set(null)" [disabled]="working()">Cancelar</button><button class="button primary" (click)="enroll()" [disabled]="working()">{{working()?'Inscribiendo…':'Confirmar inscripción'}}</button></div>}`})
export class BenefitsPage{
  private readonly api=inject(Api);readonly data=signal<Page<Beneficio>|null>(null);readonly busy=signal(false);readonly working=signal(false);readonly error=signal('');readonly note=signal('');readonly selected=signal<Beneficio|null>(null);page=0;
  constructor(){void this.load();}
  async load(){this.busy.set(true);this.error.set('');try{this.data.set(await this.api.get<Page<Beneficio>>('/beneficios/disponibles?pagina='+this.page));}catch(e){this.data.set(null);this.error.set(message(e));}finally{this.busy.set(false);}}
  move(delta:number){this.page+=delta;void this.load();}
  async enroll(){const b=this.selected();if(!b)return;this.working.set(true);this.note.set('');this.error.set('');try{await this.api.post('/inscripciones',{beneficioId:b.id});this.note.set('Ya estás inscrito en '+b.nombre+'. Lo encontrarás en Mis inscripciones.');this.selected.set(null);}catch(e){this.error.set(message(e));this.selected.set(null);}finally{this.working.set(false);}}
}
