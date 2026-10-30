package com.hexdevop.launcher

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.graphics.RadialGradient
import android.graphics.Shader
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
@Immutable
class Entry(
    val key: String,
    val label: String,
    val sub: String,
    val color: Color,
    val ink: Color,
    val icon: ImageBitmap?,
    val run: () -> Unit,
)

@Immutable
class AppInfo(val pkg: String, val label: String, val intent: Intent, val icon: ImageBitmap)

class Data(val apps: List<Entry>, val dock: List<Entry>, val all: List<AppInfo>) {
    val keys = apps.map { it.key } + dock.map { it.key } + all.map { it.pkg }
}

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    // всё тяжёлое (PackageManager, TvApi, иконки, свечения) грузится в фоне, главный поток только рисует
    private var data by mutableStateOf<Data?>(null)
    private var showAll by mutableStateOf(false)
    private var focus by mutableStateOf<String?>(null)
    private val glows = mutableStateMapOf<Color, ImageBitmap>()
    private val setFocus: (String) -> Unit = { focus = it }

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
        return list + allEntry(onAll)
    }

    private fun allEntry(onAll: () -> Unit) =
        Entry("all", "Все", "Все приложения на телевизоре", Color(0xFFE7E4DE), Color(0xFF14120C), null, onAll)

    private val tvui = Intent().setComponent(ComponentName("com.hisilicon.tvui", "com.hisilicon.tvui.MainActivity"))

    private fun input(id: Int) {
        Thread {
            TvApi.selectSource(id)
            runOnUiThread { open(Intent(tvui)) }
        }.start()
    }

    // без tvui: при пустом списке каналов он сам открывает мастер сканирования
    private fun panel(kind: String) {
        open(Intent(this, PanelActivity::class.java).putExtra("kind", kind))
    }

    private fun dockEntries(): List<Entry> {
        fun e(key: String, label: String, sub: String, color: Color = Blue, run: () -> Unit) =
            Entry(key, label, sub, color, Dark, null, run)
        val inputs = dockInputs(TvApi.sourceList()).map { (id, name) ->
            e("in$id", name, sourceCaption(id), Color(0xFFC9D2E0)) { input(id) }
        }
        return inputs + listOf(
            e("picture", "Картинка", "Яркость, контраст, режим") { panel("picture") },
            e("sound", "Звук", "Громкость и режим") { panel("sound") },
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
                AppInfo(
                    a.packageName, it.loadLabel(pm).toString(),
                    Intent().setComponent(ComponentName(a.packageName, a.name)), icon(it.loadIcon(pm)),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun load() {
        Thread {
            // HOME не имеет права падать: при сбое остаются «Все», настройки и входы
            val d = runCatching { Data(appEntries { showAll = true }, dockEntries(), allApps()) }.getOrElse {
                Log.e("Launcher", "load failed", it)
                Data(listOf(allEntry { showAll = true }), dockEntries(), emptyList())
            }
            val start = pickStartFocus(prefs.getString("last", null), d.apps.map { it.key }) ?: "all"
            runOnUiThread {
                if (data?.keys != d.keys) {
                    data = d
                    // фокус на удалённом приложении не оставляем
                    if (focus == null || focus !in d.keys) focus = start
                }
            }
            val first = (d.apps + d.dock).firstOrNull { it.key == start }?.color
            val dens = resources.displayMetrics.density
            (listOfNotNull(first) + (d.apps + d.dock).map { it.color }).distinct().forEach { c ->
                if (c !in glows) runCatching { glow(c, dens) }.onSuccess { g -> runOnUiThread { glows[c] = g } }
            }
        }.start()
    }

    override fun onStart() {
        super.onStart()
        load()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showAll = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme {
                val d = data
                if (d == null) {
                    Box(Modifier.fillMaxSize().background(Bg))
                } else if (showAll) {
                    BackHandler { showAll = false }
                    AllApps(d.all) { open(it.intent, it.pkg) }
                } else {
                    BackHandler { }
                    key(d) { Home(d.apps, d.dock, focus ?: "all", glows, setFocus) }
                }
            }
        }
    }
}

@Composable
private fun Home(
    apps: List<Entry>, dock: List<Entry>, focus: String, glows: Map<Color, ImageBitmap>, onFocus: (String) -> Unit,
) {
    val cur = (apps + dock).firstOrNull { it.key == focus } ?: apps.first()
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
    Box(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxSize().drawBehind { eclipse(glows[cur.color]) })
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
            apps.forEach { Tile(it, it.key == focus, reqs.getValue(it.key), focused) }
        }
        Row(
            Modifier.offset(48.dp, 594.dp).width(1184.dp).height(88.dp).jump(Key.DirectionUp, 0),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dock.forEach { Chip(it, it.key == focus, reqs.getValue(it.key), focused) }
        }
    }
}

// свечение и кромка затмения рисуются один раз в маленький битмап на цвет (область x 510..1280, y 0..650 dp)
private const val GLOW_SCALE = .5f

private fun glow(c: Color, d: Float): ImageBitmap {
    val k = d * GLOW_SCALE
    val bmp = Bitmap.createBitmap((770 * k).toInt(), (650 * k).toInt(), Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(bmp)
    fun ring(cx: Float, r: Float, vararg s: Pair<Float, Float>) {
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.DITHER_FLAG)
        val x = (cx - 510) * k
        p.shader = RadialGradient(
            x, 160 * k, r * k, IntArray(s.size) { c.copy(alpha = s[it].second).toArgb() },
            FloatArray(s.size) { s[it].first }, Shader.TileMode.CLAMP,
        )
        cv.drawCircle(x, 160 * k, r * k, p)
    }
    ring(1000f, 490f, 0f to .62f, .38f to .2f, .68f to 0f)
    ring(980f, 236f, 0f to .85f, .8f to .85f, 1f to 0f)
    return bmp.asImageBitmap()
}

private fun DrawScope.eclipse(g: ImageBitmap?) {
    if (g != null) {
        drawImage(
            g, dstOffset = IntOffset(510.dp.roundToPx(), 0), dstSize = IntSize(770.dp.roundToPx(), 650.dp.roundToPx()),
            filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
        )
    }
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
private fun RowScope.Tile(e: Entry, on: Boolean, req: FocusRequester, onFocus: (String) -> Unit) {
    val bg = if (on) e.color else Card
    val ink = if (on) e.ink else Ink
    Column(
        Modifier.weight(if (on) 2.1f else 1f).height(164.dp).clip(RoundedCornerShape(22.dp)).background(bg)
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
private fun RowScope.Chip(e: Entry, on: Boolean, req: FocusRequester, onFocus: (String) -> Unit) {
    val bg = if (on) Fg else Bg
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
                Column(
                    Modifier.height(124.dp).clip(RoundedCornerShape(22.dp))
                        .background(if (f) Fg else Card)
                        .startFocus(i == 0)
                        .onFocusChanged { f = it.isFocused }
                        .clickable(interactionSource = null, indication = null) { onOpen(a) }
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Image(a.icon, null, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                    Text(
                        a.label, color = if (f) Dark else Ink, fontFamily = Manrope, fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
