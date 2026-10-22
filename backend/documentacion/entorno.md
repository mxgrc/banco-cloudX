# Entorno y laboratorio

## Diagnóstico local — 2026-09-11

| Herramienta | Resultado observado |
| --- | --- |
| Java | Temurin 17.0.18 |
| Node | 24.11.0 |
| npm | 11.12.1 |
| Maven | 3.9.14, ejecutándose con Java 17 |
| Git | 2.49.0.windows.1 |
| Docker CLI | 29.4.0 |
| Docker Compose | 5.1.1 |
| Motor Docker | Inicialmente detenido; luego motor Linux 29.4.0 disponible |

Comprobaciones manuales desde PowerShell:

```powershell
java -version
node --version
npm.cmd --version
mvn.cmd --version
git --version
docker compose version
docker info
```

Se usa npm.cmd para invocar directamente el ejecutable de Windows. Docker instalado
no significa que su motor esté iniciado. En fase 3 se inició Docker Desktop,
se descargaron las imágenes y se verificaron Keycloak y PostgreSQL, incluyendo
persistencia del realm master después de reiniciar ambos contenedores.

## Restricciones suministradas por el estudiante

Fuente: Readme de Learner Lab actualizado el 2025-06-24 y captura de AWS Details.
Las políticas pueden cambiar; se comprobarán permisos al desplegar.

- Regiones: us-east-1 y us-west-2; propuesta us-east-1.
- EC2: On-Demand, tamaños nano a large, límites de 9 instancias y 32 vCPU por región.
- RDS: PostgreSQL permitido, tamaños hasta medium, gp2 hasta 100 GB;
  Enhanced Monitoring no permitido.
- IAM: usar roles existentes LabRole y LabInstanceProfile según necesidad;
  no diseñar el despliegue suponiendo creación de roles personalizados.
- API Gateway, VPC, ELB y ACM figuran como permitidos.
- Route 53 no permite registrar dominios.
- Presupuesto total USD 50; captura: USD 8.50 usados. El saldo mostrado puede
  retrasarse de 8 a 12 horas. Esto no es una medición actual del gasto.
- EC2 se detiene entre sesiones y vuelve a iniciar; puede cambiar su IP pública.
- RDS puede permanecer encendido; detenido se reinicia después de siete días.
- Reset elimina recursos y no repone presupuesto.

Primero desarrollo local. Antes de desplegar, inventariar recursos existentes,
estimar consumo y documentar procedimiento de cierre. No eliminar recursos ajenos
al proyecto ni incluir credenciales del laboratorio en Git o capturas públicas.
