#!/usr/bin/env bash
# Обратимое обезвреживание вредоносов (T-002): disable + suspend пакетов,
# файлы дропперов -> /data/local/quarantine (chmod 000), восстановление настроек.
# Откат: tools/neutralize.sh <dev> undo
set -u
DEV=${1:-192.168.1.5:5555}
ADB=${ADB:-adb}
BAD="a.b.c com.android.core.process com.android.g6tech com.android.media.module.services com.android.server.ambient com.android.system.service com.google.android.apps.status com.huawei.qx com.imjustafriendlyguy.firmware"
Q=/data/local/quarantine
T=/data/local/tmp

if [ "${2:-}" = undo ]; then
  "$ADB" -s "$DEV" shell "su 0 sh -c 'for p in $BAD; do pm unsuspend \$p; pm enable \$p; done; chmod 755 $Q/*; mv $Q/.[!.]* $Q/* $T/ 2>/dev/null'"
  exit
fi

"$ADB" -s "$DEV" shell "su 0 sh -c '
for p in $BAD; do
  am force-stop \$p
  echo \"\$p: \$(pm disable-user --user 0 \$p 2>&1 | tail -1) / \$(pm suspend \$p 2>&1 | tail -1)\"
done
mkdir -p $Q && chmod 700 $Q
for f in .d .z .s.apk .hwid_id monkeapp888 nc rxwp monkeysdk app.apk .a; do [ -e $T/\$f ] && mv $T/\$f $Q/ && echo \"quarantined \$f\"; done
chmod 000 $Q/* $Q/.[!.]* 2>/dev/null
settings put global package_verifier_enable 1
settings put global verifier_verify_adb_installs 1
settings put secure install_non_market_apps 0
settings put global app_standby_enabled 1
settings put secure enabled_accessibility_services com.hisilicon.rmservice/.HiRMService
echo selinux: \$(getenforce)
echo --- state
pm list packages -d | grep -cE \"$(echo $BAD | tr ' ' '|')\"
ls -la $T
'"
