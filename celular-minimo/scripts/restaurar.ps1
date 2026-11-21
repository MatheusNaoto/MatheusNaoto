Set-Location $PSScriptRoot
$adb = if (Test-Path ".\adb.exe") { ".\adb.exe" } else { "adb" }
if (Test-Path alterados.txt) {
  foreach ($pkg in (Get-Content alterados.txt | Where-Object { $_.Trim() })) {
    & $adb shell pm enable $pkg 2>&1 | Out-Null
    & $adb shell cmd package install-existing $pkg 2>&1 | Out-Null
    Write-Host "restaurado: $pkg"
  }
  Remove-Item alterados.txt
}
foreach ($k in "window_animation_scale","transition_animation_scale","animator_duration_scale") { & $adb shell settings put global $k 1 }
Get-Content sem-segundo-plano.txt | ForEach-Object { ($_ -replace '#.*','').Trim() } | Where-Object { $_ } |
  ForEach-Object { & $adb shell cmd appops set $_ RUN_ANY_IN_BACKGROUND allow }
Write-Host "Tudo restaurado. Reinicie o celular."
