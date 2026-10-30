# API HiSilicon/Hikeen для лаунчера (T-010, 2026-10-30)

Источник: `dexdump` `/system/framework/HitvShare.jar`, `HitvManager.jar`, `HiTvPlayer.apk`, `HkTvSettings.apk`. Сигнатуры точные, поведение на ТВ ещё не проверено (см. «Неизвестно»).

## Подключение
- `<uses-library android:name="com.hisilicon.android.tvapi" android:required="false"/>` = `/system/framework/HitvShare.jar`. Также объявлена `com.hisilicon.dtv`.
- `HitvManager.jar` (`com.hisilicon.android.tv.*`, `com.vendor.vnaudiomanager.*`) не объявлен библиотекой в `/system/etc/permissions` (проверить, как его получает HiTvPlayer: `<uses-library>` или BOOTCLASSPATH).
- Сборка: заглушки только нужных классов в `compileOnly` (без dex2jar). На устройствах без библиотеки ловить `ClassNotFoundException`/`NoClassDefFoundError`.
- Точка входа: `HitvManager.getInstance()`, далее `getPicture()`, `getAudio()`, `getSourceManager()`, `getSystemSetting()`.

## Картинка: `tvapi.Picture`
`get/setBrightness`, `Contrast`, `Saturation`, `Hue`, `Sharpness`, `Backlight` (int), `get/setPictureMode(int)`, `enableBacklight(boolean)`.
`EnumPictureMode`: STANDARD 0, DYNAMIC 1, SOFTNESS 2, USER 3, VIVID 4, NATURAL 5, SPORTS 6, ECO 7.

## Звук: `tvapi.Audio`
`getVolume(int)`, `setVolume(int,int)`, `getMute(int)`, `setMute(int,boolean)`, `get/setSoundMode(int)`. Смысл первого аргумента (тип громкости) выяснить.
`EnumSoundMode`: STANDARD 0, NEWS 1, MUSIC 2, MOVIE 3, SPORTS 4, USER 5, 4K 6, HANK 7, DESK 8.
Выход звука: `VnAudioManager.get/setMediaOutDevice(int)`, `get/setPortOutputMode`.

## Источники: `tvapi.SourceManager` и `tv.TvSourceManager`
`tvapi`: `getSourceList()`, `getAvailSourceList()` (ArrayList), `selectSource(int,int)`, `selectSource(int,int,int)`, `deselectSource(int,boolean)`, `getCurSourceId(int)`, `getSelectSourceId()`, `getSignalStatus()`, `setVideoDisplay(Surface)`.
`tv.TvSourceManager.getInstance()`: те же методы, списки `int[]`, плюс `getPreSourceId`, `setFullWindow`. HiTvPlayer вызывает именно его.
`EnumSourceIndex`: ATV 0, DVBC 1, DTMB 2, CVBS1-3 3-5, VGA 6, YPBPR1-2 7-8, HDMI1-4 9-12, MEDIA 13-14, SCART1-2 15-16, AUTO 17, DVBT 18, ATSC 19, DVBS 20, ISDBT 21.

## Готовые экраны вендора (запасной путь)
- Меню картинки/звука: intent `com.hikeen.action.tvsettings` или `com.hikeen.tvsettings/.menu.SettingsActivity`.
- Полноэкранный ТВ/HDMI: `com.hisilicon.tvui/.MainActivity`. Каким extra передать источник, не найдено: похоже, он читает текущий источник, то есть сначала `selectSource`, потом запуск activity.
- Сканирование каналов: `com.hikeen.action.scanChannel`, `showEPG`, `channelEdit`.

## Прочее
- `SystemSetting.UpdateBootLogo(String)`, `setBootSource`: пригодится для T-013. Не вызывать без проверки, логотип пишется в раздел.
- `EnumFactoryWolMode`: Wake-on-LAN в HAL есть, можно изучить отдельно.
- Класс `Factory` не использовать: он меняет заводские настройки панели.

## Неизвестно, проверить на ТВ первым делом
1. Работает ли `HitvManager.getInstance()` из обычного приложения или нужен `/system/priv-app` и системный UID.
2. Диапазоны значений `setBrightness` и других (ожидается 0-100).
3. Аргументы `selectSource(a,b)` и `setVolume(a,b)`.
4. Показывает ли `com.hisilicon.tvui/.MainActivity` выбранный источник без лишних действий.

## Результат зонда T-011 (2026-10-30, обычное приложение `com.hexdevop.launcher`, targetSdk 37)
Пп. 1-4 из списка выше закрыты отрицательно:
1. `HitvManager.getInstance()` (класс `com.hisilicon.android.tvapi.HitvManager`, не `tv.*`) из обычного приложения недоступен. С `<uses-library ... required=false>` класс находится, но падает `UnsatisfiedLinkError: dlopen failed: library "/system/lib/libhi_tv_jni.so" ... is not accessible for the namespace "clns-4"`. Нативная библиотека закрыта linker-namespace для не-системных приложений. Любые вызовы Picture/Audio/SourceManager дают null. `com.hisilicon.android.tv.TvSourceManager` из HitvManager.jar приложению вообще не виден (`ClassNotFoundException`). Диапазоны и аргументы так и не получены.
2. `com.hisilicon.tvui/.MainActivity` из обычного приложения: `SecurityException`, требуется `com.hisilicon.permission.COMMON_PERMISSION` (`signature|privileged`, объявлено в `com.hikeen.hkhotelmenu`). Пока лаунчер не в `/system/priv-app` с привилегированным разрешением, запуск ТВ/входов невозможен.
3. `com.hikeen.action.tvsettings` (`com.hikeen.tvsettings/.menu.SettingsActivity`) запускается, но сразу делает `onDestroy` (меню-оверлей, ожидает вызова из ТВ-окна). Как запасной путь не годится.
4. TvInputManager (TIF) пуст: `dumpsys tv_input` без входов. Публичным API список входов не получить.
5. Работают из обычного приложения: `android.settings.WIFI_SETTINGS` -> `com.android.tv.settings/.connectivity.NetworkActivity`, `android.settings.SETTINGS` -> `.MainSettings`, `android.settings.SOUND_SETTINGS` -> `.device.displaysound.DisplaySoundActivity`.
Вывод для T-012: если лаунчер станет привилегированным (priv-app, сигнатура/разрешение, возможно `android:sharedUserId`/namespace), зонд повторить: сначала `HitvManager.getInstance()`, потом `getBrightness`, `getAvailSourceList`, `selectSource`.

## Результат зонда из `/system/priv-app` (2026-10-30)
Лаунчер в `/system/priv-app/HexLauncher`, разрешение в `/system/etc/permissions/privapp-permissions-hexlauncher.xml`: `COMMON_PERMISSION` выдано, `HitvManager.getInstance()` работает.
- Значения: яркость/контраст/насыщенность/резкость 50, подсветка 80, `getPictureMode`=0, `getSoundMode`=0.
- `getSourceList`=[0, 18, 1, 3, 9, 10, 13]: ATV, DVBT, DVBC, CVBS1, HDMI1, HDMI2, MEDIA. `getAvailSourceList` пуст. `getSelectSourceId`=18, `getSignalStatus`=0.
- `getVolume(0)`=8, `getVolume(1)`=8, `getVolume(2)`=0, `getVolume(3)`=0.
Обновление: `adb install -r` поверх системного (тот же debug-ключ).

## Аргументы из байткода и ограничение обновления (T-011, 2026-10-30)
- `selectSource(id, 0)`: HiTvPlayer `HalApi.changeSource(I)V` зовёт `TvSourceManager.selectSource(id, 0)`; трёхаргументный вариант `(id, 0, x)`. Второй аргумент всегда 0 (окно). Затем `com.hisilicon.tvui/.MainActivity`.
- `Audio.setVolume/getVolume` ни HiTvPlayer, ни HkTvSettings не зовут. Громкость ведёт Android `AudioManager`: `STREAM_MUSIC` min 0, max 100, сейчас 8 = `Audio.getVolume(0)`. Лаунчер читает/пишет через `AudioManager`. `Audio.setMute(4, bool)` зовёт tvui (режим записи).
- Режим звука: `Audio.getSoundMode()/setSoundMode(int)` (HkTvSettings `AudioManagerImpl`).
- ВАЖНО: после `adb install -r` поверх `/system/priv-app/HexLauncher` пакет становится `UPDATED_SYSTEM_APP` с кодом в `/data/app/...`; флаг PRIVILEGED и разрешение COMMON_PERMISSION остаются, но `libhi_tv_jni.so` снова закрыт namespace (`clns-4`), `HitvManager.getInstance()` падает с `UnsatisfiedLinkError`. API работает только если APK лежит в `/system/priv-app`. Обновлять надо заменой файла в `/system` (firmware-dev) или так: `pm uninstall-system-updates` не поможет, нужен новый APK в /system.
