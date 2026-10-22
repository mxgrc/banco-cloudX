// Uso: node browser-smoke.mjs <ruta-al-paquete-playwright> [--expect-blocked]
// Chromium aislado, sin contraseñas ni acceso al perfil personal de Chrome.
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';
const require = createRequire(import.meta.url);
const { chromium } = require(process.argv[2] || 'playwright');
const browser = await chromium.launch({channel:'chrome',headless:true});
try {
  const context = await browser.newContext();
  const page = await context.newPage();
  let postedOrigin;
  page.on('request', request => {
    if (request.url() === 'http://localhost:4200/login' && request.method() === 'POST') postedOrigin = request.headers().origin;
  });
  await page.goto('http://localhost:4200/');
  await page.getByRole('button',{name:'Iniciar sesión en Keycloak',exact:true}).click();
  if (process.argv.includes('--expect-blocked')) {
    await page.getByText('Origen de formulario no permitido',{exact:true}).waitFor();
    assert.equal(postedOrigin,'null');
    console.log('REPRODUCIDO: formulario real de Chrome envía Origin: null y recibe el rechazo.');
  } else {
    await page.locator('input[type="password"]').waitFor({timeout:20000});
    assert.equal(postedOrigin,'http://localhost:4200');
    assert.equal(new URL(page.url()).origin,'http://localhost:8080');
    console.log('PASS Chrome: formulario envía Origin correcto y llega al login de Keycloak. No se ingresaron credenciales.');
  }
  await context.close();
} finally { await browser.close(); }
