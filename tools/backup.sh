#!/usr/bin/env bash
# Снимает все разделы eMMC с ТВ через adb (root) и проверяет sha256.
# Использование: tools/backup.sh [ip:port] [папка]
set -u
DEV=${1:-192.168.1.5:5555}
OUT=${2:-backup/$(date +%Y%m%d-%H%M%S)}
ADB=${ADB:-adb}
BY=/dev/block/by-name
mkdir -p "$OUT"

parts=$("$ADB" -s "$DEV" shell "ls $BY" | tr -d '\r' | grep -vE '^mmcblk0$|^userdata$|^cache$')
"$ADB" -s "$DEV" shell "getprop" > "$OUT/getprop.txt"
"$ADB" -s "$DEV" shell "ls -l $BY; cat /proc/partitions" > "$OUT/partitions.txt"

: > "$OUT/SHA256SUMS"
for p in $parts; do
  "$ADB" -s "$DEV" exec-out "su 0 dd if=\$(readlink -f $BY/$p) bs=1048576 2>/dev/null" > "$OUT/$p.img"
  want=$("$ADB" -s "$DEV" shell "su 0 sha256sum \$(readlink -f $BY/$p)" | cut -d' ' -f1)
  got=$(sha256sum "$OUT/$p.img" | cut -d' ' -f1)
  [ "$want" = "$got" ] && st=OK || st=MISMATCH
  echo "$got  $p.img" >> "$OUT/SHA256SUMS"
  echo "$st $p $(stat -c %s "$OUT/$p.img")"
done
