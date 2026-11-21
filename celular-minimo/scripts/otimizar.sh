#!/usr/bin/env bash
# Otimiza um celular Android via ADB, sem root. Tudo é reversível (restaurar.sh).
# Uso:  ./otimizar.sh            -> modo padrão (seguro)
#       ./otimizar.sh --pesado   -> também desativa apps Google pesados (listas/google-pesado.txt)
set -u
cd "$(dirname "$0")"
PESADO=0; [ "${1:-}" = "--pesado" ] && PESADO=1

adb get-state >/dev/null 2>&1 || { echo "Celular não encontrado. Ative a Depuração USB e autorize no celular."; exit 1; }

SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
MARCA=$(adb shell getprop ro.product.manufacturer | tr -d '\r' | tr '[:upper:]' '[:lower:]')
echo "== Aparelho: $(adb shell getprop ro.product.model | tr -d '\r') | marca: $MARCA | Android SDK $SDK"

listas="listas/google.txt"
case "$MARCA" in
  *samsung*) listas="$listas listas/samsung.txt" ;;
  *xiaomi*|*redmi*|*poco*) listas="$listas listas/xiaomi.txt" ;;
  *motorola*|*lenovo*) listas="$listas listas/motorola.txt" ;;
esac
[ $PESADO = 1 ] && listas="$listas listas/google-pesado.txt"

limpa() { sed -e 's/#.*//' -e 's/[[:space:]]//g' "$@" | grep -v '^$'; }
INSTALADOS=$(adb shell pm list packages | tr -d '\r' | sed 's/^package://')
MANTER=$(limpa manter.txt)
touch alterados.txt

echo "== 1/6 Removendo apps inúteis (listas: $listas)"
for pkg in $(limpa $listas | sort -u); do
  echo "$INSTALADOS" | grep -qx "$pkg" || continue
  echo "$MANTER" | grep -qx "$pkg" && { echo "   mantido: $pkg"; continue; }
  if adb shell pm disable-user --user 0 "$pkg" 2>&1 | grep -qi "disabled"; then
    echo "   desativado: $pkg"
  elif adb shell pm uninstall -k --user 0 "$pkg" 2>&1 | grep -qi "success"; then
    echo "   removido p/ usuário: $pkg"
  else
    echo "   não deu: $pkg"; continue
  fi
  grep -qx "$pkg" alterados.txt || echo "$pkg" >> alterados.txt
done

echo "== 2/6 Animações mais rápidas (0.5x)"
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb shell settings put global $k 0.5
done

echo "== 3/6 Menos varredura de Wi-Fi/Bluetooth em segundo plano"
adb shell settings put global wifi_scan_always_enabled 0
adb shell settings put global ble_scan_always_enabled 0

echo "== 4/6 Bloqueando segundo plano dos apps em sem-segundo-plano.txt"
if [ "$SDK" -ge 28 ]; then
  for pkg in $(limpa sem-segundo-plano.txt); do
    adb shell cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore && echo "   $pkg"
  done
else echo "   (precisa Android 9+, pulando)"; fi

echo "== 5/6 Limpando cache de todos os apps"
adb shell pm trim-caches 999G

if [ -f MinimoLauncher.apk ]; then
  echo "== Instalando launcher Mínimo"
  adb install -r MinimoLauncher.apk && \
  adb shell cmd package set-home-activity com.minimo.launcher/.MainActivity 2>/dev/null || \
  echo "   Abra o app 'Mínimo' e escolha-o como tela inicial padrão."
fi

echo "== 6/6 Recompilando apps para ficarem mais rápidos (pode levar 5-20 min, deixe o cabo conectado)"
if [ "$SDK" -ge 24 ]; then
  adb shell cmd package bg-dexopt-job
fi

echo
echo "Pronto! Reinicie o celular. Para desfazer tudo: ./restaurar.sh"
