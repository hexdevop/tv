#!/usr/bin/env bash
# Пускает adb (5555) только с ПК разработчика (T-004). Живёт до перезагрузки ТВ.
# Использование: tools/adb-lock.sh [ip:port] [ip ПК] | tools/adb-lock.sh <dev> - undo
set -u
DEV=${1:-192.168.1.5:5555}
PC=${2:-192.168.1.7}
ADB=${ADB:-adb}
[ "$PC" = - ] && PC=192.168.1.7

del="iptables -D INPUT -p tcp --dport 5555 ! -s $PC -j DROP; ip6tables -D INPUT -p tcp --dport 5555 -j DROP"
if [ "${3:-}" = undo ]; then
  "$ADB" -s "$DEV" shell "su 0 sh -c '$del' 2>/dev/null"; exit
fi
# снять старые копии правил, потом вставить заново (идемпотентно)
"$ADB" -s "$DEV" shell "su 0 sh -c '$del' 2>/dev/null; su 0 sh -c 'iptables -I INPUT -p tcp --dport 5555 ! -s $PC -j DROP; ip6tables -I INPUT -p tcp --dport 5555 -j DROP; iptables -S INPUT | grep 5555; ip6tables -S INPUT | grep 5555'"
