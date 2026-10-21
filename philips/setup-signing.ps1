# Maak je private Android APK-sleutel eenmalig op jouw pc.
# Vereist JDK 17+ en 'keytool' in PATH.
$ErrorActionPreference = "Stop"
$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) { throw "Geen keytool gevonden. Installeer Java JDK 17 (Temurin), start PowerShell opnieuw en probeer opnieuw." }
$directory = Join-Path $env:USERPROFILE "PhilipsCleanHomeSigning"
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$keyPath = Join-Path $directory "philips-clean-home.p12"
if (-not (Test-Path -LiteralPath $keyPath)) {
    Write-Host "Kies een sterk wachtwoord dat je veilig bewaart (minstens zes tekens)."
    & $keytool.Source -genkeypair -keystore $keyPath -storetype PKCS12 -alias philips-clean-home -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Philips Clean Home, O=Personal TV, C=BE"
    if ($LASTEXITCODE -ne 0) { throw "keytool mislukt" }
}
else { Write-Host "Bestaande sleutel wordt hergebruikt: $keyPath" }
$base64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($keyPath))
Set-Clipboard -Value $base64
Write-Host ""
Write-Host "Keystore: $keyPath"
Write-Host "Base64 staat op het klembord."
Write-Host "GitHub: https://github.com/RafM2501/tv-vibecodePhillipsTV/settings/secrets/actions"
Write-Host "Voeg twee Actions repository secrets toe:"
Write-Host "1. PHILIPS_KEYSTORE_BASE64 = plak nu je klembord."
Write-Host "2. PHILIPS_SIGNING_PASSWORD = het zelfgekozen keystorewachtwoord."
Write-Host "Bewaar sleutel EN wachtwoord offline. Deel nooit je sleutel of wachtwoord in de chat."
Write-Host "Wis daarna het klembord."
