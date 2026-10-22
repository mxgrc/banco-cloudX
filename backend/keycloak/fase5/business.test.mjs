import test from 'node:test';
import assert from 'node:assert/strict';
import { actionPlan,executeAction,businessPanel } from './business.mjs';
const fields=v=>new URLSearchParams(v);
test('cliente no ejecuta operaciones administrativas',()=>{
  assert.throws(()=>actionPlan('aprobar',fields({sub:'cliente'}),['CLIENTE']));
  assert.throws(()=>actionPlan('registrar',fields({}),['ADMIN']));
});
test('destinos fijos y validación de parámetros',()=>{
  assert.throws(()=>actionPlan('http://otro.test',fields({}),['ADMIN']));
  for(const sub of ['..','a/b','a\\b','']) assert.throws(()=>actionPlan('aprobar',fields({sub}),['ADMIN']));
  assert.throws(()=>actionPlan('inscribir',fields({beneficioId:'otro'}),['CLIENTE']));
  const p=actionPlan('aprobar',fields({sub:'cliente-sub'}),['ADMIN']);
  assert.equal(p.url,'http://localhost:8084/api/usuarios/cliente-sub/aprobar');
  assert.equal(p.method,'PATCH');
});
test('propaga token sin incluirlo en resultado y conserva 403',async()=>{
  const session={access:'secreto',report:{rol:['CLIENTE']}};
  const result=await executeAction('disponibles',fields({}),session,async(url,init)=>{
    assert.equal(url,'http://localhost:8084/api/beneficios/disponibles?tamano=100');
    assert.equal(init.headers.Authorization,'Bearer secreto');
    assert.equal(init.redirect,'error');
    return new Response('{"status":403}',{status:403});
  });
  assert.equal(result.http,403); assert.equal(result.correcto,false);
  assert.ok(!JSON.stringify(result).includes('secreto'));
});
test('registro no acepta identidad desde formulario',()=>{
  const p=actionPlan('registrar',fields({usuarioSub:'otro'}),['CLIENTE']);
  assert.equal(p.body,undefined); assert.equal(p.url,'http://localhost:8084/api/usuarios/me');
});
test('sin login no permite llamadas',async()=>{
  await assert.rejects(executeAction('mi-estado',fields({}),{}),/Inicia sesión/);
});
test('panel muestra controles por rol y escapa respuestas',()=>{
  const html=businessPanel({access:'secreto',csrf:'csrf',report:{rol:['CLIENTE']},business:{valor:'<script>'}});
  assert.ok(html.includes('Inscribirme')); assert.ok(!html.includes('Aprobar cliente'));
  assert.ok(html.includes('&lt;script&gt;')); assert.ok(!html.includes('secreto'));
});
