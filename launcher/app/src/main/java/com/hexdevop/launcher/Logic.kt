package com.hexdevop.launcher

const val YOUTUBE = "com.google.android.youtube.tv"

/** Стартовый фокус: последнее запущенное, иначе YouTube, иначе первая плитка. */
fun pickStartFocus(last: String?, available: List<String>): String? =
    last?.takeIf { it in available } ?: YOUTUBE.takeIf { it in available } ?: available.firstOrNull()

/** Источники для дока (EnumSourceIndex: ATV 0, DVBC 1, DTMB 2, CVBS1-3 3-5, HDMI1-4 9-12, DVBT 18, ATSC 19, DVBS 20, ISDBT 21). MEDIA 13 и прочее скрыто. */
fun filterInputs(ids: List<Int>): List<Int> =
    ids.filter { it in 0..5 || it in 9..12 || it in 18..21 }

fun isTvSource(id: Int) = id in 0..2 || id in 18..21

/** Кнопки входов: (куда переключать, название). ТВ-источники сливаются в одну кнопку, затем HDMI, затем AV. */
// без ТВ-каналов: антенны нет, а tvui без каналов сам открывает мастер сканирования
fun dockInputs(ids: List<Int>): List<Pair<Int, String>> {
    val ok = filterInputs(ids)
    val hdmi = ok.filter { it in 9..12 }.map { it to "HDMI ${it - 8}" }
    val av = ok.filter { it in 3..5 }.map { it to if (it == 3) "AV" else "AV ${it - 2}" }
    return hdmi + av
}

fun sourceCaption(id: Int) = when {
    isTvSource(id) -> "Источник: ТВ"
    id in 9..12 -> "Источник: HDMI"
    else -> "Источник: AV"
}

/** Шаг значения с границами. */
fun stepValue(value: Int, delta: Int, min: Int = 0, max: Int = 100): Int =
    (value + delta).coerceIn(min, max)
