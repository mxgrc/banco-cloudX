$ErrorActionPreference = 'Stop'
$destination = Join-Path $PSScriptRoot '.env'
if (Test-Path -LiteralPath $destination) {
    Write-Host '.env existente conservado. No se cambiaron credenciales.'
    exit 0
}
function New-LocalPassword {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$lines = @(
    'KC_BOOTSTRAP_ADMIN_USERNAME=bootstrap-admin'
    ('KC_BOOTSTRAP_ADMIN_PASSWORD=' + (New-LocalPassword))
    ('KC_DB_PASSWORD=' + (New-LocalPassword))
)
[System.IO.File]::WriteAllLines($destination, $lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host 'Credenciales aleatorias generadas en keycloak/.env (excluido de Git).'
Write-Host 'Consulta ese archivo localmente para iniciar sesión. No lo compartas.'
