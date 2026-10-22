$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080'
$discovery = Invoke-RestMethod "$baseUrl/realms/master/.well-known/openid-configuration" -TimeoutSec 15
if ($discovery.issuer -ne "$baseUrl/realms/master") { throw 'Issuer inesperado' }
if ($discovery.jwks_uri -ne "$baseUrl/realms/master/protocol/openid-connect/certs") { throw 'JWKS URI inesperada' }
$jwks = Invoke-RestMethod $discovery.jwks_uri -TimeoutSec 15
if (@($jwks.keys | Where-Object { $_.use -eq 'sig' -and $_.kty -eq 'RSA' }).Count -eq 0) {
    throw 'JWKS no contiene una clave RSA de firma'
}
$admin = Invoke-WebRequest "$baseUrl/admin/" -UseBasicParsing -TimeoutSec 15
if ($admin.StatusCode -ne 200) { throw 'Consola administrativa no disponible' }
Write-Host 'PASS: discovery OIDC, issuer, JWKS público RSA y consola HTTP 200.'
Write-Host 'Pendiente manual: login administrativo. Esto no prueba autorización de Banco Cloud.'
