# Arranca Spring Boot con JAVA_HOME correcto en Windows.
# Uso (desde esta carpeta):
#   .\run-spring-boot.ps1
#
# MongoDB Atlas (recomendado): define MONGODB_URI en la misma sesion, sin commitear:
#   $env:MONGODB_URI = "mongodb+srv://USER:PASSWORD@cluster/..."
#   .\run-spring-boot.ps1

$ErrorActionPreference = "Stop"

function Find-JdkHome {
    $base = "C:\Program Files\Java"
    if (-not (Test-Path $base)) { return $null }
    # Preferir versiones recientes conocidas del proyecto
    foreach ($name in @("jdk-22", "jdk-21", "jdk-17", "jdk-11")) {
        $p = Join-Path $base $name
        if (Test-Path (Join-Path $p "bin\java.exe")) { return $p }
    }
    # Cualquier jdk-* con java.exe
    Get-ChildItem $base -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^jdk-' -and (Test-Path (Join-Path $_.FullName "bin\java.exe")) } |
        Sort-Object Name -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}

$jdk = Find-JdkHome
if (-not $jdk) {
    Write-Error "No se encontro ningun JDK en C:\Program Files\Java\jdk-* con bin\java.exe. Instala JDK 17+ o JDK 22."
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;" + $env:Path

Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Cyan
& java -version 2>&1 | Write-Host

if (-not $env:MONGODB_URI) {
    Write-Host "Aviso: MONGODB_URI no esta definida; Spring usara application.yml / valor por defecto." -ForegroundColor Yellow
}

Set-Location $PSScriptRoot
mvn spring-boot:run @args
