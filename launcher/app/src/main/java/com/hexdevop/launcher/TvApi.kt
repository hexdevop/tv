package com.hexdevop.launcher

import android.util.Log

/** Только чтение: доступен ли API HiSilicon и что он возвращает (T-012). */
fun probeTvApi() {
    runCatching {
        val c = Class.forName("com.hisilicon.android.tvapi.HitvManager")
        val tv = c.getMethod("getInstance").invoke(null)
        fun part(n: String) = c.getMethod(n).invoke(tv)
        fun get(o: Any?, n: String) = runCatching { o!!.javaClass.getMethod(n).invoke(o) }.fold({ "$it" }, { "ERR ${it.cause ?: it}" })
        val p = part("getPicture")
        for (n in listOf("getBrightness", "getContrast", "getSaturation", "getSharpness", "getBacklight", "getPictureMode"))
            Log.i("TvApi", "$n=${get(p, n)}")
        val s = part("getSourceManager")
        for (n in listOf("getSourceList", "getAvailSourceList", "getSelectSourceId", "getSignalStatus"))
            Log.i("TvApi", "$n=${get(s, n)}")
        val a = part("getAudio")
        Log.i("TvApi", "getSoundMode=${get(a, "getSoundMode")}")
        for (i in 0..3) Log.i("TvApi", "getVolume($i)=${runCatching { a.javaClass.getMethod("getVolume", Int::class.java).invoke(a, i) }.getOrNull()}")
    }.onFailure { Log.e("TvApi", "unavailable", it) }
}
