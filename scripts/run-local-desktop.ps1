[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    & .\mvnw.cmd --batch-mode --no-transfer-progress package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Maven package failed.' }
    & .\mvnw.cmd --batch-mode --no-transfer-progress -pl desktop dependency:copy-dependencies '-DincludeScope=runtime' '-DoutputDirectory=target/runtime'
    if ($LASTEXITCODE -ne 0) { throw 'Dependency copy failed.' }
    & java -cp "$projectRoot\desktop\target\classes;$projectRoot\desktop\target\runtime\*" `
        io.github.marcuzapl.coregnition.desktop.DesktopLauncher `
        "--backend-jar=$projectRoot\backend\target\coregnition-backend-0.0.1-SNAPSHOT.jar" `
        "--data-dir=$projectRoot\data"
    if ($LASTEXITCODE -ne 0) { throw 'Desktop launch failed.' }
} finally { Pop-Location }
