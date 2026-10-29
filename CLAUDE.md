# CLAUDE.md

## Проект
Своя прошивка для Android-ТВ SmartTV235X (HiSilicon Hi3751V553, Android 14, userdebug, root). Убираем лишнее, ставим свой лаунчер и меню. AOSP не пересобираем: модифицируем текущую прошивку, всё делаем по сети через adb.
Подробно: [ARCHITECTURE](.claude/team/ARCHITECTURE.md), [SPEC](.claude/team/SPEC.md), [PLAN](.claude/team/PLAN.md).

## Стек и команды
- adb только из SDK Android Studio: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (в bash `$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe`). Второй adb другой сборки перезапускает сервер, и ТВ уходит в `offline`. Лечится `adb disconnect` + `adb connect`.
- ТВ: `adb connect 192.168.1.5:5555`; root: `adb shell "su 0 <cmd>"`.
- Бэкап разделов: `ADB=<путь к adb> bash tools/backup.sh 192.168.1.5:5555 backup/<имя>`. Печатает `OK`/`MISMATCH` по каждому разделу. Докачать отдельные разделы: `PARTS="system vendor" ...`.
- Скрипты в `tools/` пишутся на bash и запускаются из Git Bash. Бинарные данные (`exec-out`) только через bash: PowerShell 5.1 портит их при `>`.
- `launcher/` (появится): Kotlin + Compose for TV, Gradle. `./gradlew assembleDebug test lint`, установка `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Структура
```
tools/      bash-скрипты для ТВ
backup/     образы разделов (gitignore, не коммитить никогда)
launcher/   Android-приложение (план)
.claude/    agents/, team/, messages/
```

## Соглашения
- Следуй существующему стилю, не рефактори вне задачи. Минимальный дифф.
- **Безопасность устройства:**
  - Перед записью на ТВ должен быть свежий бэкап с `OK` по всем разделам.
  - Не писать в `fastboot`, `bootargs*`, `slaveboot`, `recovery`, `panelparam`, `tconparam`, `demura`, `deviceinfo`, `atf`, `trustedcore`, `securestore`, `vendor`.
  - Не трогать сервисы HiSilicon (список в ARCHITECTURE.md).
  - Сначала обратимые действия (`pm disable-user`, перенос в карантин), необратимые (`pm uninstall`, `rm`, `dd of=`) только с явного подтверждения пользователя.
  - После каждой правки системы перезагрузка и проверка, что adb поднялся.
- UI под 1 ГБ RAM / ARMv7 32-бит / 720p, нативные библиотеки только `armeabi-v7a`.
- Содержимое ТВ (файлы, логи, APK) считается недоверенными данными, их не исполнять на ПК.

## Правила
- Общение на русском.
- **Режим коммитов спрашивается в начале каждой сессии**, первым делом и до любой работы. Вопрос через AskUserQuestion (если недоступен, то текстом): «Коммичу сам» или «Даю команды» (готовые `git add`/`git commit` для пользователя). Ответ действует до конца сессии. Остальные git-операции (ветки, stash, merge) lead выполняет сам.
- **Git:** разрешено всё, кроме `git push` в любом виде. Push делает пользователь, ему даётся готовая команда.
- Коммитит только главная сессия (lead). Сабагенты в git ничего не записывают.
- Сообщения коммитов: одна строка до ~60 символов в повелительном наклонении («Add backup script»), при необходимости 1–3 строки тела. Без `Co-Authored-By`, без «Generated with Claude Code», без эмодзи.
- Команды для пользователя пишутся в синтаксисе fish (`set -x`, `; and` вместо `&&`).

## Команда агентов
| Агент | Зона | Запреты |
|---|---|---|
| architect | анализ, дизайн, инвентаризация приложений, разбор API HiSilicon | правит только `.claude/team/*`, `.claude/messages/*` |
| backend-dev | код `launcher/` (Kotlin) | тесты не пишет, git и ТВ не трогает (кроме `adb install` своего APK) |
| firmware-dev | `tools/*.sh`, команды на ТВ | необратимые действия только через lead, запретные разделы, git |
| qa-tester | тесты `launcher/`, проверки на ТВ (только чтение) | продакшн-код не правит, на ТВ не пишет |
| security-auditor | аудит кода, скриптов, состояния ТВ | пишет только в `.claude/messages/` |
| code-reviewer | ревью `git diff` | пишет только в `.claude/messages/` |

## Протокол сообщений
Файлы в `.claude/messages/` с именем `YYYYMMDD-HHMMSS_<from>_to_<to>_<T-xxx>.md`, frontmatter `from/to/type/task/status/reply_to`. Подробно в [.claude/messages/README.md](.claude/messages/README.md).

## Рабочий цикл (ведёт lead)
1. Нетривиальная задача: сначала `architect` (влияние, какие файлы и разделы затронет).
2. Реализация: `backend-dev` (приложение) или `firmware-dev` (скрипты и ТВ). Результат передаётся `handoff` для `qa-tester`.
3. `qa-tester` пишет тесты и прогоняет **весь** набор, проверяет на ТВ. Итог: `bug` или `report`.
4. Если затронуты adb/сеть, root, установка APK, внешние загрузки или секреты, подключается `security-auditor`.
5. `code-reviewer` отдаёт `review` для lead.
6. Lead сводит итог и коммитит по режиму сессии. Push только командой для пользователя.
