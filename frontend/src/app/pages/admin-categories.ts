import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api, Categoria, Page, message } from '../core/api';
@Component({selector:'app-admin-categories',imports:[FormsModule],template:`
  <div class="page-heading"><div><p class="eyebrow">ADMINISTRACIÓN / ORGANIZACIÓN</p><h1>Ordena por <em>categorías.</em></h1><p>Agrupa beneficios para que tus clientes encuentren lo que buscan.</p></div></div>
  @if(error()){<p class="notice error" role="alert">{{error()}}</p>} @if(note()){<p class="notice success" role="status">{{note()}}</p>}
  <form class="inline-create" (ngSubmit)="create()"><label class="sr-only" for="category">Nombre de categoría</label><input id="category" name="category" [(ngModel)]="name" maxlength="100" placeholder="Nueva categoría…" required><button class="button primary" [disabled]="busy()">Agregar categoría</button></form>
  @if(busy()){<div class="loading">Cargando categorías…</div>} @else {<div class="category-grid">@for(c of data()?.contenido;track c.id){<article class="category-card"><span class="category-symbol" aria-hidden="true">▦</span><div><h2>{{c.nombre}}</h2><p>Disponible para organizar tu catálogo.</p></div></article>} @empty{<div class="empty"><h3>Aún no hay categorías.</h3><p>Crea una para organizar tus beneficios.</p></div>}</div>}`})
export class AdminCategories {
 private readonly api=inject(Api);readonly data=signal<Page<Categoria>|null>(null);readonly busy=signal(false);readonly error=signal('');readonly note=signal('');name='';constructor(){void this.load();}
 async load(){this.busy.set(true);this.error.set('');try{this.data.set(await this.api.get<Page<Categoria>>('/categorias?tamano=100'));}catch(e){this.error.set(message(e));}finally{this.busy.set(false);}}
 async create(){this.busy.set(true);this.error.set('');try{await this.api.post('/categorias',{nombre:this.name});this.name='';this.note.set('Categoría creada.');await this.load();}catch(e){this.error.set(message(e));}finally{this.busy.set(false);}}
}
