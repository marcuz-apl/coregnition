[CmdletBinding()]
param([switch]$Installer)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    throw 'Windows packaging must run on Windows with an x64 JDK 21.'
}
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to an x64 JDK 21.' }
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $jpackage)) { throw "jpackage was not found: $jpackage" }
$javaSettings = (& $java -XshowSettings:properties -version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaSettings -notmatch 'java.specification.version\s*=\s*21\s' -or $javaSettings -notmatch 'os.arch\s*=\s*amd64\s') {
    throw 'JAVA_HOME must select an x64 JDK 21.'
}
if ($Installer) {
    foreach ($tool in @('candle.exe', 'light.exe')) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "Install WiX 3.x and add its bin directory to PATH; missing $tool." }
    }
}
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    & .\mvnw.cmd --batch-mode --no-transfer-progress verify
    if ($LASTEXITCODE -ne 0) { throw 'Maven verify failed.' }
    $output = Join-Path $projectRoot ('desktop\target\windows-package-' + [guid]::NewGuid().ToString('N'))
    $inputDirectory = Join-Path $output 'input'
    New-Item -ItemType Directory -Path $inputDirectory -Force | Out-Null
    & .\mvnw.cmd --batch-mode --no-transfer-progress -pl desktop dependency:copy-dependencies '-DincludeScope=runtime' "-DoutputDirectory=$inputDirectory"
    if ($LASTEXITCODE -ne 0) { throw 'Dependency copy failed.' }
    Copy-Item 'desktop\target\coregnition-desktop-0.0.1-SNAPSHOT.jar' (Join-Path $inputDirectory 'desktop.jar')
    Copy-Item 'backend\target\coregnition-backend-0.0.1-SNAPSHOT.jar' (Join-Path $inputDirectory 'backend.jar')
    $appVersion = ((Get-Content VERSION -Raw).Trim() -replace '^v', '') -replace '\+.*$', ''
    & $jpackage --type app-image --name Coregnition --dest $output `
        --input $inputDirectory --main-jar desktop.jar `
        --main-class io.github.marcuzapl.coregnition.desktop.DesktopLauncher `
        --app-version $appVersion --vendor Coregnition `
        --jlink-options '--strip-debug --no-man-pages --no-header-files' `
        --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.zipfs
    if ($LASTEXITCODE -ne 0) { throw 'jpackage app-image failed.' }
    $appImage = Join-Path $output 'Coregnition'
    foreach ($file in @('Coregnition.exe', 'app\desktop.jar', 'app\backend.jar', 'runtime\bin\java.exe')) {
        if (-not (Test-Path (Join-Path $appImage $file))) { throw "Incomplete app image: $file" }
    }
    $archive = Join-Path $output 'coregnition-windows-x64.zip'
    Compress-Archive -Path $appImage -DestinationPath $archive
    if ($Installer) {
        & $jpackage --type exe --name Coregnition --dest $output --app-image $appImage `
            --app-version $appVersion --vendor Coregnition --win-per-user-install `
            --win-menu --win-shortcut --win-dir-chooser `
            --win-upgrade-uuid 'ec06b6ec-8402-4898-9c59-897233a73266'
        if ($LASTEXITCODE -ne 0) { throw 'jpackage EXE installer failed.' }
    }
    Write-Host "Windows application: $appImage\Coregnition.exe"
    Write-Host "Portable Windows archive: $archive"
} finally { Pop-Location }
