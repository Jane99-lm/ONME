package com.onme.app

import androidx.compose.ui.graphics.drawscope.clipPath

import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onme.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

val Bg = Color(0xFF0D1715)
val Pale = Color(0xFFEEF7F3)
val CardBg = Color.White
val Accent = Color(0xFF3F8069)
val Point = Color(0xFF9DD5BD)
val MainText = Color(0xFF16302A)
val SubText = Color(0xFF5B7A6E)
val Mint = Color(0xFF7FC0A5)
val SosRed = Color(0xFFD2646B)
private val StartInk = Color(0xFF16302A)
private val StartSub = Color(0xFF5B7A6E)
private val StartBrand = Color(0xFF3F8069)
val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val LocalCompact = staticCompositionLocalOf { false }
val compact: Boolean @Composable get() = LocalCompact.current

@Composable
fun OnmeUi(o: Onme) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Point, onPrimary = Bg, surface = CardBg, onSurface = MainText, background = Pale, onBackground = MainText)) {
        val start = o.screen == Screen.LOADING || o.screen == Screen.PROFILE
        Surface(Modifier.fillMaxSize(), color = Color.White, contentColor = MainText) {
            LiquidGround(o.parked, beige = !start) {
              BoxWithConstraints(Modifier.fillMaxSize()) {
               CompositionLocalProvider(LocalCompact provides (maxWidth < 1200.dp)) {
                AnimatedContent(
                    targetState = start,
                    transitionSpec = { fadeIn(tween(400, 200, EaseOut)) togetherWith fadeOut(tween(200)) },
                    label = "root",
                ) { s ->
                    if (s) { if (o.screen == Screen.LOADING) Loading() else ProfilePick(o) } else AppShell(o)
                }
               }
              }
            }
        }
    }
}

@Composable
private fun AppShell(o: Onme) {
    val c = compact
    val bottom = if (o.demo) (if (c) 212.dp else 136.dp) else 44.dp
    val start = if (c) 128.dp else 216.dp
    val end = if (c) 32.dp else 56.dp
    LaunchedEffect(o.blocked) { if (o.blocked) { o.message = Copy.PARK_FIRST; o.blocked = false } }
    Box(Modifier.fillMaxSize()) {
        if (o.screen == Screen.CONFIRM) CycleModel(cycleModelUrl(o.user, o.today()), restricted = !o.parked, Modifier.fillMaxSize())
        Rail(o, Modifier.padding(start = if (c) 16.dp else 32.dp, top = 32.dp, bottom = if (o.demo) bottom - 12.dp else 32.dp))
        AnimatedContent(
            targetState = o.screen,
            transitionSpec = { fadeIn(tween(220, easing = EaseOut)) togetherWith fadeOut(tween(100)) },
            modifier = Modifier.fillMaxSize().padding(start = start, end = end, top = 44.dp, bottom = bottom + if (o.card != null) 144.dp else 0.dp),
            label = "screen",
        ) { s ->
            Column(Modifier.fillMaxSize()) {
                when (s) {
                    Screen.HOME -> Home(o)
                    Screen.GOAL, Screen.RESULT -> Result(o)
                    Screen.CONFIRM -> Confirm(o)
                    Screen.SOS -> Sos(o)
                    Screen.ONBOARDING -> Onboarding(o)
                    Screen.RESET -> Timer(o)
                    Screen.FEEDBACK -> Feedback(o)
                    Screen.CALENDAR -> Calendar(o)
                    Screen.RECORD -> Record(o)
                    Screen.SETTINGS -> Settings(o)
                    Screen.LOADING, Screen.PROFILE -> Unit
                }
            }
        }
        o.card?.let { TriggerCard(o, it, Modifier.align(Alignment.BottomStart).padding(start = start, end = end, bottom = bottom)) }
        if (o.demo) DemoBar(o, Modifier.align(Alignment.BottomStart).padding(start = start, end = end, bottom = 28.dp))
        Toast(o, Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + 20.dp))
        if (o.screen == Screen.SOS && o.sosLoading) SosSearching(o)
        o.predictionAsk?.let { (a, b) -> PredictionModal(o, a, b) }
    }
}

@Composable
fun pressScale(src: MutableInteractionSource): Float {
    val pressed by src.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 1f, stiffness = 1400f), label = "press").value
}

@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val s = pressScale(src)
    return graphicsLayer { scaleX = s; scaleY = s }.clickable(src, indication = null, role = Role.Button, onClick = onClick)
}

fun Modifier.darkGlass(shape: Shape, fill: Color = Color.White.copy(alpha = 0.55f)) = clip(shape)
    .background(Brush.verticalGradient(listOf(fill.copy(alpha = (fill.alpha + 0.15f).coerceAtMost(1f)), fill)))
    .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.3f), Accent.copy(alpha = 0.12f))), shape)

val R40 = RoundedCornerShape(40.dp)
val R32 = RoundedCornerShape(32.dp)

enum class Kind { Primary, Quiet, Danger }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.Primary, height: Dp = 84.dp, onClick: () -> Unit) {
    val bg = when (kind) { Kind.Primary -> Point; Kind.Quiet -> MainText.copy(alpha = 0.08f); Kind.Danger -> SosRed.copy(alpha = 0.22f) }
    val edge = when (kind) { Kind.Danger -> SosRed.copy(alpha = 0.55f); Kind.Quiet -> MainText.copy(alpha = 0.16f); else -> Accent.copy(alpha = 0.55f) }
    Row(
        modifier.tap(onClick).height(height).clip(CircleShape).background(bg).border(1.dp, edge, CircleShape)
            .padding(start = 40.dp, end = if (kind == Kind.Primary) 16.dp else 40.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, fontSize = if (height < 70.dp) 19.sp else if (height >= 100.dp) 26.sp else 23.sp, fontWeight = if (kind == Kind.Primary) FontWeight.SemiBold else FontWeight.Medium,
            color = if (kind == Kind.Primary) Bg else MainText, maxLines = 1)
        if (kind == Kind.Primary) {
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(if (height < 70.dp) 40.dp else 52.dp).background(Bg.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) { Glyph(Ic.CHEV, Bg, 22.dp) }
        }
    }
}

@Composable
fun <T> Seg(options: List<Pair<T, String>>, selected: T?, height: Dp = 64.dp, onPick: (T) -> Unit) {
    Row(Modifier.darkGlass(CircleShape).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (v, label) ->
            val on = v == selected
            Box(Modifier.tap { onPick(v) }.height(height).clip(CircleShape).background(if (on) Point else Color.Transparent).border(1.dp, if (on) Accent.copy(alpha = 0.55f) else MainText.copy(alpha = 0.16f), CircleShape).padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                Text(label, fontSize = 21.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, color = if (on) Bg else SubText)
            }
        }
    }
}

@Composable
fun Head(o: Onme, title: String, right: @Composable RowScope.() -> Unit = {}) = Head(o, { Text(title, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp) }, right)

@Composable
fun Head(o: Onme, title: @Composable RowScope.() -> Unit, right: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(Modifier.tap { o.go(Screen.HOME) }.size(72.dp).darkGlass(CircleShape).border(1.dp, MainText.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Glyph(Ic.BACK, MainText, 30.dp) }
        title()
        Spacer(Modifier.weight(1f))
        right()
    }
    Spacer(Modifier.height(28.dp))
}

@Composable
fun CareBadge(care: CareId, size: Dp, icon: Dp) {
    Box(Modifier.size(size).background(Brush.radialGradient(listOf(Color.White, Point, Mint), Offset.Unspecified), CircleShape), contentAlignment = Alignment.Center) {
        Glyph(Ic.CARE.getValue(care), Bg, icon, play = care)
    }
}

@Composable
fun MoodFace(m: MoodId, color: Color, size: Dp) = Canvas(Modifier.size(size)) {
    val w = size.toPx(); val sw = w * 2.6f / 56f; val st = Stroke(sw, cap = StrokeCap.Round)
    val ey = w * 0.40f; val lx = w * 0.36f; val rx = w * 0.64f
    fun dots() { drawCircle(color, sw * 0.9f, Offset(lx, ey)); drawCircle(color, sw * 0.9f, Offset(rx, ey)) }
    fun arc(top: Float, h: Float, start: Float, sweep: Float, x0: Float = 0.33f, x1: Float = 0.67f) =
        drawArc(color, start, sweep, false, Offset(w * x0, w * top), Size(w * (x1 - x0), w * h), style = st)
    fun line(x0: Float, y0: Float, x1: Float, y1: Float) = drawLine(color, Offset(w * x0, w * y0), Offset(w * x1, w * y1), sw, StrokeCap.Round)
    when (m) {
        MoodId.M01 -> { dots(); arc(0.46f, 0.22f, 15f, 150f) }
        MoodId.M02 -> { arc(0.36f, 0.08f, 0f, 180f, 0.30f, 0.42f); arc(0.36f, 0.08f, 0f, 180f, 0.58f, 0.70f); arc(0.52f, 0.12f, 20f, 140f, 0.38f, 0.62f) }
        MoodId.M03 -> { dots(); line(0.38f, 0.64f, 0.62f, 0.64f) }
        MoodId.M04 -> { line(0.30f, 0.42f, 0.42f, 0.42f); line(0.58f, 0.42f, 0.70f, 0.42f); arc(0.60f, 0.10f, 200f, 140f, 0.42f, 0.58f) }
        MoodId.M05 -> { dots(); arc(0.60f, 0.18f, 200f, 140f) }
        MoodId.M06 -> {
            line(0.30f, 0.30f, 0.43f, 0.36f); line(0.70f, 0.30f, 0.57f, 0.36f); dots()
            drawPath(Path().apply { moveTo(w * 0.34f, w * 0.66f); listOf(0.42f to 0.60f, 0.50f to 0.66f, 0.58f to 0.60f, 0.66f to 0.66f).forEach { (x, y) -> lineTo(w * x, w * y) } }, color, style = st)
        }
    }
}

fun moodLabel(m: MoodId) = Copy.MOOD.getValue(m).substringAfter(' ')

fun Modifier.orb(lit: Boolean) = drawBehind {
    val c = Offset(size.width * 0.36f, size.height * 0.28f)
    val body = if (lit) Brush.radialGradient(0f to Color.White, 0.55f to Point, 1f to Mint, center = c, radius = size.minDimension * 0.8f)
    else Brush.radialGradient(0f to Color.White.copy(alpha = 0.95f), 0.45f to Color.White.copy(alpha = 0.55f), 0.8f to Point.copy(alpha = 0.35f), 1f to Mint.copy(alpha = 0.45f), center = c, radius = size.minDimension * 0.8f)
    drawCircle(body)
    drawCircle(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Point.copy(alpha = 0.24f)))
    drawCircle(Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.2f))), style = Stroke(1.5.dp.toPx()))
    rotate(-24f, Offset(size.width * 0.40f, size.height * 0.22f)) {
        drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), Color.Transparent), Offset(size.width * 0.40f, size.height * 0.22f), size.minDimension * 0.24f),
            topLeft = Offset(size.width * 0.17f, size.height * 0.10f), size = Size(size.width * 0.46f, size.height * 0.26f))
    }
}


private class Eyes(val iw: Float, val ih: Float, val lid: Color, val eyes: List<FloatArray>)
private fun two(x: Float, y: Float, w: Float, h: Float) = listOf(floatArrayOf(x, y, w / 2 - 0.004f, h), floatArrayOf(x + w / 2 + 0.004f, y, w / 2 - 0.004f, h))
private val EYES = mapOf(
    MoodId.M01 to Eyes(336f, 390f, Color(0xFF6CBE61), listOf(floatArrayOf(0.3393f, 0.2077f, 0.1488f, 0.1897f), floatArrayOf(0.494f, 0.2077f, 0.1429f, 0.1897f))),
    MoodId.M03 to Eyes(343f, 377f, Color(0xFF51AC53), two(0.3615f, 0.2122f, 0.2828f, 0.1989f)),
    MoodId.M04 to Eyes(345f, 376f, Color(0xFF70C261), two(0.3391f, 0.3005f, 0.2812f, 0.1915f)),
    MoodId.M05 to Eyes(343f, 376f, Color(0xFF71C261), two(0.3557f, 0.3378f, 0.2886f, 0.1915f)),
    MoodId.M06 to Eyes(350f, 375f, Color(0xFF67BE5B), two(0.3686f, 0.2987f, 0.28f, 0.1867f)),
)

@Composable
private fun Blink(m: MoodId, index: Int, calm: Boolean) {
    val e = EYES[m] ?: return
    val f = remember { Animatable(0f) }
    LaunchedEffect(calm) {
        if (calm) { f.snapTo(0f); return@LaunchedEffect }
        val rnd = kotlin.random.Random(index)
        delay(800L + index * 450L)
        while (true) {
            repeat(if (rnd.nextFloat() < 0.2f) 2 else 1) {
                f.animateTo(1f, tween(70, easing = EaseIn)); f.animateTo(0f, tween(120, easing = EaseOut)); delay(90)
            }
            delay(2600L + rnd.nextLong(3200L))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val v = f.value
        if (v <= 0.01f) return@Canvas
        val k = minOf(size.width / e.iw, size.height / e.ih)
        val ox = (size.width - e.iw * k) / 2; val oy = (size.height - e.ih * k) / 2
        e.eyes.forEach { (x, y, w, h) ->
            val r = androidx.compose.ui.geometry.Rect(ox + x * e.iw * k, oy + y * e.ih * k, ox + (x + w) * e.iw * k, oy + (y + h) * e.ih * k)
            clipPath(Path().apply { addOval(r.inflate(1.5f)) }) {
                val bottom = r.top - 2f + (r.height + 4f) * v
                drawRect(Brush.verticalGradient(listOf(e.lid.copy(alpha = 1f), lerpLid(e.lid)), r.top, bottom), Offset(r.left - 2f, r.top - 2f), Size(r.width + 4f, bottom - r.top + 2f))
                drawLine(Color(0xFF0F5A2C), Offset(r.left, bottom), Offset(r.right, bottom), 2.2f * k / 1f, StrokeCap.Round)
            }
        }
    }
}
private fun lerpLid(c: Color) = Color(c.red * 0.82f, c.green * 0.86f, c.blue * 0.82f)

fun moodArt(m: MoodId) = when (m) {
    MoodId.M01 -> R.drawable.mood_m01; MoodId.M02 -> R.drawable.mood_m02; MoodId.M03 -> R.drawable.mood_m03
    MoodId.M04 -> R.drawable.mood_m04; MoodId.M05 -> R.drawable.mood_m05; MoodId.M06 -> R.drawable.mood_m06
}

@Composable
fun MoodDrop(m: MoodId, label: String, index: Int, still: Boolean = false, orb: Dp = 184.dp, onPick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ring = remember { Animatable(0f) }
    val rise = remember { Animatable(if (still) 1f else 0f) }
    var lit by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!still) { delay(250L + index * 60L); rise.animateTo(1f, tween(900, easing = EaseOut)) } }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s = pressScale(src)
    Column(
        Modifier.width(orb + 12.dp).graphicsLayer { alpha = rise.value; translationY = (1 - rise.value) * 40.dp.toPx() }
            .clickable(src, indication = null, role = Role.Button) {
                if (still) onPick() else {
                    lit = true
                    scope.launch { ring.snapTo(0f); ring.animateTo(1f, tween(700, easing = EaseOut)) }
                    scope.launch { delay(220); onPick() }
                }
            }
            .drawBehind {
                if (ring.value in 0.001f..0.999f) {
                    val k = 0.5f + 1.4f * ring.value; val w = orb.toPx() * k; val h = 28.dp.toPx() * k
                    drawOval(Point.copy(alpha = 0.9f * (1 - ring.value)), Offset((size.width - w) / 2, (orb + 16.dp).toPx() - h / 2), Size(w, h), style = Stroke(1.5.dp.toPx()))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val on = lit || pressed
        val calm = still || android.provider.Settings.Global.getFloat(androidx.compose.ui.platform.LocalContext.current.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        val squash by androidx.compose.animation.core.animateFloatAsState(if (pressed) 1f else 0f,
            if (pressed) tween(110, easing = EaseOut) else spring(dampingRatio = 0.35f, stiffness = 420f), label = "squash")
        val hop = remember { Animatable(0f) }
        LaunchedEffect(lit) { if (lit && !calm) { hop.animateTo(1f, tween(140, easing = EaseOut)); hop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 380f)) } }
        Box(
            Modifier.size(orb).graphicsLayer {
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.92f)
                scaleX = 1f + 0.07f * squash - 0.03f * hop.value
                scaleY = 1f - 0.10f * squash + 0.05f * hop.value
                translationY = -hop.value * 18.dp.toPx()
            },
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(moodArt(m)), null, Modifier.fillMaxSize())
            Blink(m, index, calm)
        }
        Spacer(Modifier.height(18.dp))
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Rail(o: Onme, modifier: Modifier) {
    val p = o.profile ?: return
    val c = compact
    val cell = if (c) 80.dp else 112.dp
    Column(
        modifier.width(if (c) 96.dp else 136.dp).fillMaxHeight().darkGlass(RoundedCornerShape(if (c) 36.dp else 44.dp)).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(Modifier.tap { o.switchProfile() }.size(cell), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(if (c) 44.dp else 56.dp).background(Brush.radialGradient(listOf(Color.White, avatarColor(o, p))), CircleShape), contentAlignment = Alignment.Center) {
                Text(p.role.take(1), fontSize = if (c) 18.sp else 22.sp, fontWeight = FontWeight.Bold, color = Bg)
            }
            Spacer(Modifier.height(if (c) 4.dp else 8.dp))
            Text(p.role, fontSize = if (c) 16.sp else 19.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.padding(top = 4.dp, bottom = 8.dp).width(56.dp).height(1.dp).background(MainText.copy(alpha = 0.08f)))
        val home = o.screen in setOf(Screen.HOME, Screen.RESULT, Screen.GOAL, Screen.CONFIRM)
        RailItem(Ic.HOME, "홈", home) { o.go(Screen.HOME) }
        if (o.parked) {
            if (o.user.cycleTracking) {
                RailItem(Ic.REC, "기록", o.screen == Screen.RECORD) { o.go(Screen.RECORD) }
                RailItem(Ic.CAL, "캘린더", o.screen == Screen.CALENDAR) { o.go(Screen.CALENDAR) }
            }
            RailItem(Ic.SET, "설정", o.screen == Screen.SETTINGS) { o.go(Screen.SETTINGS) }
        }
        Spacer(Modifier.weight(1f))
        val on = o.screen == Screen.SOS
        Column(
            Modifier.tap { o.go(Screen.SOS) }.size(cell).clip(RoundedCornerShape(if (c) 24.dp else 30.dp))
                .background(Brush.linearGradient(listOf(SosRed.copy(alpha = if (on) 0.6f else 0.42f), SosRed.copy(alpha = if (on) 0.4f else 0.18f))))
                .border(1.5.dp, SosRed.copy(alpha = 0.6f), RoundedCornerShape(if (c) 24.dp else 30.dp)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(30.dp).background(SosRed.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Box(Modifier.size(16.dp).background(SosRed, CircleShape)) }
            Spacer(Modifier.height(6.dp))
            Text("급할 때", fontSize = if (c) 16.sp else 19.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RailItem(icon: AnimIcon, label: String, on: Boolean, onClick: () -> Unit) {
    val c = compact
    Column(
        Modifier.tap(onClick).size(width = if (c) 80.dp else 112.dp, height = if (c) 88.dp else 104.dp).clip(RoundedCornerShape(if (c) 24.dp else 30.dp)).background(if (on) Point.copy(alpha = 0.14f) else Color.Transparent).border(1.dp, if (on) Accent.copy(alpha = 0.45f) else MainText.copy(alpha = 0.10f), RoundedCornerShape(if (c) 24.dp else 30.dp)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Glyph(icon, Accent, if (c) 26.dp else 30.dp, play = on)
        Spacer(Modifier.height(if (c) 4.dp else 8.dp))
        Text(label, fontSize = if (c) 16.sp else 19.sp, fontWeight = FontWeight.Medium, color = if (on) MainText else SubText)
    }
}

@Composable
private fun Toast(o: Onme, modifier: Modifier) {
    val msg = o.message
    LaunchedEffect(msg) { if (msg != null) { delay(2400); if (o.message == msg) o.message = null } }
    androidx.compose.animation.AnimatedVisibility(msg != null, modifier, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Row(Modifier.height(76.dp).darkGlass(CircleShape, Color.White.copy(alpha = 0.95f)).padding(horizontal = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph(Ic.CHECK, Accent, 26.dp)
            Spacer(Modifier.width(14.dp))
            Text(msg ?: "", fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TriggerCard(o: Onme, id: String, modifier: Modifier) {
    val text = when (id) { Cards.TR1 -> Copy.TR1; Cards.TR2 -> Copy.TR2; else -> Copy.TR3 }
    Row(
        modifier.fillMaxWidth().height(128.dp).darkGlass(RoundedCornerShape(64.dp), Color.White.copy(alpha = 0.88f)).border(1.5.dp, Point, RoundedCornerShape(64.dp))
            .padding(start = 48.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, fontSize = if (compact) 22.sp else 26.sp, fontWeight = FontWeight.SemiBold)
            if (id == Cards.TR3) Text(Copy.READY_KIT_ITEMS, fontSize = 19.sp, color = SubText)
        }
        Btn(if (id == Cards.TR3) "확인" else Copy.OK) { o.cardOk() }
        if (id != Cards.TR3) Btn(Copy.LATER, kind = Kind.Quiet) { o.cardLater() }
    }
}

@Composable
private fun DemoBar(o: Onme, modifier: Modifier) {
    val c by o.cabin.collectAsState()
    val md = DateTimeFormatter.ofPattern("M/d")
    val readout = "열선 ${c.seatHeatLevel} · 통풍 ${c.seatVentLevel} · ${c.cabinTempC}°C · 바람 ${c.fanLevel} · 조명 ${if (c.ambientDim) "낮춤" else "기본"}"
    val narrow = compact
    Column(
        modifier.fillMaxWidth().darkGlass(RoundedCornerShape(44.dp)).padding(start = 32.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("시연", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Accent)
            Seg(listOf(DrivingState.PARKED to "정차", DrivingState.DRIVING to "주행", DrivingState.UNKNOWN to "신호 없음"), o.demoDriving, 56.dp) { o.demoDriving = it }
            if (!narrow) DemoExtras(o, md)
            Spacer(Modifier.weight(1f))
            if (!narrow) Text(readout, fontSize = 18.sp, color = SubText, modifier = Modifier.padding(end = 16.dp))
        }
        if (narrow) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DemoExtras(o, md)
            Text(readout, Modifier.weight(1f), fontSize = 16.sp, color = SubText, maxLines = 2)
        }
    }
}

@Composable
private fun DemoExtras(o: Onme, md: DateTimeFormatter) {
    Btn("주행 +90분", kind = Kind.Quiet, height = 60.dp) { o.demoAddDriveMinutes(90) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.tap { o.demoDate = o.today().minusDays(1) }.size(52.dp).clip(CircleShape).background(MainText.copy(alpha = 0.06f)).border(1.dp, MainText.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Glyph(Ic.BACK, MainText, 22.dp) }
        Text(o.today().format(md), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.tap { o.demoDate = o.today().plusDays(1) }.size(52.dp).clip(CircleShape).background(MainText.copy(alpha = 0.06f)).border(1.dp, MainText.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Glyph(Ic.CHEV, MainText, 22.dp) }
    }
}

private val DAY = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

@Composable
private fun ColumnScope.Home(o: Onme) {
    val hour = remember { java.time.LocalTime.now().hour }
    Box(Modifier.fillMaxWidth()) {
        Text(o.today().format(DAY), Modifier.align(Alignment.TopEnd).padding(top = 6.dp), fontSize = 22.sp, color = SubText)
    }
    val c = compact
    val orb = if (c) 200.dp else 212.dp
    Spacer(Modifier.height(if (c) 48.dp else 72.dp))
    Text("${greeting(hour)}, ${o.profile?.role ?: ""}", fontSize = 22.sp, color = SubText)
    Spacer(Modifier.height(12.dp))
    Text("지금 기분은 어떤가요?", fontSize = if (c) 48.sp else 64.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.4).sp)
    Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
        (if (c) MoodId.entries.chunked(3) else listOf(MoodId.entries)).forEachIndexed { r, row ->
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = if (c) Arrangement.SpaceEvenly else Arrangement.SpaceBetween) {
                    row.forEachIndexed { i, m -> MoodDrop(m, moodLabel(m), r * 3 + i, still = !o.parked, orb = orb) { o.pickMood(m) } }
                }
            }
        }
    }
    when {
        o.driving == DrivingState.UNKNOWN -> HomeNote("차량 상태를 확인할 수 없어 주행 중과 같이 보여드려요")
        !o.parked -> HomeNote("주행 중에는 기분 고르기와 급할 때 찾기만 쓸 수 있어요")
        o.feedbackPending -> HomeSlot(Copy.FEEDBACK_Q, "평가하기") { o.go(Screen.FEEDBACK) }
        o.user.cycleTracking && o.user.onboarding == null -> HomeSlot("몇 가지만 알려주시면 케어가 더 잘 맞아요", "초기 질문 시작하기") { o.go(Screen.ONBOARDING) }
        o.card == null -> HomeNote("기분을 고르면 지금 할 수 있는 케어를 찾아 드려요")
    }
}

@Composable
private fun HomeNote(text: String) = Box(Modifier.height(120.dp), contentAlignment = Alignment.CenterStart) { Text(text, fontSize = 20.sp, color = SubText) }

@Composable
private fun HomeSlot(text: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(120.dp).darkGlass(RoundedCornerShape(60.dp)).padding(start = 44.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Btn(action, onClick = onClick)
    }
}

@Composable
private fun ColumnScope.Result(o: Onme) {
    val goal = o.goal ?: return
    val mood = o.mood ?: return
    val c = compact
    val goals = goalsFor(mood).map { it to Copy.GOAL.getValue(it) }
    Head(o, {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(moodArt(mood)), null, Modifier.fillMaxSize()); Blink(mood, 9, !o.parked) }
        Text(moodLabel(mood), fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }) { if (!c) Seg(goals, goal) { o.pickGoal(it) } }
    if (c) { Seg(goals, goal) { o.pickGoal(it) }; Spacer(Modifier.height(24.dp)) }
    val r = o.rec ?: return
    val top = when (r) { is Recommendation.Main -> r.main.care; is Recommendation.SafetyFirst -> r.main.care; is Recommendation.GeneralChoices -> r.choices.firstOrNull() }
    val alts = when (r) { is Recommendation.Main -> r.alternatives.map { it.care }; is Recommendation.SafetyFirst -> r.alternatives.map { it.care }; else -> emptyList() }
    val others = when (r) {
        is Recommendation.GeneralChoices -> r.choices.drop(1).take(2)
        is Recommendation.SafetyFirst -> alts
        is Recommendation.Main -> (alts + r.ranked.map { it.care }.filter { it != top && it !in alts }).take(2)
    }
    Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (top != null) TopPick(o, top, mood, goal, (r as? Recommendation.SafetyFirst)?.notice, Modifier.fillMaxWidth().weight(1f))
        else Text(Copy.GENERAL, fontSize = 22.sp, color = SubText, modifier = Modifier.padding(start = 8.dp))
        others.forEachIndexed { i, care ->
            OtherRow(o, care, i < alts.size, Modifier.fillMaxWidth().height(if (c) 120.dp else 140.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopPick(o: Onme, care: CareId, mood: MoodId, goal: GoalId, notice: String?, modifier: Modifier) {
    val c = compact
    val liked = o.user.sessions.any { it.chosen == care && it.rating == Rating.POSITIVE }
    val tags = if (notice != null) listOf("안전 우선") else listOfNotNull("${moodLabel(mood)}일 때", "${Copy.GOAL.getValue(goal)}에 좋아요", "지난번 좋았어요".takeIf { liked }, "정차 중에만".takeIf { care == CareId.RESET })
    val dets = if (notice != null) listOf(notice) else details(o, care)
    val action = Copy.BUTTONS.getValue(care).first
    @Composable fun Head(m: Modifier) = Column(m, verticalArrangement = Arrangement.spacedBy(18.dp, if (c) Alignment.Top else Alignment.CenterVertically)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CareBadge(care, if (c) 80.dp else 96.dp, if (c) 38.dp else 44.dp)
            Box(Modifier.height(40.dp).clip(CircleShape).background(if (notice != null) Color(0xFFE9A9AD) else Point).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                Text(if (notice != null) "먼저 쉬어가요" else "가장 추천", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Bg)
            }
        }
        Text(o.preview(care).headline, fontSize = if (c) 36.sp else 50.sp, fontWeight = FontWeight.SemiBold, lineHeight = if (c) 46.sp else 62.sp, letterSpacing = (-1).sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tags.forEach { t ->
                Box(Modifier.height(46.dp).clip(CircleShape).background(Point.copy(alpha = 0.16f)).border(1.dp, Point.copy(alpha = 0.35f), CircleShape).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) { Text(t, fontSize = 20.sp) }
            }
        }
    }
    @Composable fun Plan(m: Modifier) = Column(
        m.clip(R32).background(Color.White.copy(alpha = 0.7f)).border(1.dp, Color.White, R32).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(if (notice != null) "안내" else "적용하면", fontSize = 19.sp, color = SubText)
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
            dets.forEach { d ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Point.copy(alpha = 0.10f)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Point.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { Glyph(Ic.CHECK, Accent, 16.dp) }
                    Spacer(Modifier.width(14.dp))
                    Text(d, fontSize = 23.sp, fontWeight = FontWeight.Medium, lineHeight = 31.sp)
                }
            }
        }
        Btn(action, Modifier.fillMaxWidth(), height = 96.dp) { o.start(care) }
    }
    val m = modifier.darkGlass(R40).border(1.5.dp, Point.copy(alpha = 0.45f), R40)
    if (c) Column(m.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Head(Modifier.fillMaxWidth())
        Plan(Modifier.fillMaxWidth().weight(1f))
    } else Row(m.padding(28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Head(Modifier.weight(1f).fillMaxHeight().padding(start = 20.dp))
        Plan(Modifier.width(400.dp).fillMaxHeight())
    }
}

@Composable
private fun OtherRow(o: Onme, care: CareId, alsoGood: Boolean, modifier: Modifier) {
    val c = compact
    Row(modifier.tap { o.start(care) }.darkGlass(R32).border(1.dp, MainText.copy(alpha = 0.12f), R32).padding(horizontal = if (c) 22.dp else 30.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(72.dp).background(Point.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) { Glyph(Ic.CARE.getValue(care), Accent, 36.dp) }
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(Copy.CARE_NAME.getValue(care), fontSize = if (c) 23.sp else 27.sp, fontWeight = FontWeight.SemiBold)
            Text(details(o, care).first(), fontSize = 20.sp, color = SubText, maxLines = 1)
        }
        if (alsoGood && !c) Text("이것도 괜찮아요", fontSize = 18.sp, color = Accent, modifier = Modifier.padding(end = 18.dp))
        Glyph(Ic.CHEV, SubText, 26.dp)
    }
}

@Composable
private fun DataCard(label: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.darkGlass(R32).padding(horizontal = 28.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 19.sp, color = SubText)
        content()
    }

@Composable
private fun CabinCard(o: Onme, modifier: Modifier) {
    val cab by o.cabin.collectAsState()
    DataCard("지금 차 안", modifier) {
        Text("${cab.cabinTempC}°C", fontSize = 46.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
        Text("열선 ${cab.seatHeatLevel}단계 · 통풍 ${cab.seatVentLevel}단계 · 바람 ${cab.fanLevel}단계" + if (cab.ambientDim) " · 조명 낮춤" else "", fontSize = 19.sp, color = SubText, lineHeight = 28.sp)
    }
}

private fun details(o: Onme, care: CareId): List<String> {
    val a = o.preview(care)
    return when (care) {
        CareId.COMFORT, CareId.REPLAY -> summary(a.settings).ifEmpty { listOf("저장한 설정") }
        CareId.QUIET -> summary(a.settings) + listOfNotNull("이번 주행 동안 알림 끄기".takeIf { a.quietDrive })
        CareId.RESET -> listOf("호흡 안내와 함께 ${a.minutes ?: OnmeConfig.RESET_MINUTES}분 쉬어요")
        CareId.REST -> listOf(if (o.parked) Copy.REST_PARKED else Copy.REST_DRIVING)
        CareId.PLACE -> a.placeKinds.map { "근처 $it" }.ifEmpty { listOf("근처 화장실") }
        CareId.MUSIC -> listOf(a.playlistTag ?: "플레이리스트")
    }
}

private fun summary(s: VehicleSettings?): List<String> = s?.let {
    listOfNotNull(
        it.seatHeatLevel?.let { l -> "시트 열선 ${l}단계" }, it.seatVentLevel?.let { l -> "시트 통풍 ${l}단계" },
        it.cabinTempC?.let { t -> "실내 온도 ${t}도" }, it.fanLevel?.let { l -> "바람 세기 ${l}단계" },
        it.ambientDim?.let { "실내 조명 낮춤" }, it.seatProfileId?.let { "저장한 시트 위치" },
    )
}.orEmpty()

@Composable
private fun ColumnScope.Confirm(o: Onme) {
    val a = o.action ?: return
    val c = compact
    val care = a.care
    val running = o.applied || care !in VEHICLE_CARES
    Head(o, Copy.CARE_NAME.getValue(care))
    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom)) {
        if (o.applied) Row(Modifier.height(40.dp).clip(CircleShape).background(Point).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph(Ic.CHECK, Bg, 18.dp); Spacer(Modifier.width(8.dp)); Text(Copy.APPLIED, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Bg)
        }
        Text(a.headline, fontSize = if (c) 34.sp else 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp, lineHeight = if (c) 44.sp else 54.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (!running) Text("적용하는 중이에요", fontSize = 21.sp, color = SubText)
    }
    Spacer(Modifier.height(20.dp))
    val action: @Composable (Modifier) -> Unit = { m -> ActionCard(o, a, m) }
    val third: @Composable (Modifier) -> Unit = { m ->
        DataCard(if (care in VEHICLE_CARES) "바뀐 설정" else "안내", m) {
            val lines = if (care in VEHICLE_CARES) summary(a.settings) + listOfNotNull("이번 주행 동안 온미 알림 끄기".takeIf { a.quietDrive }) else details(o, care)
            lines.forEach { d -> Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(Point, CircleShape)); Spacer(Modifier.width(10.dp)); Text(d, fontSize = 21.sp, color = SubText) } }
        }
    }
    if (c) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        action(Modifier.fillMaxWidth())
        Row(Modifier.height(200.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { CabinCard(o, Modifier.weight(1f).fillMaxHeight()); third(Modifier.weight(1f).fillMaxHeight()) }
    } else Row(Modifier.fillMaxWidth().height(250.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        action(Modifier.weight(1.3f).fillMaxHeight())
        CabinCard(o, Modifier.weight(1f).fillMaxHeight())
        third(Modifier.weight(1f).fillMaxHeight())
    }
}

private val VEHICLE_CARES = setOf(CareId.COMFORT, CareId.QUIET, CareId.REPLAY)

@Composable
private fun ActionCard(o: Onme, a: CareAction, modifier: Modifier) {
    val (yes, no) = Copy.BUTTONS.getValue(a.care)
    DataCard("다음", modifier) {
        when (a.care) {
            CareId.COMFORT, CareId.QUIET, CareId.REPLAY -> {
                Spacer(Modifier.weight(1f))
                if (o.applied) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Btn("확인", Modifier.weight(1f), height = 84.dp) { o.go(Screen.HOME) }
                    Btn(Copy.REVERT, Modifier.weight(1f), Kind.Quiet, height = 84.dp) { o.revert(); o.go(Screen.RESULT) }
                } else Btn(no, Modifier.fillMaxWidth(), Kind.Quiet, height = 84.dp) { o.go(Screen.RESULT) }
            }
            CareId.PLACE -> {
                if (a.sosToilet) Btn(yes, Modifier.fillMaxWidth(), height = 72.dp) { o.confirm() }
                else a.placeKinds.take(2).forEach { k -> MiniAction(Ic.CARE.getValue(CareId.PLACE), "근처 $k") { o.sendPlace(k) } }
                Spacer(Modifier.weight(1f))
                Text(no, Modifier.tap { o.go(Screen.HOME) }.padding(vertical = 6.dp), fontSize = 20.sp, color = SubText)
            }
            CareId.MUSIC -> {
                o.playlists.take(2).forEach { p -> MiniAction(Ic.CARE.getValue(CareId.MUSIC), p.title) { o.openPlaylist(p) } }
                Spacer(Modifier.weight(1f))
                Text(no, Modifier.tap { o.go(Screen.HOME) }.padding(vertical = 6.dp), fontSize = 20.sp, color = SubText)
            }
            CareId.REST -> {
                Spacer(Modifier.weight(1f))
                Btn(if (o.parked) "10분 쉬기" else yes, Modifier.fillMaxWidth(), height = 84.dp) { o.confirm() }
            }
            CareId.RESET -> {
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Btn(yes, Modifier.weight(1f), height = 84.dp) { o.confirm() }
                    Btn(no, Modifier.weight(1f), Kind.Quiet, height = 84.dp) { o.go(Screen.HOME) }
                }
            }
        }
    }
}

@Composable
private fun MiniAction(icon: AnimIcon, text: String, onClick: () -> Unit) =
    Row(Modifier.tap(onClick).fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp)).background(Point.copy(alpha = 0.18f)).border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Glyph(icon, Accent, 24.dp); Spacer(Modifier.width(14.dp))
        Text(text, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Glyph(Ic.CHEV, SubText, 18.dp)
    }

@Composable
private fun ColumnScope.Sos(o: Onme) {
    Head(o, SosCopy.TITLE)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SosCategory.entries.forEach { c ->
            val on = o.sosCat == c
            Box(
                Modifier.tap { o.sos(c) }.weight(1f).height(104.dp).darkGlass(CircleShape, if (on) SosRed else Color.White.copy(alpha = 0.55f)).border(1.dp, if (on) SosRed.copy(alpha = 0.8f) else MainText.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(SosCopy.CATEGORY.getValue(c), fontSize = 28.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
    Spacer(Modifier.height(24.dp))
    if (o.sosCat == null) Text("무엇을 찾을까요? 가까운 곳 3곳을 보여드려요", fontSize = 24.sp, color = SubText)
    o.sosError?.let { err ->
        Column(Modifier.fillMaxWidth().darkGlass(R40).padding(40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(err, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            o.sosCat?.let { c -> Btn(SosCopy.TRY_AGAIN, kind = Kind.Quiet) { o.sos(c) } }
        }
    }
    val res = o.sosResult ?: return
    if (res.locationWarning) Text(SosCopy.LOCATION_WEAK, fontSize = 19.sp, color = SubText, modifier = Modifier.padding(start = 12.dp, bottom = 14.dp))
    if (res.results.isEmpty()) Column(Modifier.fillMaxWidth().darkGlass(R40).padding(40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(SosCopy.NONE, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Btn(SosCopy.RETRY, Modifier.weight(1f), Kind.Quiet) { o.sos(o.sosCat!!) }
            Btn(SosCopy.OTHER, Modifier.weight(1f), Kind.Quiet) { o.sos(SosCategory.entries.first { it != o.sosCat }) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { res.results.forEach { (c, _) -> SosRow(o, c) } }
    if (o.sosCat == SosCategory.STORE && res.results.isNotEmpty()) Text(SosCopy.STORE_NOTE, fontSize = 19.sp, color = SubText, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
}

@Composable
private fun SosSearching(o: Onme) {
    val c = compact
    val move = o.parked
    val pulse = rememberInfiniteTransition(label = "radar")
    val t by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "t")
    val what = SosCopy.CATEGORY.getValue(o.sosCat ?: SosCategory.TOILET)
    val r = o.sosRadius
    val steps = listOf(
        "현재 위치 확인" to (r != null),
        (if (r == null) "가까운 ${what} 찾기" else "${r / 1000}km 안에서 ${what} 찾는 중") to false,
        "가까운 순으로 정리" to false,
    )
    BackHandler { o.cancelSos() }
    Column(
        Modifier.fillMaxSize().background(Color.White).clickable(remember { MutableInteractionSource() }, null) {}.padding(vertical = if (c) 40.dp else 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("가까운 ${what}을 찾고 있어요", fontSize = if (c) 36.sp else 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp)
        Spacer(Modifier.height(8.dp))
        Text("위치는 찾을 때만 쓰고 저장하지 않아요", fontSize = 20.sp, color = SubText)
        Box(Modifier.weight(1f).fillMaxWidth().drawBehind {
            val max = size.minDimension / 2 * 0.95f
            for (i in 0 until 4) {
                val p = if (move) (t + i / 4f) % 1f else (i + 1) / 4.5f
                drawCircle(Accent.copy(alpha = 0.30f * (1f - p)), radius = max * (0.18f + 0.82f * p), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
            drawCircle(Point.copy(alpha = 0.16f), radius = max * 0.32f)
            drawCircle(Point.copy(alpha = 0.22f), radius = max * 0.2f)
        }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(if (c) 88.dp else 104.dp).background(Brush.radialGradient(listOf(Color.White, Point, Mint)), CircleShape).border(1.dp, Accent.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Glyph(Ic.CARE.getValue(CareId.PLACE), Bg, if (c) 40.dp else 48.dp, play = r)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            steps.forEachIndexed { i, (label, done) ->
                val active = !done && (i == 0 && r == null || i == 1 && r != null)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).background(if (done) Point else if (active) Point.copy(alpha = 0.25f) else MainText.copy(alpha = 0.06f), CircleShape), contentAlignment = Alignment.Center) {
                        if (done) Glyph(Ic.CHECK, Bg, 20.dp, play = true)
                        else if (active) Box(Modifier.size(10.dp).graphicsLayer { val k = if (move) 0.7f + 0.6f * kotlin.math.abs(kotlin.math.sin(t * 2f * Math.PI.toFloat())) else 1f; scaleX = k; scaleY = k }.background(Accent, CircleShape))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(label, fontSize = 24.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, color = if (done || active) MainText else SubText)
                }
            }
        }
        Spacer(Modifier.height(if (c) 28.dp else 36.dp))
        Btn("취소", Modifier.widthIn(min = 240.dp), Kind.Quiet, height = 72.dp) { o.cancelSos() }
    }
}

@Composable
private fun SosRow(o: Onme, c: SosCandidate) {
    var open by remember(c.name) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().darkGlass(R32).padding(start = 36.dp, end = 20.dp, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(c.name, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                Text(listOfNotNull(c.eta?.let { SosCopy.eta(it) }, SosCopy.open(c.openStatus)).joinToString(" · "), fontSize = 20.sp, color = SubText)
            }
            Btn(if (o.parked) "안내" else "경유지 추가") { o.sendSos(c) }
        }
        if (o.parked) {
            if (open) {
                Text(SosCopy.parking(c.parkingStatus) + ", 빈자리는 보장되지 않아요", fontSize = 19.sp, color = SubText)
                if (c.category == SosCategory.TOILET) Text(SosCopy.access(c.accessStatus), fontSize = 19.sp, color = SubText)
                Text(SosCopy.reliability(c.reliability), fontSize = 19.sp, color = SubText)
            }
            Text(if (open) "접기" else "자세히", Modifier.tap { open = !open }.padding(vertical = 10.dp), fontSize = 19.sp, color = Accent)
        }
    }
}

@Composable
private fun Loading() {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(400, easing = EaseOut)) }
    Column(
        Modifier.fillMaxSize().graphicsLayer { alpha = t.value; val s = 0.95f + 0.05f * t.value; scaleX = s; scaleY = s },
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("온미", fontSize = 88.sp, fontWeight = FontWeight.Bold, color = StartBrand, letterSpacing = (-1).sp)
        Text("ONME", fontSize = 22.sp, letterSpacing = 10.sp, color = StartSub)
        Spacer(Modifier.height(56.dp))
        LinearProgressIndicator(Modifier.width(120.dp).height(3.dp).clip(CircleShape), color = StartBrand, trackColor = Point.copy(alpha = 0.3f))
    }
}

private val AVATARS = listOf(Color(0xFFBFE8D6), Color(0xFFC9D9F0), Color(0xFFF5D6C8), Color(0xFFDCD3F0))
private fun avatarColor(o: Onme, p: Profile) = AVATARS[o.profiles.indexOf(p).coerceAtLeast(0) % AVATARS.size]

private fun greeting(hour: Int) = when (hour) {
    in 5..10 -> "좋은 아침이에요"
    in 11..16 -> "좋은 오후예요"
    in 17..21 -> "좋은 저녁이에요"
    else -> "편안한 밤이에요"
}

@Composable
private fun ProfilePick(o: Onme) {
    var addNote by remember { mutableStateOf(false) }
    val hour = remember { java.time.LocalTime.now().hour }
    Box(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 44.dp)) {
        Row(Modifier.align(Alignment.TopStart), verticalAlignment = Alignment.Bottom) {
            Text("온미", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = StartBrand, letterSpacing = (-0.5).sp)
            Spacer(Modifier.width(10.dp))
            Text("ONME", fontSize = 14.sp, letterSpacing = 3.sp, color = StartSub, modifier = Modifier.padding(bottom = 5.dp))
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(greeting(hour), fontSize = 22.sp, color = StartSub)
            Spacer(Modifier.height(10.dp))
            val c = compact
            val bead = if (c) 84.dp else 112.dp
            Text("누가 운전하나요?", fontSize = if (c) 40.sp else 46.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp, color = StartInk)
            Spacer(Modifier.height(56.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(if (c) 16.dp else 28.dp)) {
                o.profiles.forEach { p ->
                    ProfileCard(p.role, { o.selectProfile(p) }) {
                        Box(Modifier.size(bead).bead(avatarColor(o, p)), contentAlignment = Alignment.Center) {
                            Text(p.role.take(1), fontSize = if (c) 36.sp else 46.sp, fontWeight = FontWeight.SemiBold, color = StartInk)
                        }
                    }
                }
                ProfileCard("프로필 추가", { addNote = true }, quiet = true) {
                    Box(Modifier.size(bead).glass(CircleShape, 0.6f), contentAlignment = Alignment.Center) {
                        Glyph(Ic.PLUS, StartSub, 34.dp)
                    }
                }
            }
        }
        Text(
            if (addNote) "프로필은 휴대폰 앱에서 추가할 수 있어요 (준비 중)" else "각 프로필의 기록은 이 차량에 따로 보관돼요",
            Modifier.align(Alignment.BottomCenter), fontSize = 19.sp, color = StartSub,
        )
    }
}

private val CardShape = RoundedCornerShape(32.dp)

@Composable
private fun ProfileCard(label: String, onClick: () -> Unit, quiet: Boolean = false, avatar: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s = pressScale(src)
    GlassPane(
        CardShape,
        Modifier.graphicsLayer { scaleX = s; scaleY = s }.size(width = if (compact) 160.dp else 224.dp, height = if (compact) 210.dp else 268.dp)
            .clickable(src, indication = null, role = Role.Button, onClick = onClick),
        strength = if (quiet) 0.55f else 1f,
    ) {
        Column(
            Modifier.fillMaxSize().then(if (pressed) Modifier.border(2.dp, StartBrand, CardShape) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            avatar()
            Spacer(Modifier.height(if (compact) 16.dp else 24.dp))
            Text(label, fontSize = if (compact) 20.sp else 24.sp, fontWeight = FontWeight.Medium, color = if (quiet) StartSub else StartInk)
        }
    }
}
