package com.hexdevop.launcher

const val YOUTUBE = "com.google.android.youtube.tv"

/** Стартовый фокус: последнее запущенное, иначе YouTube, иначе первая плитка. */
fun pickStartFocus(last: String?, available: List<String>): String? =
    last?.takeIf { it in available } ?: YOUTUBE.takeIf { it in available } ?: available.firstOrNull()

/** Только ТВ-источники и HDMI (EnumSourceIndex: ATV 0, DVBC 1, DTMB 2, HDMI1-4 9-12, DVBT 18, ATSC 19, DVBS 20, ISDBT 21). */
fun filterInputs(ids: List<Int>): List<Int> =
    ids.filter { it in 9..12 || it in 0..2 || it in 18..21 }

/** Шаг значения с границами. */
fun stepValue(value: Int, delta: Int, min: Int = 0, max: Int = 100): Int =
    (value + delta).coerceIn(min, max)
