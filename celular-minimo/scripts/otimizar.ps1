# Otimiza um celular Android via ADB, sem root. Tudo é reversível (restaurar.bat).
param([switch]$Pesado)
Set-Location $PSScriptRoot
$adb = if (Test-Path ".\adb.exe") { ".\adb.exe" } else { "adb" }
function Sh { & $adb shell @args 2>&1 | Out-String }
function Limpa($arquivos) {
  foreach ($a in $arquivos) { Get-Content $a | ForEach-Object { ($_ -replace '#.*','').Trim() } | Where-Object { $_ } }
}

if ((& $adb get-state 2>$null) -ne "device") { Write-Host "Celular nao encontrado. Ative a Depuracao USB e autorize no celular."; exit 1 }

$sdk   = [int](Sh getprop ro.build.version.sdk).Trim()
$marca = (Sh getprop ro.product.manufacturer).Trim().ToLower()
Write-Host "== Aparelho: $((Sh getprop ro.product.model).Trim()) | marca: $marca | Android SDK $sdk"

$listas = @("listas\google.txt")
if ($marca -match "samsung") { $listas += "listas\samsung.txt" }
elseif ($marca -match "xiaomi|redmi|poco") { $listas += "listas\xiaomi.txt" }
elseif ($marca -match "motorola|lenovo") { $listas += "listas\motorola.txt" }
if ($Pesado) { $listas += "listas\google-pesado.txt" }

$instalados = (Sh pm list packages) -split "`r?`n" | ForEach-Object { $_ -replace '^package:','' }
$manter = Limpa @("manter.txt")
if (-not (Test-Path alterados.txt)) { New-Item alterados.txt | Out-Null }

Write-Host "== 1/6 Removendo apps inuteis"
foreach ($pkg in (Limpa $listas | Sort-Object -Unique)) {
  if ($instalados -notcontains $pkg) { continue }
  if ($manter -contains $pkg) { Write-Host "   mantido: $pkg"; continue }
  if ((Sh pm disable-user --user 0 $pkg) -match "disabled") { Write-Host "   desativado: $pkg" }
  elseif ((Sh pm uninstall -k --user 0 $pkg) -match "Success") { Write-Host "   removido p/ usuario: $pkg" }
  else { Write-Host "   nao deu: $pkg"; continue }
  if ((Get-Content alterados.txt) -notcontains $pkg) { Add-Content alterados.txt $pkg }
}

Write-Host "== 2/6 Animacoes mais rapidas (0.5x)"
foreach ($k in "window_animation_scale","transition_animation_scale","animator_duration_scale") { Sh settings put global $k 0.5 | Out-Null }

Write-Host "== 3/6 Menos varredura de Wi-Fi/Bluetooth em segundo plano"
Sh settings put global wifi_scan_always_enabled 0 | Out-Null
Sh settings put global ble_scan_always_enabled 0 | Out-Null

Write-Host "== 4/6 Bloqueando segundo plano (sem-segundo-plano.txt)"
if ($sdk -ge 28) { foreach ($pkg in (Limpa @("sem-segundo-plano.txt"))) { Sh cmd appops set $pkg RUN_ANY_IN_BACKGROUND ignore | Out-Null; Write-Host "   $pkg" } }

Write-Host "== 5/6 Limpando cache de todos os apps"
Sh pm trim-caches 999G | Out-Null

if (Test-Path MinimoLauncher.apk) {
  Write-Host "== Instalando launcher Minimo"
  & $adb install -r MinimoLauncher.apk
  Sh cmd package set-home-activity com.minimo.launcher/.MainActivity | Out-Null
  Write-Host "   Se a tela inicial nao mudar: abra o app 'Minimo' e escolha-o como padrao."
}

Write-Host "== 6/6 Recompilando apps (5-20 min, deixe o cabo conectado)"
if ($sdk -ge 24) { Sh cmd package bg-dexopt-job | Out-Null }

Write-Host "`nPronto! Reinicie o celular. Para desfazer: restaurar.bat"
