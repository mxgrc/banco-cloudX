const users = 'http://localhost:8084';
const benefits = 'http://localhost:8084';
const enrollments = 'http://localhost:8084';
const escape = v => String(v).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

export function actionPlan(action, fields, roles) {
  const admin = ['listar-usuarios','aprobar','desactivar','reactivar','crear-beneficio','listar-beneficios'];
  const cliente = ['registrar','mi-estado','disponibles','inscribir','mis-inscripciones'];
  const role = admin.includes(action) ? 'ADMIN' : cliente.includes(action) ? 'CLIENTE' : null;
  if (!role || !roles?.includes(role)) throw Error('Acción no permitida para esta sesión');
  let method = 'GET', url, body;
  switch (action) {
    case 'registrar': method='POST'; url=users+'/api/usuarios/me'; break;
    case 'mi-estado': url=users+'/api/usuarios/me'; break;
    case 'listar-usuarios': url=users+'/api/usuarios?tamano=100'; break;
    case 'aprobar': case 'desactivar': case 'reactivar': {
      const sub = fields.get('sub') ?? '';
      if (!sub || sub.length>255 || sub.trim()!==sub || /[\/\\\x00-\x1f]/.test(sub) || sub==='.' || sub==='..')
        throw Error('Copia el usuarioSub del listado de usuarios');
      method='PATCH'; url=users+'/api/usuarios/'+encodeURIComponent(sub)+'/'+action; break;
    }
    case 'crear-beneficio':
      method='POST'; url=benefits+'/api/beneficios';
      body={nombre:'Beneficio de prueba',descripcion:'Descuento de prueba para verificar la inscripción',descuento:15}; break;
    case 'listar-beneficios': url=benefits+'/api/beneficios?tamano=100'; break;
    case 'disponibles': url=benefits+'/api/beneficios/disponibles?tamano=100'; break;
    case 'inscribir': {
      const id=fields.get('beneficioId') ?? '';
      if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id))
        throw Error('Copia el id UUID del beneficio disponible');
      method='POST'; url=enrollments+'/api/inscripciones'; body={beneficioId:id}; break;
    }
    case 'mis-inscripciones': url=enrollments+'/api/inscripciones/me'; break;
  }
  return {method,url,body};
}

export async function executeAction(action, fields, session, fetcher=fetch) {
  if (!session.access) throw Error('Inicia sesión nuevamente para probar las APIs');
  const plan=actionPlan(action,fields,session.report?.rol);
  const response=await fetcher(plan.url,{
    method:plan.method, headers:{Authorization:'Bearer '+session.access,...(plan.body?{'Content-Type':'application/json'}:{})},
    body:plan.body?JSON.stringify(plan.body):undefined,
    redirect:'error',signal:AbortSignal.timeout(12000)
  });
  const text=await response.text();
  let result;
  try { result=text?JSON.parse(text):null; } catch { result='Respuesta no JSON'; }
  // Destinos fijos: solo APIs locales propias. Nunca se incluye el token de petición.
  return {via:'BFF :8084',accion:action,metodo:plan.method,http:response.status,correcto:response.ok,resultado:result,comprobadoEn:new Date().toISOString()};
}

export function businessPanel(session) {
  if (!session.access) return '';
  const form=(action,label,input='') => `<form method="post" action="/negocio"><input type="hidden" name="csrf" value="${escape(session.csrf)}"><input type="hidden" name="accion" value="${action}">${input}<button>${label}</button></form>`;
  const sub='<label>usuarioSub del cliente <input name="sub" required maxlength="255"></label> ';
  let controls='';
  if(session.report?.rol?.includes('CLIENTE')) {
    controls=form('registrar','1. Registrar mi perfil')+form('mi-estado','Consultar mi estado')+
      form('disponibles','Ver beneficios disponibles')+
      form('inscribir','Inscribirme','<label>ID del beneficio <input name="beneficioId" required maxlength="36"></label> ')+
      form('mis-inscripciones','Ver mis inscripciones');
  } else if(session.report?.rol?.includes('ADMIN')) {
    controls=form('listar-usuarios','Listar clientes (máximo 100)')+form('aprobar','Aprobar cliente',sub)+
      form('desactivar','Desactivar cliente',sub)+form('reactivar','Reactivar cliente',sub)+
      form('crear-beneficio','Crear beneficio de prueba (15%)')+form('listar-beneficios','Listar beneficios (máximo 100)');
  }
  return `<section><h2>Prueba a través del BFF · Fase 7</h2><p>Los botones de registro, aprobación y creación modifican la base local temporal. Cada clic en crear beneficio agrega uno nuevo.</p>${controls}${session.business?'<h3>Última respuesta de la API</h3><pre>'+escape(JSON.stringify(session.business,null,2))+'</pre>':''}<p>401: renueva la sesión. 403: revisa el rol o la aprobación del cliente. 409: operación incompatible o inscripción duplicada.</p></section>`;
}
