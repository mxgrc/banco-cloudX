import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../environments/environment';
import { portalRole } from './policy';

@Injectable({providedIn:'root'})
export class AuthService {
  private readonly keycloak = new Keycloak({url:environment.keycloakUrl,realm:environment.realm,clientId:environment.clientId});
  readonly authenticated = signal(false);
  readonly role = signal<'ADMIN' | 'CLIENTE' | null>(null);
  readonly name = signal('');
  readonly error = signal('');
  readonly refreshing = signal(false);

  async initialize(): Promise<void> {
    this.keycloak.onAuthLogout = () => this.reset();
    this.keycloak.onTokenExpired = () => { void this.token().catch(() => undefined); };
    try {
      await this.keycloak.init({onLoad:'check-sso',checkLoginIframe:false,pkceMethod:'S256',
        responseMode:'query',redirectUri:location.origin+'/'});
      this.sync();
    } catch {
      this.reset();
      this.error.set('No pudimos conectar con el inicio de sesión. Comprueba que Keycloak esté disponible y recarga la página.');
    }
  }
  private sync(): void {
    this.authenticated.set(!!this.keycloak.authenticated);
    this.role.set(portalRole(this.keycloak.tokenParsed));
    this.name.set(this.keycloak.tokenParsed?.['preferred_username'] ?? '');
  }
  private reset(): void { this.authenticated.set(false); this.role.set(null); this.name.set(''); }
  async login(): Promise<void> {
    try { await this.keycloak.login({redirectUri:location.origin+'/'}); }
    catch { this.error.set('No se pudo abrir el inicio de sesión. Recarga la página e inténtalo de nuevo.'); }
  }
  async logout(): Promise<void> {
    try { await this.keycloak.logout({redirectUri:location.origin+'/'}); }
    catch { this.invalidate(); this.error.set('La sesión local terminó. No se pudo confirmar el cierre en Keycloak.'); }
  }
  invalidate(): void {
    this.keycloak.clearToken(); this.reset();
    this.error.set('Tu sesión terminó. Vuelve a ingresar para continuar.');
  }
  async token(force = false): Promise<string> {
    if (!this.keycloak.authenticated) throw new Error('Inicia sesión para continuar.');
    try {
      await this.keycloak.updateToken(force ? -1 : 30);
      this.sync();
      if (!this.keycloak.token) throw new Error('No hay token');
      return this.keycloak.token;
    } catch { this.invalidate(); throw new Error('Tu sesión terminó. Vuelve a ingresar.'); }
  }
  async renew(): Promise<void> {
    this.refreshing.set(true);
    try { await this.token(true); } finally { this.refreshing.set(false); }
  }
}
