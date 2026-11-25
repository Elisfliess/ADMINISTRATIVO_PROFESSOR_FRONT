$ErrorActionPreference = "Stop"

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw "Este script requer PowerShell 7 ou superior. Execute com: pwsh -NoProfile -File .\backend\scripts\run-with-encrypted-env.ps1"
}

$BackendDir = Split-Path -Parent $PSScriptRoot
$EncryptedEnvPath = Join-Path $BackendDir ".env.enc"
$MavenWrapper = Join-Path $BackendDir "mvnw.cmd"
if (-not $IsWindows) {
    $MavenWrapper = Join-Path $BackendDir "mvnw"
}

if (-not (Get-Command age -ErrorAction SilentlyContinue)) {
    throw "age não foi encontrado no PATH. Instale o age e abra um novo terminal."
}
if (-not (Test-Path -LiteralPath $EncryptedEnvPath -PathType Leaf)) {
    throw "backend/.env.enc não encontrado. Solicite o arquivo à pessoa responsável pelo ambiente."
}
if (-not (Test-Path -LiteralPath $MavenWrapper -PathType Leaf)) {
    throw "Maven Wrapper não encontrado em backend."
}

Write-Host "Descriptografando backend/.env.enc em memória. O age solicitará a senha."
$DecryptedText = (& age --decrypt --output - $EncryptedEnvPath | Out-String)
$AgeExitCode = $LASTEXITCODE
if ($AgeExitCode -ne 0) {
    $DecryptedText = $null
    throw "Não foi possível descriptografar o arquivo. Confira a senha e a integridade do .env.enc."
}

$EnvironmentNames = @()
$OriginalEnvironment = @{}
try {
    foreach ($Line in ($DecryptedText -split "\r?\n")) {
        $TrimmedLine = $Line.Trim()
        if ([string]::IsNullOrWhiteSpace($TrimmedLine) -or $TrimmedLine.StartsWith("#")) {
            continue
        }

        if ($Line -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            throw "Formato inválido no .env: use linhas KEY=VALUE, sem 'export'. Nenhum valor foi exibido."
        }

        $Name = $Matches[1]
        # Accept new application variables without requiring a script change.
        # Keep process/runtime-critical variables protected from accidental replacement.
        $ReservedNames = @(
            "PATH", "PATHEXT", "PSMODULEPATH", "PSHOME", "COMSPEC",
            "SYSTEMROOT", "WINDIR", "TEMP", "TMP", "HOME", "USERPROFILE",
            "JAVA_HOME", "PROCESSOR_ARCHITECTURE", "PROCESSOR_IDENTIFIER",
            "OS", "PROMPT"
        )
        if ($ReservedNames -contains $Name.ToUpperInvariant()) {
            throw "Variável reservada do sistema não pode ser definida pelo .env: $Name."
        }
        $Value = $Matches[2].Trim()
        if ($Value.Length -ge 2) {
            $First = $Value.Substring(0, 1)
            $Last = $Value.Substring($Value.Length - 1, 1)
            if (($First -eq '"' -and $Last -eq '"') -or ($First -eq "'" -and $Last -eq "'")) {
                $Value = $Value.Substring(1, $Value.Length - 2)
            }
        }

        if ($EnvironmentNames -notcontains $Name) {
            $EnvironmentNames += $Name
            $OriginalEnvironment[$Name] = [Environment]::GetEnvironmentVariable($Name, "Process")
        }
        [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
    }

    foreach ($RequiredName in @("DB_URL", "DB_USERNAME", "DB_PASSWORD")) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($RequiredName, "Process"))) {
            throw "Variável obrigatória ausente ou vazia: $RequiredName."
        }
    }

    $DecryptedText = $null
    Write-Host "Iniciando a aplicação. As variáveis serão removidas/restauradas ao encerrar o processo."
    Push-Location $BackendDir
    try {
        if ($IsWindows) {
            & $MavenWrapper spring-boot:run
        } else {
            & bash $MavenWrapper spring-boot:run
        }
        $MavenExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}
finally {
    $DecryptedText = $null
    foreach ($Name in $EnvironmentNames) {
        [Environment]::SetEnvironmentVariable($Name, $OriginalEnvironment[$Name], "Process")
    }
}

if ($null -ne $MavenExitCode -and $MavenExitCode -ne 0) {
    exit $MavenExitCode
}
