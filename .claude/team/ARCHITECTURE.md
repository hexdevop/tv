# Архитектура (как есть, 2026-10-29)

## Цель проекта
Своя «прошивка» для ТВ: вырезать лишнее, свой лаунчер, меню и дизайн. Без пересборки AOSP: BSP HiSilicon закрыт, поэтому модифицируем текущую прошивку. Все работы ведём по сети через adb, без физического доступа.

## Репозиторий
```
E:\tv
├── CLAUDE.md
├── tools/            # shell-скрипты для работы с ТВ по adb (bash, запуск из Git Bash/WSL)
│   └── backup.sh     # дамп всех разделов + sha256
├── backup/           # образы разделов (в .gitignore, НЕ коммитить: ключи DRM, серийник)
├── launcher/         # (план) Android-приложение: лаунчер + меню, Kotlin + Compose for TV
└── .claude/          # команда агентов, план, сообщения
```
Кода приложения пока нет. Android Studio у пользователя в процессе установки.

## Устройство
| Параметр | Значение |
|---|---|
| Модель | `SmartTV235X`, board `cs2352p586`, devicetype `CS2352U_Noah5_SSmart_UZ_SSmart` |
| SoC | HiSilicon Hi3751V553 (`bigfish`), 4× ARMv7, **только 32-бит** (`armeabi-v7a`) |
| RAM | 1 ГБ (+ swap 600 МБ), обычно свободно ~200 МБ |
| Экран | 1280×720, density 160 |
| ОС | Android 14 (SDK 34), patch 2023-11-01, ядро 5.10.66 |
| Сборка | `userdebug`, `dev-keys`, `ro.debuggable=1`, есть `/system/xbin/su` (root:shell, setuid) |
| adb | TCP 5555, `persist.adb.tcp.port=5555`, **без авторизации** (`ro.adb.secure` пуст) |
| Сеть | eth0 `192.168.1.5`, wlan0 `192.168.1.6`; ПК разработчика `192.168.1.7` |
| Платформа UI | Hikeen + Zeasn (лаунчер `com.zeasn.launcher`, Noah5) |

## Разметка eMMC (7.2 ГБ, без dynamic partitions и A/B)
`/dev/block/platform/*/by-name/`:

| Раздел | Блок | Назначение | Можно трогать |
|---|---|---|---|
| fastboot | p1 | загрузчик | **НЕТ** |
| bootargs / bootargsbak | p2/p3 | параметры загрузки | **НЕТ** |
| slaveboot | p4 | | **НЕТ** |
| recovery | p5 | аварийный выход | **НЕТ** (наш путь восстановления) |
| panelparam / tconparam / demura | p6/p18/p19 | калибровка панели, уникальна | **НЕТ** |
| deviceinfo | p7 | серийник/MAC | **НЕТ** |
| logo | p8 | загрузочный логотип | да |
| boot | p9 | ядро + ramdisk | осторожно |
| dtbo | p10 | | нет |
| atf / trustedcore | p11/p12 | TrustZone | **НЕТ** |
| securestore | p13 | ключи DRM | **НЕТ** |
| versioninfo | p14 | | нет |
| misc | p15 | | нет |
| bootmusic / bootmusicsec | p16/p17 | звук загрузки | да |
| system | p20 | `/`, ext4 1.7 ГБ, занято 87% | да (основная цель) |
| vendor | p21 | HAL HiSilicon | **нет** (кроме чтения) |
| atv | p22 | | нет |
| log | p23 | | — |
| cache | p24 | | — |
| ramdisk | p25 | | осторожно |
| userdata | p26 | `/data` | — |

Verified boot (AVB) по getprop не обнаружен.

## Что нельзя ломать (иначе пропадёт картинка, звук или каналы)
`vendor.hisilicon.hardware.*`, `com.hisilicon.tv.service`, `pqserver`, `hiavplayer`, `hiaoservice`, `virtualkeypad`, `android_ir_user` (пульт), `com.hisilicon.rmservice`, `com.hisilicon.dlna.dmr`.

## Известные проблемы
- **Заражение (обнаружено 2026-10-29).** 9 вредоносных пакетов от нескольких ADB-ботнетов, установлены 2026-06…08: `a.b.c`, `com.android.core.process`, `com.android.g6tech`, `com.android.media.module.services`, `com.android.server.ambient`, `com.android.system.service`, `com.google.android.apps.status`, `com.huawei.qx`, `com.imjustafriendlyguy.firmware`. Дропперы лежали в `/data/local/tmp` (`.d`, `.z`, `.s.apk`, `monkeapp888`, `nc`, `rxwp`). Серверы управления: 205.237.110.243:21998, 124.198.131.252:6001, 107.151.141.9:10003, 66.80.1.92:10001, 185.74.7.x. `/system` чистый. Статус чистки см. в PLAN.md.
- adb без авторизации на всех интерфейсах. Это сама причина заражения. Временное ограничение: `tools/adb-lock.sh` (iptables, до перезагрузки). После каждой перезагрузки ТВ запускать заново.
- SELinux `permissive` задан прошивкой (`androidboot.selinux=permissive` в cmdline), это не след вредоноса. Enforcing не включать без проверки HAL HiSilicon.
- `/system` заполнен на 87%: при встраивании своих APK освобождать место удалением.
- В `enabled_accessibility_services` прописаны несуществующие `com.leadschool.*`.

## Как работать
- adb: `adb connect 192.168.1.5:5555` (ПК подключён по проводу).
- root-команда: `adb shell "su 0 <cmd>"`.
- Бинарный вывод снимать через `adb exec-out` в bash. Перенаправление `>` в PowerShell 5.1 портит бинарные данные.
- Бэкап: `ADB=<путь к adb> bash tools/backup.sh 192.168.1.5:5555 backup/<имя>`.
- Тестов и линтеров пока нет: появятся вместе с проектом `launcher/` (Gradle: `./gradlew test lint`).
