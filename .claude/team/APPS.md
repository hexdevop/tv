# Инвентаризация системных приложений (T-006, 2026-10-29)

Источник: `.claude/team/data/system-packages.txt` (снимок `pm list packages -s`, формат `пакет|apk|on/off|run`). Все пакеты сейчас `on`. В PLAN было «64», фактически 111 записей.
Назначение определено по имени пакета и пути APK, без разбора dex. Где не уверен, написано «предп.».

YouTube, Allplay и TelecomTV в списке нет: это пользовательские приложения в `/data/app`. Их зависимости среди системных: WebView, GMS/GSF (YouTube: вход и обновления), `com.android.providers.tv` (каналы ТВ-приложений), Widevine/DRM (в `vendor`, не приложение), сеть.

Решения:
- **keep** — не трогать.
- **disable** — `pm disable-user --user 0`, откат `pm enable <пакет>`. В какой волне, см. наборы ниже.
- **remove-later** — инертно (оверлеи), отключать смысла нет; убрать из `/system` при освобождении места (T-012), только с подтверждения.
- **ask-user** — решает пользователь.

## Ядро Android

| Пакет | Что это | Решение | Риск, если отключить | Завязан на / от него зависит |
|---|---|---|---|---|
| android | framework-res | keep | система не загрузится | всё |
| android.ext.services | ExtServices (apex), ранжирование уведомлений, autofill | keep | сбои уведомлений и системных сервисов | system_server |
| com.android.adservices.api | AdServices (apex) | keep | ошибки в логах; из apex не удалить | apex adservices |
| com.android.ext.adservices.api | AdExtServices (apex) | keep | то же | apex extservices |
| com.android.sdksandbox | SDK Runtime (apex) | keep | то же | adservices |
| com.android.federatedcompute.services | Federated Compute (apex) | keep | инертен, выигрыша нет | apex ondevicepersonalization |
| com.android.ondevicepersonalization.services | On-device personalization (apex) | keep | инертен | то же |
| com.android.backupconfirm | диалог подтверждения `adb backup` | keep | не будет работать `adb backup/restore` | adb |
| com.android.localtransport | транспорт резервного копирования | keep | не будет работать `adb backup` | BackupManager |
| com.android.sharedstoragebackup | бэкап общей памяти | keep | то же | BackupManager |
| com.android.wallpaperbackup | бэкап обоев | disable | нет | BackupManager |
| com.android.bips | встроенный сервис печати | disable | нет (печать на ТВ не нужна) | PrintManager |
| com.android.bluetoothmidiservice | Bluetooth MIDI | disable | нет | Bluetooth |
| com.android.bluetooth | стек Bluetooth (apex) | keep | **пропадут BT-пульт, BT-аудио** | BT-пульты, наушники |
| com.android.bookmarkprovider | провайдер закладок (заглушка) | disable | нет | — |
| com.android.cameraextensions | прокси расширений камеры | disable | нет (камеры нет) | CameraService |
| com.android.captiveportallogin | вход в Wi-Fi с порталом | keep | не войти в «гостевые» Wi-Fi | NetworkStack |
| com.android.companiondevicemanager | сопряжение компаньон-устройств | keep | может сломаться сопряжение пультов/телефонов | Google TV Remote, BT |
| com.android.connectivity.resources | ресурсы Connectivity (apex) | keep | **сеть** | tethering apex |
| com.android.credentialmanager | Credential Manager UI | keep | ошибки входа в приложениях | GMS, приложения |
| com.android.devicelockcontroller | DeviceLock для кредитных устройств (apex) | keep | инертен, выигрыша нет | apex devicelock |
| com.android.dynsystem | DSU, установка GSI | disable | нет; лишний вектор атаки | — |
| com.android.externalstorage | провайдер USB/внешней памяти | keep | USB-флешки не видны | MediaProvider, файлы |
| com.android.health.connect.backuprestore | Health Connect backup (apex) | keep | известны падения PermissionController | apex healthfitness |
| com.android.healthconnect.controller | Health Connect UI (apex) | keep | то же | PermissionController |
| com.android.hotspot2.osulogin | Passpoint OSU (wifi apex) | keep | инертен | Wi-Fi |
| com.android.wifi.dialog | диалоги Wi-Fi (apex) | keep | **Wi-Fi** | Wi-Fi |
| com.android.wifi.resources | ресурсы Wi-Fi (apex) | keep | **Wi-Fi** | Wi-Fi |
| com.android.inputdevices | раскладки клавиатур | keep | раскладки внешних клавиатур | InputManager |
| com.android.inputmethod.latin | клавиатура LatinIME (run) | keep | **нечем вводить текст** (поиск, пароль Wi-Fi) | все приложения, наш лаунчер |
| com.android.intentresolver | диалог выбора приложения | keep | падения при «Открыть с помощью» | система |
| com.android.keychain | хранилище сертификатов | keep | TLS-сертификаты, Wi-Fi EAP | сеть |
| com.android.location.fused | Fused Location | keep | GMS и приложения без локации | GMS |
| com.android.modulemetadata | метаданные mainline-модулей | keep | инертен | PackageManager |
| com.android.mtp | MTP (ТВ как устройство для ПК / камеры по USB) | disable | нет для ТВ | USB |
| com.android.nearby.halfsheet | Nearby UI (apex) | keep | инертен | tethering apex |
| com.android.networkstack.tethering | Tethering (apex) | keep | **сеть** | ConnectivityService |
| com.android.networkstack | NetworkStack (DHCP, проверка сети) | keep | **нет сети** | ConnectivityService |
| com.android.packageinstaller | установщик пакетов | keep | не поставить APK, в т.ч. наш лаунчер | PackageManager |
| com.android.pacprocessor | PAC-прокси | keep | прокси | сеть |
| com.android.proxyhandler | обработчик прокси | keep | прокси | сеть |
| com.android.permissioncontroller | разрешения (apex) | keep | **система нестабильна** | PackageManager |
| com.android.safetycenter.resources | ресурсы Safety Center (apex) | keep | инертен | permission apex |
| com.android.providers.blockednumber | заблокированные номера | keep | инертен, может ронять телефонные API | telephony |
| com.android.providers.downloads | DownloadManager | keep | не скачиваются обновления/APK, GMS | GMS, приложения |
| com.android.providers.downloads.ui | UI загрузок | keep | мелко | DownloadManager |
| com.android.providers.media.module | MediaProvider (apex, run) | keep | **медиа, USB, плееры** | все медиа |
| com.android.providers.media | MediaProvider legacy | keep | старые приложения | MediaProvider |
| com.android.providers.settings | SettingsProvider | keep | **система не загрузится** | всё |
| com.android.providers.tv | TvProvider (каналы, входы) | keep | **каналы и источники**, TelecomTV/ТВ-приложения | `com.hisilicon.tv.service`, TIF, наш лаунчер |
| com.android.providers.userdictionary | словарь клавиатуры | keep | клавиатура | LatinIME |
| com.android.rkpdapp | Remote Key Provisioning (apex) | keep | аттестация ключей, DRM-приложения | keystore |
| com.android.se | SecureElement (run) | disable | нет (NFC/SIM на ТВ нет) | — |
| com.android.settings.intelligence | поиск в настройках телефона | disable | нет (TvSettings его не использует, предп.) | Settings |
| com.android.shell | Shell (adb, bugreport) | keep | **может сломать adb** | adb |
| com.android.soundpicker | выбор звуков | keep | мелко | AudioManager |
| com.android.storagemanager | управление памятью | keep | мелко | Settings |
| com.android.systemui | SystemUI (run) | keep | **чёрный экран, громкость, уведомления** | всё |
| com.android.traceur | системная трассировка | disable | нет | — |
| com.android.tv.overlay.framework | TV-оверлей framework | keep | вид системы под ТВ | framework |
| com.android.tv.settings | TvSettings (сеть, система, приложения) | keep | **нет меню сети/системы**, пока нет своего | наш лаунчер (FR-005) |
| com.android.uwb.resources | ресурсы UWB (apex) | keep | инертен | apex uwb |
| com.android.virtualmachine.res | ресурсы AVF (apex) | keep | инертен | apex virt |
| com.android.webview | System WebView | keep | **Allplay, TelecomTV, вход в аккаунты** (предп. используют WebView) | приложения |
| com.android.chrome | Chrome | ask-user | нет браузера; ~100+ МБ RAM при запуске | WebView не зависит от Chrome |
| com.android.internal.display.cutout.emulation.corner | оверлей выреза экрана | remove-later | нет | — |
| com.android.internal.display.cutout.emulation.double | оверлей выреза экрана | remove-later | нет | — |
| com.android.internal.display.cutout.emulation.hole | оверлей выреза экрана | remove-later | нет | — |
| com.android.internal.display.cutout.emulation.tall | оверлей выреза экрана | remove-later | нет | — |
| com.android.internal.display.cutout.emulation.waterfall | оверлей выреза экрана | remove-later | нет | — |
| com.android.internal.systemui.navbar.gestural | оверлей навбара | remove-later | нет; перед удалением проверить `cmd overlay list`, какой активен | SystemUI |
| com.android.internal.systemui.navbar.gestural_extra_wide_back | оверлей навбара | remove-later | то же | SystemUI |
| com.android.internal.systemui.navbar.gestural_narrow_back | оверлей навбара | remove-later | то же | SystemUI |
| com.android.internal.systemui.navbar.gestural_wide_back | оверлей навбара | remove-later | то же | SystemUI |
| com.android.internal.systemui.navbar.threebutton | оверлей навбара | remove-later | то же (может быть активным) | SystemUI |
| com.android.internal.systemui.navbar.transparent | оверлей навбара | remove-later | то же | SystemUI |
| com.android.role.notes.enabled | оверлей роли «Заметки» | remove-later | нет | — |
| com.android.theme.font.notoserifsource | оверлей шрифта | remove-later | нет | — |

## HiSilicon / HAL-обвязка

| Пакет | Что это | Решение | Риск, если отключить | Завязан на / от него зависит |
|---|---|---|---|---|
| com.hisilicon.tv.service | HiTvService (run), TV-сервис платформы | keep | **картинка, звук, источники, каналы** (в запретном списке) | HAL `vendor.hisilicon.hardware.*`, Hikeen-меню, наш лаунчер (T-010) |
| com.hisilicon.rmservice | HiRMService (run), менеджер ресурсов (предп.) | keep | **в запретном списке** | HAL, плееры |
| com.hisilicon.dlna.dmr | DLNA Renderer (run) | keep | **в запретном списке** | сеть |
| com.hisilicon.tvui | HiTvPlayer: полноэкранный ТВ/HDMI-плеер (предп.) | keep | **не открываются HDMI/ТВ-тюнер** с текущего лаунчера | `com.hisilicon.tv.service`, Zeasn, HkSingleKey |
| com.hisilicon.dlna.dmp | DLNA Player (клиент) | disable | нет | — |
| com.hisilicon.miracast | Miracast (трансляция экрана) | ask-user | нет трансляции с телефона | Wi-Fi Direct |
| com.hisilicon.android.music | HkMusic, музыкальный плеер (run) | ask-user | нет плеера музыки с USB | MediaProvider |
| com.hisilicon.android.videoplayer | HkVideoPlayer, видео с USB | ask-user | нет плеера видео с USB | MediaProvider, `com.hikeen.mediaplayer` (предп.) |
| com.vendor.vnaudiomanager | VnAudioManager (run), аудио-менеджер вендора (предп.) | keep | **звук** (предп.: выходы, режимы) | HAL аудио, Hikeen-настройки |
| com.adups.fota | Adups FOTA, обновления по воздуху (run) | disable | нет; без него вендорский OTA не перезапишет наши изменения. Adups известен сбором данных | сеть |
| com.adups.fota.overlay | оверлей FOTA в `/vendor` | keep | инертен; `vendor` не трогаем | `com.adups.fota` |

## Hikeen (настройки ТВ, меню, factory, hotel)

| Пакет | Что это | Решение | Риск, если отключить | Завязан на / от него зависит |
|---|---|---|---|---|
| com.hikeen.tvsettings | HkTvSettings: картинка, звук, источники (предп.) | keep | **нет меню картинки/звука**, пока нет своего | `com.hisilicon.tv.service`, VnAudioManager |
| com.hikeen.menu | HkSingleKey: обработка «одиночных» кнопок пульта (Source, Menu, Picture) (предп.) | disable | **кнопки Source/Menu на пульте перестанут работать**, пока наш лаунчер их не перехватит | пульт, HkTvSettings, HiTvPlayer |
| com.hikeen.guide | HkGuide: мастер первой настройки или справка (не уверен) | disable | если мастер: при сбросе настроек не будет первичной настройки | Zeasn (предп.) |
| com.hikeen.hkhotelmenu | гостиничный режим (блокировка источника/громкости) | disable | если hotel mode где-то включён, его настройки перестанут применяться | `com.hisilicon.tv.service` |
| com.hikeen.tv.factorymenu | сервисное (заводское) меню: баланс белого, панель | keep | нет доступа к сервисному меню для ремонта; инертен, пока не вызвать кодом | `com.hisilicon.tv.service` |
| com.hikeen.cleanmaster | «очистка памяти» | disable | нет | — |
| com.hikeen.higallery | галерея фото с USB | ask-user | нет просмотра фото | MediaProvider |
| com.hikeen.mediaplayer | медиацентр USB (предп. оболочка над плеерами) | ask-user | нет USB-медиа | HkVideoPlayer, HkMusic, HiGallery |
| com.factory.factoryautomation | FactoryAutomationService (run): заводской автотест (предп.) | disable | предп. нет; может слушать сеть/UART для заводских команд. Проверить отдельно (что держит, `dumpsys package`) | `com.hisilicon.tv.service` (предп.) |
| com.dolphin.assistant | VoiceAssistant (run), голосовой помощник Dolphin | ask-user | кнопка микрофона на пульте может перестать работать | BT/голосовой пульт, Katniss? |
| com.dolphin.remote.server | DolphinsRemote: сервер для пульта-приложения на телефоне (предп.) | disable | нет пульта с телефона Dolphin; ИК-пульт не затрагивает (ИК идёт через `android_ir_user`) | сеть (открытый порт, предп.) |

## Zeasn (лаунчер, магазин)

| Пакет | Что это | Решение | Риск, если отключить | Завязан на / от него зависит |
|---|---|---|---|---|
| com.zeasn.launcher | ZeasnLauncher_Noah5, текущий HOME | disable | **без другого HOME — пустой экран после загрузки** | HOME, HiTvPlayer, HkSingleKey |
| zeasn.open.technical.tv.store | TvStore_Noah5, магазин Zeasn | ask-user | не обновятся приложения, поставленные из него (предп. Allplay/TelecomTV) | Zeasn launcher |

## Google

| Пакет | Что это | Решение | Риск, если отключить | Завязан на / от него зависит |
|---|---|---|---|---|
| com.google.android.gms | Google Play Services (run) | keep | **YouTube без входа/падает**, ломается Cast | YouTube, GSF, Play |
| com.google.android.gsf | Google Services Framework | keep | то же | GMS |
| com.google.android.ext.shared | общая библиотека Google | keep | падения GMS-приложений | GMS |
| com.google.android.tv.remote.service | AtvRemoteService (run): пульт Google TV с телефона, голос с BT-пультов | keep | приложение-пульт Google TV; возможно голос с BT-пульта | GMS, Katniss |
| com.android.vending | Play Market (`product/priv-app/Tubesky/Tubesky.apk`, 32.0.18). Подпись проверена lead 2026-10-29: оригинальный сертификат Google `F0:FD:6C:…:DB:83`, тот же, что у GMS. `Tubesky` — просто имя папки у вендора | keep | Allplay и TelecomTV установлены и обновляются через Play | GMS |
| com.google.android.katniss | голосовой поиск Android TV | ask-user | нет голосового поиска | GMS, кнопка микрофона |
| com.google.android.backdrop | заставка Backdrop; APK в `/data/app` (обновление или установлено отдельно) | ask-user | будет стандартная заставка | GMS |
| com.google.android.onetimeinitializer | одноразовая инициализация Google при первом запуске | ask-user | нет (уже отработал) | GMS |
| com.google.android.exoplayer2.demo | демо-приложение ExoPlayer | disable | нет | — |

## Итог по решениям

| Решение | Кол-во |
|---|---|
| keep | 66 |
| disable | 20 (14 в первом наборе, 6 после замены Zeasn) |
| remove-later | 13 |
| ask-user | 12 |
| **всего** | 111 |

## 1. Безопасный первый набор (`pm disable-user --user 0`)

Не участвуют в картинке, звуке, пульте, источниках, сети и настройках. Ничего из запретного списка ARCHITECTURE.md.

```
com.adups.fota
com.android.bips
com.android.bluetoothmidiservice
com.android.bookmarkprovider
com.android.cameraextensions
com.android.dynsystem
com.android.mtp
com.android.se
com.android.settings.intelligence
com.android.traceur
com.android.wallpaperbackup
com.google.android.exoplayer2.demo
com.hikeen.cleanmaster
com.hisilicon.dlna.dmp
```
Откат: `pm enable <пакет>` по тому же списку. Проверка после перезагрузки: картинка, HDMI, ТВ-тюнер, кнопки пульта, звук, Wi-Fi/Ethernet, YouTube, Allplay, TelecomTV, меню Hikeen.

## 2. Только после замены Zeasn нашим лаунчером (и с отдельной проверкой)

Отключать по одному с проверкой, после того как наш лаунчер стал HOME и умеет переключать источники и открывать меню:
```
com.zeasn.launcher            # только когда наш лаунчер — HOME по умолчанию
com.hikeen.menu               # когда наш лаунчер перехватывает Source/Menu с пульта
com.hikeen.guide              # сначала выяснить, что это
com.hikeen.hkhotelmenu        # сначала убедиться, что hotel mode выключен
com.factory.factoryautomation # сначала посмотреть, какие сервисы/порты держит
com.dolphin.remote.server     # отдельно проверить, что ИК- и BT-пульт работают
```
Позже, когда своё меню картинки/звука проверено, можно рассмотреть `com.hikeen.tvsettings` и `com.hisilicon.tvui`, но они пока **keep**: от них, вероятно, зависят входы и `com.hisilicon.tv.service`.

## 3. Открытые вопросы к пользователю

1. ~~Play Market в `Tubesky`~~: закрыт. Подпись оригинальная Google, Play оставляем: через него установлены Allplay и TelecomTV.
2. **Google-сервисы**: оставить GMS (нужно YouTube) или искать замену? Katniss, Backdrop, OneTimeInitializer можно отключить, если голосовой поиск и заставка Google не нужны.
3. **Голос**: пользуетесь ли кнопкой микрофона на пульте? От ответа зависят `com.dolphin.assistant` и `com.google.android.katniss`.
4. **USB-медиа**: смотрите ли фото/видео/музыку с флешки? (HkMusic, HkVideoPlayer, HiGallery, Hikeen MediaPlayer.)
5. **Miracast** (трансляция экрана телефона) нужен?
6. **Chrome** нужен как браузер? На 1 ГБ RAM лучше отключить.
7. **Магазин Zeasn**: Allplay и TelecomTV установлены через Play (`installerPackageName=com.android.vending`), магазин Zeasn для них не нужен. Нужен ли он пользователю сам по себе?
8. **Сервисное меню Hikeen** оставляем (по умолчанию да, на случай настройки панели)?
