# План

| ID | Задача | Исполнитель | Зависит от | Статус |
|---|---|---|---|---|
| T-001 | Полный бэкап разделов + sha256 (`tools/backup.sh`), готово в `backup/20261029-factory` (26 разделов, 2.5 ГБ, все OK) | lead | — | done |
| T-002 | Обезвредить вирусы (`tools/neutralize.sh`): disable-user + suspend, дропперы в `/data/local/quarantine`, настройки проверки APK восстановлены | lead | T-001 | done |
| T-003 | Окончательно удалить вредоносные пакеты (`pm uninstall`) и карантин, с подтверждения пользователя 2026-10-29 | lead | T-002 | done |
| T-004 | Ограничить adb: iptables пускает 5555 только с 192.168.1.7. В рантайме уже работает (`tools/adb-lock.sh`), осталось сделать постоянным через init-скрипт | firmware-dev → security-auditor | T-002, T-007 | in_progress |
| T-005 | Пользователь проверяет роутер: проброс 5555, DMZ, UPnP | пользователь | — | todo |
| T-006 | Инвентаризация 64 системных приложений: оставить / отключить / удалить, с обоснованием | architect | T-001 | todo |
| T-007 | Проверить, есть ли AVB/dm-verity и возможен ли remount `/system` (без изменений) | firmware-dev | T-001 | todo |
| T-008 | Отключить лишние системные приложения через `pm disable-user` по списку T-006 | firmware-dev | T-006 | todo |
| T-009 | Создать проект `launcher/` (Kotlin, Compose for TV, minSdk 29, armeabi-v7a) | backend-dev | Android Studio | todo |
| T-010 | Изучить API `com.hisilicon.tv.service` для меню картинки и звука (dex-дамп из бэкапа) | architect | T-001 | todo |
| T-011 | Лаунчер: сетка приложений, источники входа, настройки | backend-dev → qa-tester | T-009, T-010 | todo |
| T-012 | Встроить лаунчер в `/system/priv-app`, снять Zeasn | firmware-dev | T-007, T-011 | todo |
| T-013 | Свои logo, bootanimation, bootmusic | firmware-dev | T-007 | todo |

## Бэклог техдолга
- Почистить `enabled_accessibility_services` от несуществующих `com.leadschool.*`.
- `/system` заполнен на 87%.
- Патч безопасности 2023-11: уязвимости ядра и системы закрыть не сможем, поэтому важно ограничить доступ по сети.
