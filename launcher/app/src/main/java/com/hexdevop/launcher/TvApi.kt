package com.hexdevop.launcher

import android.content.Context
import android.media.AudioManager
import android.util.Log

/**
 * Тонкая обёртка над com.hisilicon.android.tvapi (рефлексия, работает только у привилегированного приложения).
 * Без API все чтения возвращают null, записи молча ничего не делают.
 * Громкость: Audio.getVolume(0) совпадает с STREAM_MUSIC (0..100), поэтому пишем через AudioManager.
 */
object TvApi {
    private const val TAG = "TvApi"
    private const val PKG = "com.hisilicon.android.tvapi.HitvManager"
    private val INT = Int::class.javaPrimitiveType!!

    private val tv: Any? by lazy {
        runCatching { Class.forName(PKG).getMethod("getInstance").invoke(null) }
            .onFailure { Log.w(TAG, "unavailable: $it") }.getOrNull()
    }
    private val picture by lazy { part("getPicture") }
    private val audio by lazy { part("getAudio") }
    private val source by lazy { part("getSourceManager") }

    private fun part(n: String) = runCatching { tv?.let { it.javaClass.getMethod(n).invoke(it) } }.getOrNull()

    private fun call(o: Any?, n: String, vararg a: Int): Any? = runCatching {
        o?.let { it.javaClass.getMethod(n, *Array(a.size) { INT }).invoke(it, *a.toTypedArray()) }
    }.onFailure { Log.w(TAG, "$n failed: ${it.cause ?: it}") }.getOrNull()

    val available get() = tv != null

    /** Параметры картинки: "Brightness", "Contrast", "Saturation", "Sharpness", "Backlight", "PictureMode"; 0..100, режим 0..7. */
    fun getPicture(name: String): Int? = call(picture, "get$name") as? Int
    fun setPicture(name: String, v: Int) {
        call(picture, "set$name", v)
    }

    /** Режим звука: EnumSoundMode. */
    fun getSoundMode(): Int? = call(audio, "getSoundMode") as? Int
    fun setSoundMode(v: Int) {
        call(audio, "setSoundMode", v)
    }

    fun getVolume(c: Context): Int =
        (c.getSystemService(Context.AUDIO_SERVICE) as AudioManager).getStreamVolume(AudioManager.STREAM_MUSIC)

    fun setVolume(c: Context, v: Int) =
        (c.getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)

    /** EnumSourceIndex доступных на этом ТВ входов. */
    @Suppress("UNCHECKED_CAST")
    fun sourceList(): List<Int> = (call(source, "getSourceList") as? List<Int>).orEmpty()

    fun currentSource(): Int? = call(source, "getSelectSourceId") as? Int

    /** selectSource(id, window=0): так вызывает HiTvPlayer (HalApi.changeSource). */
    fun selectSource(id: Int) {
        call(source, "selectSource", id, 0)
    }
}
