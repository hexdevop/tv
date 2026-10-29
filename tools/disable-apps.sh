#!/usr/bin/env bash
# Отключает безопасный набор системных приложений (T-008, APPS.md §1). Обратимо: pm enable.
# Использование: tools/disable-apps.sh [ip:port] | tools/disable-apps.sh <dev> - undo
set -u
DEV=${1:-192.168.1.5:5555}
ADB=${ADB:-adb}
PKGS="com.android.bips com.android.bluetoothmidiservice com.android.bookmarkprovider
com.android.cameraextensions com.android.dynsystem com.android.mtp com.android.se
com.android.settings.intelligence com.android.traceur com.android.wallpaperbackup
com.google.android.exoplayer2.demo com.hikeen.cleanmaster com.hisilicon.dlna.dmp
com.dolphin.assistant com.google.android.backdrop com.google.android.katniss
com.google.android.onetimeinitializer com.hisilicon.miracast zeasn.open.technical.tv.store"

cmd=disable-user; [ "${3:-}" = undo ] && cmd=enable
for p in $PKGS; do
  if [ "$cmd" = enable ]; then a="pm enable $p"; else a="pm disable-user --user 0 $p"; fi
  "$ADB" -s "$DEV" shell "su 0 $a" | tr -d '\r'
done
echo "--- disabled:"
"$ADB" -s "$DEV" shell "pm list packages -d" | tr -d '\r' | grep -F -w -f <(echo $PKGS | tr ' ' '\n' | sed 's/^/package:/')
