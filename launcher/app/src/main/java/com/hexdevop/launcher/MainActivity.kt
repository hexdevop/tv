package com.hexdevop.launcher

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Text
import com.hexdevop.launcher.ui.theme.LauncherTheme
import com.hexdevop.launcher.ui.theme.Manrope
import com.hexdevop.launcher.ui.theme.Unbounded
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF06070A)
private val Fg = Color(0xFFF4F2EE)
private val Ink = Color(0xFFECE9E3)
private val Card = Color(0xFF15171C)
private val Dark = Color(0xFF0B0C0F)
private val Blue = Color(0xFF6FB7FF)

/** Плитка ряда приложений или чип дока. */
class Entry(
    val key: String,
    val label: String,
    val sub: String,
    val color: Color,
    val ink: Color,
    val icon: ImageBitmap?,
    val run: () -> Unit,
)

class AppInfo(val pkg: String, val label: String, val intent: Intent)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    private fun open(i: Intent, saveKey: String? = null) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            if (saveKey != null) prefs.edit().putString("last", saveKey).apply()
        } catch (_: Exception) {
        }
    }

    private fun icon(d: Drawable) = d.toBitmap(64, 64).asImageBitmap()

    private fun appEntries(onAll: () -> Unit): List<Entry> {
        val pm = packageManager
        val defs = listOf(
            arrayOf(YOUTUBE, "YouTube", "Смотреть видео", 0xFFE5261C, 0xFFFFFFFF),
            arrayOf("uz.telecom.telecomtv", "TelecomTV", "Телеканалы онлайн", 0xFF2368E8, 0xFFFFFFFF),
            arrayOf("com.android.chrome", "Chrome", "Браузер", 0xFFF2B632, 0xFF14120C),
            arrayOf("com.android.vending", "Play Маркет", "Новые приложения", 0xFF17B97A, 0xFF04140D),
            arrayOf("com.hikeen.mediaplayer", "Файлы", "Фото и видео с флешки", 0xFF7E3FE0, 0xFFFFFFFF),
        )
        val list = defs.mapNotNull { d ->
            val pkg = d[0] as String
            val intent = pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
            Entry(
                pkg, d[1] as String, d[2] as String, Color((d[3] as Long).toInt()), Color((d[4] as Long).toInt()),
                icon(pm.getApplicationIcon(pkg)),
            ) { open(intent, pkg) }
        }
        return list + Entry("all", "Все", "Все приложения на телевизоре", Color(0xFFE7E4DE), Color(0xFF14120C), null, onAll)
    }

    // tvapi и com.hisilicon.tvui/com.hikeen.tvsettings обычному приложению недоступны (см. HISILICON_API.md): Картинка и Звук ведут в com.android.tv.settings, ТВ заработает у привилегированного приложения (T-012).
    private fun dockEntries(): List<Entry> {
        fun e(key: String, label: String, sub: String, color: Color = Blue, run: () -> Unit) =
            Entry(key, label, sub, color, Dark, null, run)
        return listOf(
            e("tv", "ТВ-каналы", "Антенна и входы", Color(0xFFC9D2E0)) {
                open(Intent().setComponent(ComponentName("com.hisilicon.tvui", "com.hisilicon.tvui.MainActivity")))
            },
            e("picture", "Картинка", "Яркость, контраст, режим") { open(Intent(Settings.ACTION_DISPLAY_SETTINGS)) },
            e("sound", "Звук", "Громкость и режим") { open(Intent("android.settings.SOUND_SETTINGS")) },
            e("net", "Сеть", "Wi-Fi и кабель") { open(Intent(Settings.ACTION_WIFI_SETTINGS)) },
            e("settings", "Настройки", "Все настройки системы") { open(Intent(Settings.ACTION_SETTINGS)) },
        )
    }

    private fun allApps(): List<AppInfo> {
        val pm = packageManager
        return listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
            .flatMap { pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(it), 0) }
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val a = it.activityInfo
                AppInfo(a.packageName, it.loadLabel(pm).toString(), Intent().setComponent(ComponentName(a.packageName, a.name)))
            }
            .sortedBy { it.label.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread { probeTvApi() }.start()
        setContent {
            LauncherTheme {
                var showAll by remember { mutableStateOf(false) }
                val apps = remember { appEntries { showAll = true } }
                val dock = remember { dockEntries() }
                val all = remember { allApps() }
                var focus by remember {
                    mutableStateOf(pickStartFocus(prefs.getString("last", null), apps.map { it.key }) ?: "all")
                }
                if (showAll) {
                    BackHandler { showAll = false }
                    AllApps(all) { open(it.intent, it.pkg) }
                } else {
                    BackHandler { }
                    Home(apps, dock, focus) { focus = it }
                }
            }
        }
    }
}

@Composable
private fun Home(apps: List<Entry>, dock: List<Entry>, focus: String, onFocus: (String) -> Unit) {
    val cur = (apps + dock).firstOrNull { it.key == focus } ?: apps.first()
    val color by animateColorAsState(cur.color, tween(450), label = "eclipse")
    val reqs = remember { (apps + dock).associate { it.key to FocusRequester() } }
    // последний элемент каждого ряда: вверх/вниз возвращает на него, а не на ближайший по геометрии
    val last = remember { arrayOf(if (cur in apps) cur.key else apps.first().key, if (cur in dock) cur.key else dock.first().key) }
    val appKeys = remember { apps.map { it.key }.toSet() }
    val focused = remember<(String) -> Unit>(onFocus) {
        { k ->
            last[if (k in appKeys) 0 else 1] = k
            onFocus(k)
        }
    }
    LaunchedEffect(Unit) { reqs[focus]?.requestFocus() }
    fun Modifier.jump(dir: Key, to: Int) = onPreviewKeyEvent {
        val r = if (it.key == dir && it.type == KeyEventType.KeyDown) reqs[last[to]] else null
        r?.requestFocus() ?: false
    }
    Box(Modifier.fillMaxSize().background(Bg).drawBehind { eclipse(color) }) {
        Clock(Modifier.offset(48.dp, 38.dp))
        Column(Modifier.offset(48.dp, 148.dp).width(780.dp)) {
            Text(
                if (cur.key == "all") "Все приложения" else cur.label, color = Fg,
                fontFamily = Unbounded, fontWeight = FontWeight.Medium, fontSize = 80.sp,
                lineHeight = 83.sp, letterSpacing = (-2.4).sp, maxLines = 2,
            )
            Text(
                cur.sub, Modifier.padding(top = 16.dp), color = Color(0xFFD6D3CD),
                fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 26.sp, maxLines = 1,
            )
        }
        Row(
            Modifier.offset(48.dp, 402.dp).width(1184.dp).height(164.dp).jump(Key.DirectionDown, 1),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            apps.forEach { Tile(it, focus, reqs.getValue(it.key), focused) }
        }
        Row(
            Modifier.offset(48.dp, 594.dp).width(1184.dp).height(88.dp).jump(Key.DirectionUp, 0),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dock.forEach { Chip(it, focus, reqs.getValue(it.key), focused) }
        }
    }
}

private fun DrawScope.eclipse(c: Color) {
    val g = Offset(1000.dp.toPx(), 160.dp.toPx())
    val gr = 490.dp.toPx()
    drawCircle(
        Brush.radialGradient(
            0f to c.copy(alpha = .62f), .38f to c.copy(alpha = .2f), .68f to c.copy(alpha = 0f),
            center = g, radius = gr,
        ), gr, g,
    )
    // кромка затмения: градиент вместо blur-тени
    val rim = Offset(980.dp.toPx(), 160.dp.toPx())
    val rr = 236.dp.toPx()
    drawCircle(
        Brush.radialGradient(
            0f to c.copy(alpha = .85f), .8f to c.copy(alpha = .85f), 1f to c.copy(alpha = 0f),
            center = rim, radius = rr,
        ), rr, rim,
    )
    drawCircle(Bg, 200.dp.toPx(), Offset(980.dp.toPx(), 170.dp.toPx()))
}

@Composable
private fun Clock(modifier: Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                now = System.currentTimeMillis()
            }
        }
        ContextCompat.registerReceiver(ctx, r, IntentFilter(Intent.ACTION_TIME_TICK), ContextCompat.RECEIVER_NOT_EXPORTED)
        now = System.currentTimeMillis()
        onDispose { ctx.unregisterReceiver(r) }
    }
    val ru = Locale("ru")
    val time = remember(now) { SimpleDateFormat("HH:mm", ru).format(Date(now)) }
    val date = remember(now / 3600000) { SimpleDateFormat("EEEE, d MMMM", ru).format(Date(now)) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
        Text(
            time, color = Fg, fontFamily = Unbounded, fontWeight = FontWeight.Light,
            fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-0.9).sp,
        )
        Text(
            date, Modifier.padding(bottom = 2.dp), color = Color(0xFFC9C7C2),
            fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 22.sp,
        )
    }
}

/** Фокус сообщает ключ наверх, Enter/OK запускает. */
private fun Modifier.nav(key: String, onFocus: (String) -> Unit, run: () -> Unit): Modifier =
    onFocusChanged { if (it.isFocused) onFocus(key) }
        .clickable(interactionSource = null, indication = null, onClick = run)

/** Запрашивает фокус при первом показе. */
@Composable
private fun Modifier.startFocus(on: Boolean): Modifier {
    if (!on) return this
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    return focusRequester(fr)
}

@Composable
private fun RowScope.Tile(e: Entry, focus: String, req: FocusRequester, onFocus: (String) -> Unit) {
    val on = e.key == focus
    val w by animateFloatAsState(if (on) 2.1f else 1f, tween(220), label = "w")
    val bg by animateColorAsState(if (on) e.color else Card, tween(220), label = "bg")
    val ink = if (on) e.ink else Ink
    Column(
        Modifier.weight(w).height(164.dp).clip(RoundedCornerShape(22.dp)).background(bg)
            .focusRequester(req).nav(e.key, onFocus, e.run).padding(14.dp, 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (e.icon != null) {
            Image(e.icon, null, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
        } else {
            GridIcon(ink)
        }
        // unbounded: ширина текста не зависит от анимируемой ширины плитки, раскладка текста не пересчитывается
        Text(
            e.label, Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
            color = ink, fontFamily = Manrope, fontWeight = FontWeight.Bold,
            fontSize = 20.sp, lineHeight = 23.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun GridIcon(c: Color) {
    Canvas(Modifier.size(56.dp)) {
        val s = size.width / 24f
        val st = Stroke(1.7f * s)
        for (x in listOf(4f, 14f)) for (y in listOf(4f, 14f))
            drawRoundRect(c, Offset(x * s, y * s), Size(6 * s, 6 * s), CornerRadius(s), style = st)
    }
}

@Composable
private fun RowScope.Chip(e: Entry, focus: String, req: FocusRequester, onFocus: (String) -> Unit) {
    val on = e.key == focus
    val bg by animateColorAsState(if (on) Fg else Bg, tween(180), label = "chip")
    Box(
        Modifier.weight(1f).height(60.dp).clip(CircleShape).background(bg)
            .focusRequester(req).nav(e.key, onFocus, e.run),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            e.label, color = if (on) Dark else Ink, fontFamily = Manrope, fontWeight = FontWeight.Bold,
            fontSize = 22.sp, maxLines = 1, softWrap = false,
        )
    }
}

@Composable
private fun AllApps(apps: List<AppInfo>, onOpen: (AppInfo) -> Unit) {
    val pm = LocalContext.current.packageManager
    Box(Modifier.fillMaxSize().background(Bg)) {
        Text(
            "Все приложения", Modifier.offset(48.dp, 38.dp), color = Fg, fontFamily = Unbounded,
            fontWeight = FontWeight.Medium, fontSize = 44.sp, letterSpacing = (-1.3).sp,
        )
        LazyVerticalGrid(
            GridCells.Fixed(6),
            Modifier.offset(0.dp, 120.dp).fillMaxSize(),
            contentPadding = PaddingValues(48.dp, 8.dp, 48.dp, 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(apps, key = { _, a -> a.pkg }) { i, a ->
                var f by remember { mutableStateOf(false) }
                val icon = remember(a.pkg) { pm.getApplicationIcon(a.pkg).toBitmap(64, 64).asImageBitmap() }
                Column(
                    Modifier.height(124.dp).clip(RoundedCornerShape(22.dp))
                        .background(if (f) Fg else Card)
                        .startFocus(i == 0)
                        .onFocusChanged { f = it.isFocused }
                        .clickable(interactionSource = null, indication = null) { onOpen(a) }
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Image(icon, null, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                    Text(
                        a.label, color = if (f) Dark else Ink, fontFamily = Manrope, fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
