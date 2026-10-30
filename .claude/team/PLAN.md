# План

| ID | Задача | Исполнитель | Зависит от | Статус |
|---|---|---|---|---|
| T-001 | Полный бэкап разделов + sha256 (`tools/backup.sh`), готово в `backup/20261029-factory` (26 разделов, 2.5 ГБ, все OK) | lead | — | done |
| T-002 | Обезвредить вирусы (`tools/neutralize.sh`): disable-user + suspend, дропперы в `/data/local/quarantine`, настройки проверки APK восстановлены | lead | T-001 | done |
| T-003 | Окончательно удалить вредоносные пакеты (`pm uninstall`) и карантин, с подтверждения пользователя 2026-10-29 | lead | T-002 | done |
| T-004 | Ограничить adb: iptables пускает 5555 только из 192.168.1.0/24 (IP ПК может меняться). Постоянно через `/system/etc/init/adb-lock.rc` (копия `tools/adb-lock.rc`, oneshot-сервис `adb-lock`, seclabel `u:r:su:s0`, запуск после boot_completed и при перезапуске netd, перезапуск `setprop ctl.start adb-lock`). Проверено после reboot 2026-10-29 | firmware-dev → security-auditor | T-002, T-007 | done |
| T-005 | Пользователь проверяет роутер: проброс 5555, DMZ, UPnP. Отложено: нет пароля от TP-Link XX530v, сеть решили не трогать | пользователь | — | blocked |
| T-006 | Инвентаризация 111 системных приложений: оставить / отключить / удалить, с обоснованием (`.claude/team/APPS.md`) | architect | T-001 | done |
| T-007 | Проверить, есть ли AVB/dm-verity и возможен ли remount `/system` (без изменений). Итог: AVB и verity нет (нет vbmeta, `root=/dev/mmcblk0p20` напрямую, в fstab нет `verify`/`avb`), ext4 без `shared_blocks`, SELinux permissive, свободно 265 МБ (system) и 155 МБ (vendor). Remount rw должен работать, сам remount не пробовали: проверим при первой записи (T-004) | lead | T-001 | done |
| T-008 | Отключить лишние системные приложения через `pm disable-user` по списку T-006 (`tools/disable-apps.sh`, 19 пакетов) | firmware-dev | T-006 | done |
| T-009 | Создать проект `launcher/` (Kotlin, Compose for TV, minSdk 29). Шаблон собран и запущен на ТВ (PSS ~57 МБ, холодный старт ~8 с) | backend-dev | Android Studio | done |
| T-010 | Изучить API `com.hisilicon.tv.service` для меню картинки и звука (итог: `HISILICON_API.md`) | architect | T-001 | done |
| T-011 | Лаунчер: «Затмение», все приложения, входы HDMI/AV, панели Картинка и Звук, p90 кадра 32 мс | backend-dev → qa-tester | T-009, T-010, T-012 | done |
| T-012 | Лаунчер в `/system/priv-app` и HOME, `com.zeasn.launcher` отключён (2026-10-30). `com.hikeen.menu` оставлен | firmware-dev | T-007, T-011 | done |
| T-014 | Авторизация adb: `ro.adb.secure=1` в `/system/build.prop`, ключ ПК в `/data/misc/adb/adb_keys`. Проверено 2026-10-30: подключение без ключа получает AUTH. Новый ПК: подтвердить запрос на экране ТВ. Оригинал `build.prop` в `/data/local/quarantine/build.prop.orig` и `backup/20261030-build-prop/`. Не проверено: пропадает ли правило iptables при перезапуске netd | firmware-dev → security-auditor | T-004 | done |
| T-013 | Свои logo, bootanimation, bootmusic | firmware-dev | T-007 | todo |

## Бэклог техдолга
- Release подписан debug-ключом: сделать отдельный ключ вне репозитория. `allowBackup=false`.
- После установки лаунчера запускать `cmd package compile -m speed -f com.hexdevop.launcher` (скрипт перезагружает ТВ, поэтому вручную).
- Повторное заражение (найдено 2026-10-30): `com.android.media.module.services` стоял заново с 2026-10-29 22:10:31, installer `com.zeasn.launcher`, sha256 6e8630a3...daa34f. Отключён и заморожен, APK в `/data/local/quarantine/media.module.services.apk.bad`. В `/data/local/quarantine/media0/`: `.system.apk` (фальшивый `com.google.play`, автозапуск, установка APK), `.bbc`, `.f`. Источник: `filedownloader` лаунчера Zeasn (в базе файл из 3 частей ровно 15 921 552 байта, размер вредоноса; там же следы загрузок 10,1 и 13,3 МБ), URL стёрты. Zeasn отключён. Нужно подтверждение на `pm uninstall` и удаление карантина.
- Adups FOTA (`/system/app/FotaUpdate`) перенесён в `/data/local/quarantine/FotaUpdate` 2026-10-29. Окончательное удаление карантина только с подтверждения.
- `mount -o remount,ro /` после записи отвечает «busy», раздел остаётся rw до перезагрузки.
- Почистить `enabled_accessibility_services` от несуществующих `com.leadschool.*`.
- `/system` заполнен на 86% (265 МБ свободно).
- Патч безопасности 2023-11: уязвимости ядра и системы закрыть не сможем, поэтому важно ограничить доступ по сети.
