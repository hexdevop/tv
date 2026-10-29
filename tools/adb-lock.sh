#!/usr/bin/env bash
# Пускает adb (5555) только из домашней подсети или с одного ПК (T-004). После загрузки то же делает tools/adb-lock.rc.
# Использование: tools/adb-lock.sh [ip:port] [ip ПК или подсеть] | tools/adb-lock.sh <dev> - undo
set -u
DEV=${1:-192.168.1.5:5555}
PC=${2:-192.168.1.0/24}
ADB=${ADB:-adb}
[ "$PC" = - ] && PC=192.168.1.0/24

# снять ВСЕ правила для 5555 (с любым IP), чтобы не осталось двух DROP с разными ПК
del='for t in iptables ip6tables; do r=$($t -w -S INPUT | grep -e "--dport 5555" | sed "s/^-A //"); echo "$r" | while read -r l; do [ -n "$l" ] && $t -w -D $l; done; done'
add="iptables -w -I INPUT -p tcp --dport 5555 ! -s $PC -j DROP; ip6tables -w -I INPUT -p tcp --dport 5555 -j DROP"
show='iptables -L INPUT -n --line-numbers | head -4; ip6tables -S INPUT | grep 5555'
if [ "${3:-}" = undo ]; then script="$del"; else script="$del; $add; $show"; fi
# скрипт идёт через stdin, без вложенных кавычек
echo "$script" | "$ADB" -s "$DEV" shell su 0 sh
