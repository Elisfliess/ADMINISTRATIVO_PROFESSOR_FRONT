$ErrorActionPreference = "Stop"

$BackendDir = Split-Path -Parent $PSScriptRoot
$PlainEnvPath = Join-Path $BackendDir ".env"
$EncryptedEnvPath = Join-Path $BackendDir ".env.enc"

if (-not (Get-Command age -ErrorAction SilentlyContinue)) {
    throw "age não foi encontrado no PATH. Instale o age e abra um novo terminal."
}
if (-not (Test-Path -LiteralPath $PlainEnvPath -PathType Leaf)) {
    throw "Arquivo backend/.env não encontrado. Crie-o a partir de .env.example e preencha as credenciais."
}
if (Test-Path -LiteralPath $EncryptedEnvPath) {
    throw "backend/.env.enc já existe. Remova ou renomeie o arquivo antes de gerar outro para evitar sobrescrita acidental."
}

Write-Host "O age solicitará uma senha e sua confirmação no terminal."
Write-Host "Use uma senha forte e compartilhe-a com a equipe por um canal diferente do arquivo."
& age --passphrase --output $EncryptedEnvPath $PlainEnvPath
if ($LASTEXITCODE -ne 0) {
    if (Test-Path -LiteralPath $EncryptedEnvPath) {
        Remove-Item -LiteralPath $EncryptedEnvPath -Force
    }
    throw "Falha ao criptografar backend/.env."
}

Write-Host "Arquivo criptografado criado em backend/.env.enc."
Write-Host "Não envie o .env original. Compartilhe .env.enc e transmita a senha separadamente."
