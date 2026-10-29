# План

| ID | Задача | Исполнитель | Зависит от | Статус |
|---|---|---|---|---|
| T-001 | Полный бэкап разделов + sha256 (`tools/backup.sh`), готово в `backup/20261029-factory` (26 разделов, 2.5 ГБ, все OK) | lead | — | done |
| T-002 | Обезвредить вирусы (`tools/neutralize.sh`): disable-user + suspend, дропперы в `/data/local/quarantine`, настройки проверки APK восстановлены | lead | T-001 | done |
| T-003 | Окончательно удалить вредоносные пакеты (`pm uninstall`) и карантин, с подтверждения пользователя 2026-10-29 | lead | T-002 | done |
| T-004 | Ограничить adb: iptables пускает 5555 только с 192.168.1.7. Постоянно через `/system/etc/init/adb-lock.rc` (копия `tools/adb-lock.rc`, oneshot-сервис `adb-lock` с seclabel `u:r:su:s0`, перезапуск `setprop ctl.start adb-lock`). Проверено после reboot 2026-10-29 | firmware-dev → security-auditor | T-002, T-007 | done |
| T-005 | Пользователь проверяет роутер: проброс 5555, DMZ, UPnP. Отложено: нет пароля от TP-Link XX530v, сеть решили не трогать | пользователь | — | blocked |
| T-006 | Инвентаризация 111 системных приложений: оставить / отключить / удалить, с обоснованием (`.claude/team/APPS.md`) | architect | T-001 | done |
| T-007 | Проверить, есть ли AVB/dm-verity и возможен ли remount `/system` (без изменений). Итог: AVB и verity нет (нет vbmeta, `root=/dev/mmcblk0p20` напрямую, в fstab нет `verify`/`avb`), ext4 без `shared_blocks`, SELinux permissive, свободно 265 МБ (system) и 155 МБ (vendor). Remount rw должен работать, сам remount не пробовали: проверим при первой записи (T-004) | lead | T-001 | done |
| T-008 | Отключить лишние системные приложения через `pm disable-user` по списку T-006 (`tools/disable-apps.sh`, 19 пакетов) | firmware-dev | T-006 | done |
| T-009 | Создать проект `launcher/` (Kotlin, Compose for TV, minSdk 29). Шаблон собран и запущен на ТВ (PSS ~57 МБ, холодный старт ~8 с) | backend-dev | Android Studio | done |
| T-010 | Изучить API `com.hisilicon.tv.service` для меню картинки и звука (dex-дамп из бэкапа) | architect | T-001 | todo |
| T-011 | Лаунчер: сетка приложений, источники входа, настройки | backend-dev → qa-tester | T-009, T-010 | todo |
| T-012 | Встроить лаунчер в `/system/priv-app`, снять Zeasn | firmware-dev | T-007, T-011 | todo |
| T-013 | Свои logo, bootanimation, bootmusic | firmware-dev | T-007 | todo |

## Бэклог техдолга
- Adups FOTA (`/system/app/FotaUpdate`) перенесён в `/data/local/quarantine/FotaUpdate` 2026-10-29. Окончательное удаление карантина только с подтверждения.
- `mount -o remount,ro /` после записи отвечает «busy», раздел остаётся rw до перезагрузки.
- Почистить `enabled_accessibility_services` от несуществующих `com.leadschool.*`.
- `/system` заполнен на 86% (265 МБ свободно).
- Патч безопасности 2023-11: уязвимости ядра и системы закрыть не сможем, поэтому важно ограничить доступ по сети.
