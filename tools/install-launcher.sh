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
"$ADB" -s "$DEV" shell "su 0 sh -c 'mount -o remount,rw / && cp /data/local/tmp/HexLauncher.apk /system/priv-app/HexLauncher/HexLauncher.apk && chmod 644 /system/priv-app/HexLauncher/HexLauncher.apk && chcon u:object_r:system_file:s0 /system/priv-app/HexLauncher/HexLauncher.apk && sync && rm /data/local/tmp/HexLauncher.apk && md5sum /system/priv-app/HexLauncher/HexLauncher.apk && reboot'"
md5sum "$APK"
