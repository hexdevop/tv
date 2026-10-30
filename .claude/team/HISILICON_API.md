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
