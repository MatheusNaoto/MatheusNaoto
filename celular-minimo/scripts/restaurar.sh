#!/usr/bin/env bash
# Desfaz o que o otimizar.sh fez.
set -u
cd "$(dirname "$0")"
adb get-state >/dev/null 2>&1 || { echo "Celular não encontrado."; exit 1; }
if [ -f alterados.txt ]; then
  for pkg in $(tr -d '\r' < alterados.txt); do
    adb shell pm enable "$pkg" >/dev/null 2>&1
    adb shell cmd package install-existing "$pkg" >/dev/null 2>&1
    echo "restaurado: $pkg"
  done
  rm alterados.txt
fi
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb shell settings put global $k 1
done
for pkg in $(sed -e 's/#.*//' -e 's/[[:space:]]//g' sem-segundo-plano.txt | grep -v '^$'); do
  adb shell cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND allow
done
echo "Tudo restaurado. Reinicie o celular."
