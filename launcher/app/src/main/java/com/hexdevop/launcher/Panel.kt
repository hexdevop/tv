package com.hexdevop.launcher

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.hexdevop.launcher.ui.theme.LauncherTheme
import com.hexdevop.launcher.ui.theme.Manrope
import com.hexdevop.launcher.ui.theme.Unbounded

/** Строка панели: число 0..100 (get/set) или перебор режимов (names, значение = индекс). */
class Param(
    val label: String,
    val names: List<String>? = null,
    val step: Int = 2,
    val get: () -> Int?,
    val set: (Int) -> Unit,
)

private val PictureModes = listOf(
    "Стандартный", "Динамичный", "Мягкий", "Пользовательский", "Яркий", "Естественный", "Спорт", "Эко",
)
private val SoundModes = listOf("Стандартный", "Новости", "Музыка", "Кино", "Спорт", "Пользовательский", "4K")

private fun pictureParams() = listOf(
    Param("Режим", PictureModes, get = { TvApi.getPicture("PictureMode") }, set = { TvApi.setPicture("PictureMode", it) }),
    num("Яркость", "Brightness"),
    num("Контраст", "Contrast"),
    num("Цвет", "Saturation"),
    num("Резкость", "Sharpness"),
    num("Подсветка", "Backlight"),
)

private fun num(label: String, api: String) =
    Param(label, get = { TvApi.getPicture(api) }, set = { TvApi.setPicture(api, it) })

private fun soundParams(c: Context) = listOf(
    Param("Режим", SoundModes, get = { TvApi.getSoundMode() }, set = { TvApi.setSoundMode(it) }),
    Param("Громкость", step = 1, get = { TvApi.getVolume(c) }, set = { TvApi.setVolume(c, it) }),
)

/** Нижняя панель поверх текущего изображения (прозрачная тема), Назад закрывает. extra "kind": picture | sound. */
class PanelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pic = intent.getStringExtra("kind") != "sound"
        setContent {
            LauncherTheme {
                Panel(if (pic) "Картинка" else "Звук", remember { if (pic) pictureParams() else soundParams(this) })
            }
        }
    }
}

private val Blue = Color(0xFF6FB7FF)

@Composable
private fun Panel(title: String, params: List<Param>) {
    var sel by remember { mutableIntStateOf(if (params.size > 1) 1 else 0) }
    val vals = remember { params.map { mutableIntStateOf(0) } }
    var ready by remember { mutableStateOf(false) }
    // TvApi/AudioManager: binder-вызовы только вне главного потока
    val io = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { io.shutdown() } }
    LaunchedEffect(Unit) {
        val init = withContext(Dispatchers.IO) { params.map { it.get() } }
        init.forEachIndexed { i, x -> vals[i].intValue = (x ?: 0).coerceIn(0, (params[i].names?.size ?: 101) - 1) }
        ready = true
    }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    val p = params[sel]
    var v by vals[sel]
    val n = p.names?.size
    // режим: тики показывают положение в списке
    val frac = if (n != null) (v + 1f) / n else v / 100f
    fun change(d: Int) {
        if (!ready) return
        val nv = if (n != null) stepValue(v, d, 0, n - 1) else stepValue(v, d * p.step)
        if (nv != v) {
            v = nv
            io.execute { runCatching { p.set(nv) } }
        }
    }
    Box(
        Modifier.fillMaxSize().focusRequester(fr).focusable().onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (it.key) {
                Key.DirectionLeft -> change(-1)
                Key.DirectionRight -> change(1)
                Key.DirectionUp -> sel = (sel - 1).coerceAtLeast(0)
                Key.DirectionDown -> sel = (sel + 1).coerceAtMost(params.size - 1)
                else -> return@onPreviewKeyEvent false
            }
            true
        },
    ) {
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(250.dp).background(Color(0xFF06070A))
                .padding(48.dp, 26.dp, 48.dp, 30.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Column(Modifier.width(300.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color(0xFFF4F2EE), fontFamily = Unbounded, fontWeight = FontWeight.Medium, fontSize = 26.sp)
                Text(
                    if (n != null) "${v + 1}" else "$v", color = Blue, fontFamily = Unbounded, fontWeight = FontWeight.Light,
                    fontSize = 96.sp, lineHeight = 96.sp, letterSpacing = (-2.88).sp, maxLines = 1,
                )
                Text(
                    p.names?.get(v) ?: p.label, color = Color(0xFFF4F2EE), fontFamily = Manrope,
                    fontWeight = FontWeight.Bold, fontSize = 26.sp, maxLines = 1,
                )
            }
            Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    params.forEachIndexed { i, q ->
                        val on = i == sel
                        Box(
                            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(28.dp))
                                .background(if (on) Color(0xFFF4F2EE) else Color(0xFF191B21)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                q.label, color = if (on) Color(0xFF0B0C0F) else Color(0xFFECE9E3), fontFamily = Manrope,
                                fontWeight = FontWeight.Bold, fontSize = 21.sp, maxLines = 1, softWrap = false,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(74.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                    for (i in 0 until 40) {
                        Box(
                            Modifier.weight(1f).height((22 + i * 1.3f).dp).clip(RoundedCornerShape(3.dp))
                                .background(if ((i + 1) / 40f <= frac + 1e-4f) Blue else Color(0xFF23262D)),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (t in listOf("Влево: меньше", "Вправо: больше"))
                        Text(t, color = Color(0xFFD6D3CD), fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 22.sp)
                }
            }
        }
    }
}
