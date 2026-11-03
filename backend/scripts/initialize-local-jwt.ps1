param([switch]$Rotate)

$ErrorActionPreference = 'Stop'
$jwtBackendDirectory = Split-Path -Parent $PSScriptRoot
$jwtLocalDirectory = Join-Path $jwtBackendDirectory '.local'
$jwtPropertiesPath = Join-Path $jwtLocalDirectory 'jwt.properties'

if ((Test-Path -LiteralPath $jwtPropertiesPath) -and -not $Rotate) {
    Write-Output 'Local JWT key already exists. It has been preserved. Use -Rotate only to invalidate existing access tokens.'
    return
}

$jwtRandomBytes = New-Object byte[] 64
$jwtRandomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $jwtRandomGenerator.GetBytes($jwtRandomBytes)
} finally {
    $jwtRandomGenerator.Dispose()
}
$jwtGeneratedSecret = [Convert]::ToBase64String($jwtRandomBytes)
[System.IO.Directory]::CreateDirectory($jwtLocalDirectory) | Out-Null
$jwtPropertyLine = 'jwt.signer-key=${JWT_SIGNER_KEY:' + $jwtGeneratedSecret + '}'
[System.IO.File]::WriteAllText($jwtPropertiesPath, $jwtPropertyLine + [Environment]::NewLine,
        [System.Text.UTF8Encoding]::new($false))
Write-Output 'Created a persistent local JWT key from 64 random bytes. The key is not printed and .local is ignored by Git.'
