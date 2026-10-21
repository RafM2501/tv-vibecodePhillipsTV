# Philips Clean Home - optionele systeemapps veilig beheren
# Eerst pakketten controleren. Dit script verwijdert nooit automatisch iets.
param([switch]$Apply, [switch]$Restore)
$adb = ".\adb.exe"
$packages = @(
  "com.google.android.youtube.tvmusic",
  "com.google.android.play.games",
  "com.google.android.videos",
  "org.droidtv.android.fitness",
  "org.droidtv.nettvrecommender"
)
if (-not (Test-Path $adb)) { throw "Open PowerShell in de platform-tools map (waar adb.exe staat)." }
& $adb devices
Write-Host "Geselecteerde optionele pakketten:" -ForegroundColor Cyan
foreach ($pkg in $packages) {
  & $adb shell pm path $pkg
  if ($Restore) {
    Write-Host "Herstel $pkg"
    & $adb shell cmd package install-existing --user 0 $pkg
    & $adb shell pm enable --user 0 $pkg
  } elseif ($Apply) {
    $confirmation = Read-Host "Verwijder $pkg voor gebruiker 0? (typ JA)"
    if ($confirmation -ceq "JA") {
      & $adb shell pm uninstall --user 0 $pkg
    } else { Write-Host "Overgeslagen: $pkg" }
  } else {
    Write-Host "Voorbeeld (NIETS gewijzigd): adb shell pm uninstall --user 0 $pkg"
  }
}
Write-Host "Klaar. Zonder -Apply is niets verwijderd."