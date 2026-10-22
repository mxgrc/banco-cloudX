import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Page<T> {contenido:T[];pagina:number;tamano:number;total:number}
export interface Usuario {usuarioSub:string;estado:'PENDIENTE'|'ACTIVO'|'INACTIVO';fechaRegistro:string;fechaCambioEstado:string|null}
export type EstadoUsuario = Usuario['estado'];
export interface Beneficio {id:string;nombre:string;descripcion:string;descuento:number;disponible:boolean;categoriaId:string|null}
export interface Categoria {id:string;nombre:string}
export interface Inscripcion {id:string;beneficioId:string;fechaInscripcion:string}
@Injectable({providedIn:'root'})
export class Api {
  private readonly http=inject(HttpClient);
  get<T>(path:string):Promise<T> {return firstValueFrom(this.http.get<T>(environment.apiBase+path));}
  post<T>(path:string,body:unknown={}):Promise<T> {return firstValueFrom(this.http.post<T>(environment.apiBase+path,body));}
  patch<T>(path:string,body:unknown={}):Promise<T> {return firstValueFrom(this.http.patch<T>(environment.apiBase+path,body));}
  put<T>(path:string,body:unknown):Promise<T> {return firstValueFrom(this.http.put<T>(environment.apiBase+path,body));}
}
export function message(error:unknown):string {
  if(error instanceof HttpErrorResponse) {
    switch(error.status) {
      case 401:return 'Tu sesión terminó. Vuelve a ingresar.';
      case 403:return 'No tienes acceso a esta operación. Revisa el estado de tu perfil.';
      case 404:return 'No encontramos el registro solicitado.';
      case 409:return 'La operación no se pudo completar: el registro ya existe o su estado cambió. Actualiza la lista.';
      case 400:return 'Revisa los campos del formulario e inténtalo nuevamente.';
      case 0:case 502:case 503:return 'El servicio no está disponible. Inténtalo en unos momentos.';
    }
  }
  return error instanceof Error ? error.message : 'No se pudo completar la operación.';
}
