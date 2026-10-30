#!/usr/bin/env bash
# Кладёт release-сборку лаунчера в /system/priv-app и перезагружает ТВ.
# adb install -r нельзя: обновление в /data теряет доступ к libhi_tv_jni.so.
# Использование: tools/install-launcher.sh [ip:port]
set -eu
export MSYS_NO_PATHCONV=1
DEV=${1:-192.168.1.5:5555}
ADB=${ADB:-adb}
APK=launcher/app/build/outputs/apk/release/app-release.apk

"$ADB" -s "$DEV" shell "pm uninstall-system-updates com.hexdevop.launcher" || true
"$ADB" -s "$DEV" push "$APK" /data/local/tmp/HexLauncher.apk
want=$(md5sum "$APK" | cut -d' ' -f1)
T=/system/priv-app/HexLauncher/HexLauncher.apk
# перезагрузка только при совпавшем хеше: битый APK на месте HOME хуже старого.
# кеш разбора сбрасываем, иначе система не увидит изменения манифеста
"$ADB" -s "$DEV" shell "su 0 sh -c 'mount -o remount,rw / && cp /data/local/tmp/HexLauncher.apk $T && chmod 644 $T && chcon u:object_r:system_file:s0 $T && sync && rm /data/local/tmp/HexLauncher.apk && find /data/system/package_cache -name \"*Hex*\" -delete && [ \"\$(md5sum $T | cut -d\" \" -f1)\" = $want ] && reboot'"
# после загрузки: adb shell cmd package compile -m speed -f com.hexdevop.launcher
