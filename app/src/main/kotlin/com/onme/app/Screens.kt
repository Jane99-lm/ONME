package com.onme.app

import androidx.compose.foundation.layout.offset

import androidx.compose.ui.semantics.contentDescription

import androidx.compose.ui.semantics.semantics

import androidx.activity.compose.BackHandler

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onme.core.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MD = DateTimeFormatter.ofPattern("M/d")
private val R26 = RoundedCornerShape(26.dp)

private object Why {
    const val HEALTH = "기록한 주기와 증상을 케어 추천에만 써요. 끄면 일반 추천만 보여드려요."
    const val LEARN = "좋았던 케어를 다음 추천에 더 자주 보여드려요."
    const val LOCATION = "가까운 화장실·편의점과 들를 곳을 찾을 때만 쓰고, 저장하지 않아요."
    const val VEHICLE = "버튼을 누른 뒤에만 시트·공조·조명을 바꿔요."
    const val MUSIC = "기분에 맞는 플레이리스트를 열 때만 써요."
    const val CARDS = "출발 전이나 오래 운전할 때 짧은 카드를 보여드려요."
    const val FP = "가족계획 중일 때만 켜세요. 피임 용도로 쓸 수 없어요."
}

@Composable
private fun Panel(modifier: Modifier = Modifier, scroll: Boolean = false, content: @Composable ColumnScope.() -> Unit) = Column(
    modifier.darkGlass(R40).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(40.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp), content = content,
)

@Composable
private fun Split(modifier: Modifier, vertical: Boolean, content: @Composable (side: (Float) -> Modifier) -> Unit) {
    if (vertical) Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) { content { w -> Modifier.fillMaxWidth().weight(w) } }
    else Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) { content { w -> Modifier.weight(w).fillMaxHeight() } }
}

@Composable
private fun H(text: String) = Text(text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun Toggle(label: String, value: Boolean, why: String? = null, onChange: (Boolean) -> Unit) {
    val x by animateDpAsState(if (value) 37.dp else 5.dp, tween(180, easing = EaseOut), label = "sw")
    Row(Modifier.tap { onChange(!value) }.fillMaxWidth().heightIn(min = 84.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 20.dp)) {
            Text(label, fontSize = 22.sp)
            why?.let { Text(it, fontSize = 19.sp, color = SubText, lineHeight = 25.sp, modifier = Modifier.padding(top = 4.dp)) }
        }
        Box(Modifier.size(width = 76.dp, height = 44.dp).clip(CircleShape).background(if (value) Point else MainText.copy(alpha = 0.14f))) {
            Box(Modifier.offset(x = x, y = 5.dp).size(34.dp).background(Color.White, CircleShape))
        }
    }
}

@Composable
private fun Opt(label: String, on: Boolean, onClick: () -> Unit) = Box(
    Modifier.tap(onClick).height(72.dp).clip(CircleShape).background(if (on) Point else MainText.copy(alpha = 0.06f))
        .border(1.dp, if (on) Accent.copy(alpha = 0.55f) else MainText.copy(alpha = 0.16f), CircleShape).padding(horizontal = 30.dp),
    contentAlignment = Alignment.Center,
) { Text(label, fontSize = 21.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) Bg else MainText) }

@Composable
private fun <T> Choice(title: String, options: List<Pair<T, String>>, selected: T?, onPick: (T) -> Unit) {
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (v, label) -> Opt(label, v == selected) { onPick(v) } }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Round(icon: AnimIcon, onClick: () -> Unit) = Box(
    Modifier.tap(onClick).size(72.dp).clip(CircleShape).background(MainText.copy(alpha = 0.08f)).border(1.dp, MainText.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center,
) { Glyph(icon, MainText, 30.dp) }

@Composable
fun ColumnScope.Onboarding(o: Onme) {
    val today = o.today()
    var page by remember { mutableIntStateOf(0) }
    var consents by remember { mutableStateOf(o.user.consents.takeIf { it != Consents() } ?: Consents(true, true, true, true, true, true)) }
    var ago by remember { mutableIntStateOf(0) }
    var uncertain by remember { mutableStateOf(false) }
    var bucket by remember { mutableStateOf<CycleBucket?>(null) }
    var regularity by remember { mutableStateOf<Regularity?>(null) }
    var duration by remember { mutableStateOf<DurationBucket?>(null) }
    var signals by remember { mutableStateOf(mapOf<Symptom, SignalTiming>()) }
    var thermal by remember { mutableStateOf<ThermalPref?>(null) }
    var heat by remember { mutableStateOf<Int?>(null) }
    var pattern by remember { mutableStateOf<DrivingPattern?>(null) }
    var situation by remember { mutableStateOf<Situation?>(null) }

    fun finish() = o.finishOnboarding(OnboardingAnswers(
        lastStart = today.minusDays(ago.toLong()), lastStartUncertain = uncertain, usualCycleBucket = bucket, regularity = regularity,
        durationBucket = duration, earlySignals = signals.map { EarlySignal(it.key, it.value) }.toSet(), thermal = thermal,
        preferredSeatHeatLevel = heat, drivingPattern = pattern, situation = situation,
    ), consents)

    Head(o, "초기 질문") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(5) { i -> Box(Modifier.size(width = 40.dp, height = 6.dp).clip(CircleShape).background(if (i <= page) Point else MainText.copy(alpha = 0.14f))) }
        }
    }
    Column(Modifier.fillMaxWidth().weight(1f).darkGlass(R40).padding(48.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (page) {
                0 -> {
                    Text("온미가 사용할 정보를 골라 주세요. 언제든 설정에서 바꿀 수 있어요.", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Toggle("주기·증상으로 케어 맞춤", consents.healthPersonalization, Why.HEALTH) { consents = consents.copy(healthPersonalization = it) }
                    Toggle("케어 평가로 추천 학습", consents.learnFromFeedback, Why.LEARN) { consents = consents.copy(learnFromFeedback = it) }
                    Toggle("위치 (주변 장소·급할 때)", consents.location, Why.LOCATION) { consents = consents.copy(location = it) }
                    Toggle("차량 설정 제어", consents.vehicleControl, Why.VEHICLE) { consents = consents.copy(vehicleControl = it) }
                    Toggle("음악 서비스 연결", consents.musicService, Why.MUSIC) { consents = consents.copy(musicService = it) }
                    Toggle("상황 알림 카드", consents.proactiveCards, Why.CARDS) { consents = consents.copy(proactiveCards = it) }
                    Text("다른 앱 기록 가져오기는 준비 중이에요.", fontSize = 19.sp, color = SubText)
                }
                1 -> {
                    Text("마지막 생리 시작일", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Round(Ic.MINUS) { ago = minOf(90, ago + 1) }
                        Text("${today.minusDays(ago.toLong()).format(MD)} (${if (ago == 0) "오늘" else "${ago}일 전"})", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                        Round(Ic.PLUS) { ago = maxOf(0, ago - 1) }
                    }
                    Toggle("정확하지 않아요", uncertain) { uncertain = it }
                }
                2 -> {
                    Choice("평소 주기", listOf(CycleBucket.D21_24 to "21–24일", CycleBucket.D25_28 to "25–28일", CycleBucket.D29_32 to "29–32일",
                        CycleBucket.D33_35 to "33–35일", CycleBucket.D36_PLUS to "36일 이상", CycleBucket.UNKNOWN to "모르겠어요"), bucket) { bucket = it }
                    Choice("주기가 얼마나 일정한가요?", listOf(Regularity.SAME to "거의 같아요", Regularity.FEW_DAYS to "며칠 차이", Regularity.VARIES to "많이 달라요", Regularity.UNKNOWN to "모르겠어요"), regularity) { regularity = it }
                    Choice("생리 기간", listOf(DurationBucket.D3_4 to "3–4일", DurationBucket.D5_6 to "5–6일", DurationBucket.D7_PLUS to "7일 이상", DurationBucket.UNKNOWN to "모르겠어요"), duration) { duration = it }
                }
                3 -> {
                    Text("자주 불편한 시기가 있나요? 누를 때마다 생리 전, 생리 중, 둘 다, 없음 순으로 바뀌어요", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    val timingLabel = mapOf(SignalTiming.BEFORE to "생리 전", SignalTiming.DURING to "생리 중", SignalTiming.BOTH to "둘 다")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SYMPTOM_OPTIONS.forEach { s ->
                            val t = signals[s]
                            Opt(SymptomCopy.LABEL.getValue(s) + (t?.let { ", ${timingLabel.getValue(it)}" } ?: ""), t != null) {
                                val next = when (t) { null -> SignalTiming.BEFORE; SignalTiming.BEFORE -> SignalTiming.DURING; SignalTiming.DURING -> SignalTiming.BOTH; SignalTiming.BOTH -> null }
                                signals = if (next == null) signals - s else signals + (s to next)
                            }
                        }
                    }
                }
                else -> {
                    Choice("차 안 온도", listOf(ThermalPref.COLD_SENSITIVE to "추위를 잘 타요", ThermalPref.HEAT_SENSITIVE to "더위를 잘 타요", ThermalPref.NORMAL to "보통이에요"), thermal) { thermal = it }
                    Choice("좋아하는 열선 단계", listOf(0 to "안 써요", 1 to "1단계", 2 to "2단계", 3 to "3단계"), heat) { heat = it }
                    Choice("운전 패턴", listOf(DrivingPattern.COMMUTE to "출퇴근", DrivingPattern.LONG_DISTANCE to "장거리 잦음", DrivingPattern.OCCASIONAL to "가끔"), pattern) { pattern = it }
                    Choice("해당하는 상황", listOf(Situation.HORMONAL_PILL to "호르몬 피임약", Situation.PREGNANT to "임신 중", Situation.POSTPARTUM to "출산 후",
                        Situation.PERIMENOPAUSE to "폐경 전후", Situation.NONE to "해당 없음", Situation.NO_ANSWER to "답하지 않을래요"), situation) { situation = it }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (page > 0) Btn("이전", Modifier.weight(1f), Kind.Quiet) { page-- } else Btn("나중에", Modifier.weight(1f), Kind.Quiet) { o.go(Screen.HOME) }
            if (page in 2..3) Btn("건너뛰기", Modifier.weight(1f), Kind.Quiet) { page++ }
            if (page < 4) Btn("다음", Modifier.weight(1f)) { page++ } else Btn("완료", Modifier.weight(1f)) { finish() }
        }
    }
}

@Composable
fun ColumnScope.Timer(o: Onme) {
    var left by remember(o.timerMinutes) { mutableIntStateOf(o.timerMinutes * 60) }
    LaunchedEffect(o.timerMinutes) { while (left > 0) { delay(1000); left-- }; o.timerDone() }
    val reset = o.timerMinutes <= OnmeConfig.RESET_MINUTES_LONG
    val breath = rememberInfiniteTransition(label = "breath")
    val k by breath.animateFloat(0.62f, 0.62f, infiniteRepeatable(keyframes {
        durationMillis = 10_000
        0.62f at 0 using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
        1f at 4_000 using CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
        0.62f at 10_000
    }), label = "k")
    val inBreath = ((o.timerMinutes * 60 - left) % 10) < 4
    LaunchedEffect(inBreath) { if (o.timerMinutes <= OnmeConfig.RESET_MINUTES_LONG) o.say(if (inBreath) "들이쉬어요" else "천천히 내쉬어요") }
    Head(o, if (reset) "잠깐 휴식" else "쉬어가기")
    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically)) {
        Box(Modifier.size(420.dp), contentAlignment = Alignment.Center) {
            val wobble by breath.animateFloat(0f, (Math.PI * 2).toFloat(), infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "w")
            BreathBlob(k, wobble, Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%d:%02d".format(left / 60, left % 60), fontSize = 72.sp, fontWeight = FontWeight.SemiBold)
                if (reset) Text(if (inBreath) "들이쉬어요" else "천천히 내쉬어요", fontSize = 28.sp, color = Accent)
            }
        }
        Text(if (reset) Copy.RESET_GUIDE else Copy.REST_PARKED, fontSize = 22.sp, color = SubText)
        if (reset) Seg(listOf(OnmeConfig.RESET_MINUTES to "${OnmeConfig.RESET_MINUTES}분", OnmeConfig.RESET_MINUTES_LONG to "${OnmeConfig.RESET_MINUTES_LONG}분"), o.timerMinutes) { o.timerMinutes = it }
        Btn("닫기", kind = Kind.Quiet) { o.timerDone() }
    }
}

@Composable
private fun BreathBlob(k: Float, w: Float, modifier: Modifier) = androidx.compose.foundation.Canvas(modifier) {
    val c = center
    val r0 = size.minDimension * 0.42f * k
    fun blob(r: Float, phase: Float) = androidx.compose.ui.graphics.Path().apply {
        val n = 96
        for (i in 0..n) {
            val a = (i.toFloat() / n) * (Math.PI * 2).toFloat()
            val rr = r * (1f + 0.035f * kotlin.math.sin(3 * a + w + phase) + 0.022f * kotlin.math.sin(5 * a - 1.3f * w + phase) + 0.012f * kotlin.math.sin(2 * a + 0.7f * w))
            val x = c.x + rr * kotlin.math.cos(a); val y = c.y + rr * kotlin.math.sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawCircle(Brush.radialGradient(listOf(Point.copy(alpha = 0.35f), Color.Transparent), c, r0 * 1.45f), r0 * 1.45f)
    drawPath(blob(r0 * 1.08f, 1.7f), Brush.radialGradient(listOf(Point.copy(alpha = 0.22f), Mint.copy(alpha = 0.10f)), c, r0 * 1.1f))
    drawOval(Brush.radialGradient(listOf(MainText.copy(alpha = 0.14f), Color.Transparent), Offset(c.x, c.y + r0 * 1.02f), r0 * 0.7f), Offset(c.x - r0 * 0.7f, c.y + r0 * 0.88f), Size(r0 * 1.4f, r0 * 0.28f))
    val body = blob(r0, 0f)
    drawPath(body, Brush.radialGradient(0f to Color.White, 0.35f to Color(0xFFDDF3EA), 0.75f to Point, 1f to Mint, center = Offset(c.x - r0 * 0.28f, c.y - r0 * 0.32f), radius = r0 * 1.45f))
    drawPath(body, Brush.radialGradient(0.55f to Color.Transparent, 1f to Accent.copy(alpha = 0.22f), center = Offset(c.x + r0 * 0.15f, c.y + r0 * 0.2f), radius = r0 * 1.15f))
    drawPath(body, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.1f)), c.y - r0, c.y + r0), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
    drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.Transparent), Offset(c.x - r0 * 0.38f, c.y - r0 * 0.48f), r0 * 0.34f),
        Offset(c.x - r0 * 0.72f, c.y - r0 * 0.68f), Size(r0 * 0.68f, r0 * 0.40f))
}

@Composable
fun ColumnScope.Feedback(o: Onme) {
    Head(o, "케어 평가")
    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterVertically)) {
        Text(Copy.FEEDBACK_Q, fontSize = if (compact) 40.sp else 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 64.dp)) {
            listOf(Rating.POSITIVE to MoodId.M01, Rating.NEUTRAL to MoodId.M03, Rating.NEGATIVE to MoodId.M05).forEachIndexed { i, (r, m) ->
                MoodDrop(m, Copy.RATING.getValue(r), i, orb = if (compact) 160.dp else 184.dp) { o.rate(r) }
            }
        }
        Spacer(Modifier.height(40.dp))
        Text("건너뛰기", Modifier.tap { o.rate(null) }.padding(horizontal = 24.dp, vertical = 16.dp), fontSize = 20.sp, color = SubText)
    }
}

@Composable
private fun NoticeBox(text: String, action: String, onClick: () -> Unit) = Column(
    Modifier.fillMaxWidth().clip(R26).background(Point.copy(alpha = 0.10f)).border(1.dp, Point.copy(alpha = 0.3f), R26).padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
) {
    Text(text, fontSize = 19.sp, lineHeight = 29.sp)
    Btn(action, kind = Kind.Quiet, height = 64.dp, onClick = onClick)
}

@Composable
fun ColumnScope.Calendar(o: Onme) {
    val u = o.user; val today = o.today()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val f = predictCycle(u)
    val excluded = cycleLengths(allStarts(u)).excluded
    var specialist by remember { mutableStateOf(!u.specialistNoticeShown && specialistAdvised(u)) }
    val ph = phaseOn(u, today)
    val c = compact
    val last = allStarts(u).lastOrNull()
    val mu = f?.muDays ?: 28
    val dayIdx = last?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today).toInt() } ?: 0
    val until = f?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it.rangeStart) }
    val now = when (ph?.first) {
        Phase.P1 -> "생리 ${ph.second}일째"
        Phase.OVERDUE -> "예정일 지남"
        Phase.P5 -> "곧 시작해요"
        Phase.PAUSED -> "기록만 해요"
        else -> "평소 기간"
    }
    val sub = when {
        ph?.first == Phase.OVERDUE -> Copy.OVERDUE
        f == null -> "생리 시작일을 기록해 주세요"
        until != null && until > 0 -> "다음 시작까지 약 ${until}일"
        else -> "예정 범위 안이에요"
    }

    Head(o, "캘린더") { Btn(if (c) "기록" else "오늘 기록하기") { o.go(Screen.RECORD) } }
    val side: @Composable ColumnScope.(Modifier) -> Unit = { dialMod ->
        if (specialist) NoticeBox(Copy.SPECIALIST, "확인") { specialist = false; o.specialistNoticeShown() }
        if (excluded.isNotEmpty()) NoticeBox("이 기록을 확인해 주세요: 주기 ${excluded.joinToString()}일", "기록 보기") { o.go(Screen.RECORD) }
        Box(dialMod.darkGlass(R40).border(if (ph?.first == Phase.P1) 1.5.dp else 0.dp, Point.copy(alpha = 0.6f), R40), contentAlignment = Alignment.Center) {
            CycleDial(f, last, mu, dayIdx, Modifier.fillMaxSize(0.86f).aspectRatio(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(now, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp, color = if (ph?.first == Phase.P1) Accent else if (ph?.first == Phase.OVERDUE) SosRed else MainText)
                Text(sub, fontSize = 19.sp, color = SubText)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat("다음 시작 예상", f?.takeIf { !it.paused }?.let { "${it.rangeStart.format(MD)} – ${it.rangeEnd.format(MD)}" } ?: "–", "추정", Modifier.weight(1f))
            SmallStat("예측 정확도", f?.let { listOf("시작", "학습 중", "개인화")[it.accuracyStage - 1] } ?: "–", null, Modifier.weight(1f), steps = f?.accuracyStage ?: 0)
        }
        if (u.familyPlanning) f?.ovulationEstimate?.let { SmallStat("배란 추정일", it.format(MD), "추정", Modifier.fillMaxWidth()) }
    }
    if (c) Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (specialist) NoticeBox(Copy.SPECIALIST, "확인") { specialist = false; o.specialistNoticeShown() }
        Row(
            Modifier.fillMaxWidth().height(220.dp).darkGlass(R40).border(if (ph?.first == Phase.P1) 1.5.dp else 0.dp, Point.copy(alpha = 0.6f), R40).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CycleDial(f, last, mu, dayIdx, Modifier.fillMaxHeight().aspectRatio(1f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(now, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, color = if (ph?.first == Phase.P1) Accent else if (ph?.first == Phase.OVERDUE) SosRed else MainText)
                Text(sub, fontSize = 19.sp, color = SubText)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    f?.takeIf { !it.paused }?.let { MiniChip("다음", "${it.rangeStart.format(MD)}–${it.rangeEnd.format(MD)}") }
                    f?.let { MiniChip("정확도", listOf("시작", "학습 중", "개인화")[it.accuracyStage - 1]) }
                }
            }
        }
        MonthCard(u, f, today, month, { month = it }, Modifier.fillMaxWidth().weight(1f))
    } else Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.width(440.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            side(Modifier.fillMaxWidth().weight(1f))
            Text(Copy.ESTIMATE_DISCLAIMER, fontSize = 19.sp, color = SubText, lineHeight = 23.sp, modifier = Modifier.padding(horizontal = 6.dp))
        }
        MonthCard(u, f, today, month, { month = it }, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun CycleDial(f: CycleForecast?, last: LocalDate?, mu: Int, day: Int, modifier: Modifier) = androidx.compose.foundation.Canvas(modifier) {
    val sw = size.minDimension * 0.075f
    val inset = sw / 2 + size.minDimension * 0.04f
    val tl = Offset(inset, inset); val sz = Size(size.width - inset * 2, size.height - inset * 2)
    val r = sz.width / 2; val cen = center
    fun arc(from: Float, len: Float, color: Color, width: Float = sw) {
        if (len <= 0f) return
        drawArc(color, -90f + from / mu * 360f, len / mu * 360f, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(width, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
    drawCircle(MainText.copy(alpha = 0.06f), r, cen, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
    for (i in 0 until mu) {
        val a = Math.toRadians(-90.0 + i * 360.0 / mu)
        drawCircle(MainText.copy(alpha = 0.18f), sw * 0.09f, Offset(cen.x + (r - sw * 1.2f) * kotlin.math.cos(a).toFloat(), cen.y + (r - sw * 1.2f) * kotlin.math.sin(a).toFloat()))
    }
    if (f != null && !f.paused && last != null) {
        arc(mu - 6f, 6f, Mint.copy(alpha = 0.45f))
        val rs = java.time.temporal.ChronoUnit.DAYS.between(last, f.rangeStart).toFloat()
        val re = java.time.temporal.ChronoUnit.DAYS.between(last, f.rangeEnd).toFloat()
        arc(rs, re - rs + 1f, Crimson.copy(alpha = 0.35f), sw * 0.28f)
        arc(0f, f.periodDays.toFloat(), Crimson.copy(alpha = 0.8f))
    }
    val a = Math.toRadians(-90.0 + (((day % mu) + mu) % mu) * 360.0 / mu)
    val m = Offset(cen.x + r * kotlin.math.cos(a).toFloat(), cen.y + r * kotlin.math.sin(a).toFloat())
    drawCircle(Color.White, sw * 0.5f, m)
    drawCircle(MainText, sw * 0.5f, m, style = androidx.compose.ui.graphics.drawscope.Stroke(sw * 0.18f))
}

@Composable
private fun MiniChip(label: String, value: String) = Column(
    Modifier.clip(RoundedCornerShape(18.dp)).background(Point.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 8.dp),
) {
    Text(label, fontSize = 14.sp, color = SubText)
    Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SmallStat(label: String, value: String, note: String?, modifier: Modifier, steps: Int = -1) = Column(
    modifier.darkGlass(RoundedCornerShape(28.dp)).padding(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(label, fontSize = 19.sp, color = SubText)
    Text(value, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
    note?.let { Text(it, fontSize = 19.sp, color = Accent) }
    if (steps >= 0) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..3).forEach { k -> Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(if (k <= steps) Point else MainText.copy(alpha = 0.12f))) }
    }
}

@Composable
private fun MonthCard(u: UserData, f: CycleForecast?, today: LocalDate, month: YearMonth, onMonth: (YearMonth) -> Unit, modifier: Modifier) {
    val c = compact
    Column(modifier.darkGlass(R40).padding(start = 32.dp, end = 32.dp, top = 26.dp, bottom = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${month.monthValue}월", fontSize = 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
            Text("${month.year}", fontSize = 22.sp, color = SubText, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.weight(1f))
            if (month != YearMonth.from(today)) Btn("오늘", kind = Kind.Quiet, height = 60.dp) { onMonth(YearMonth.from(today)) }
            Round(Ic.BACK) { onMonth(month.minusMonths(1)) }
            Round(Ic.CHEV) { onMonth(month.plusMonths(1)) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) { listOf("일", "월", "화", "수", "목", "금", "토").forEach { Text(it, Modifier.weight(1f), fontSize = 19.sp, color = SubText, textAlign = TextAlign.Center) } }
        val first = month.atDay(1)
        val cells = List(first.dayOfWeek.value % 7) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cells.chunked(7).forEach { week ->
                Row(Modifier.weight(1f)) {
                    (week + List(7 - week.size) { null }).forEach { day -> DayCell(day, u, f, today, Modifier.weight(1f).fillMaxHeight(), if (c) 52.dp else 64.dp) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (c) 16.dp else 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(Crimson.copy(alpha = 0.85f), "생리"); Legend(Crimson.copy(alpha = 0.16f), "예상 생리"); Legend(null, "생리 전"); Legend(Mint.copy(alpha = 0.3f), "예정 범위")
            if (u.familyPlanning) Legend(Accent.copy(alpha = 0.32f), "배란 추정")
        }
    }
}

@Composable
private fun Legend(fill: Color?, label: String, edge: Color = Color.Transparent) = Row(verticalAlignment = Alignment.CenterVertically) {
    if (fill == null) Box(Modifier.padding(7.dp).size(8.dp).background(Accent, CircleShape))
    else Box(Modifier.size(22.dp).clip(CircleShape).background(fill).border(2.5.dp, edge, CircleShape))
    Spacer(Modifier.width(8.dp)); Text(label, fontSize = if (compact) 18.sp else 19.sp, color = SubText)
}

private val Crimson = Color(0xFFC0283C)

private enum class Band { PERIOD, PREDICTED, RANGE }

private fun bandOf(day: LocalDate, u: UserData, f: CycleForecast?, today: LocalDate): Band? {
    val ph = phaseOn(u, day)
    return when {
        ph?.first == Phase.P1 -> Band.PERIOD
        f != null && !f.paused && day > today && ph?.first == Phase.OVERDUE && (ph.second ?: 99) < f.periodDays -> Band.PREDICTED
        f != null && !f.paused && day >= f.rangeStart && day <= f.rangeEnd -> Band.RANGE
        else -> null
    }
}

private fun bandColor(b: Band) = when (b) { Band.PERIOD -> Crimson.copy(alpha = 0.16f); Band.PREDICTED -> Crimson.copy(alpha = 0.08f); Band.RANGE -> Mint.copy(alpha = 0.22f) }

@Composable
private fun DayCell(day: LocalDate?, u: UserData, f: CycleForecast?, today: LocalDate, modifier: Modifier, dot: androidx.compose.ui.unit.Dp) {
    if (day == null) return Box(modifier)
    val ph = phaseOn(u, day)
    val band = bandOf(day, u, f, today)
    val joinL = band != null && day.dayOfWeek != java.time.DayOfWeek.SUNDAY && day.dayOfMonth > 1 && bandOf(day.minusDays(1), u, f, today) == band
    val joinR = band != null && day.dayOfWeek != java.time.DayOfWeek.SATURDAY && day.dayOfMonth < day.lengthOfMonth() && bandOf(day.plusDays(1), u, f, today) == band
    val isToday = day == today
    val period = band == Band.PERIOD
    Box(modifier.drawBehind {
        if (band == null) return@drawBehind
        val h = dot.toPx(); val top = center.y - h / 2; val col = bandColor(band)
        if (joinL) drawRect(col, Offset(0f, top), Size(center.x, h))
        if (joinR) drawRect(col, Offset(center.x, top), Size(size.width - center.x, h))
        if (!joinL || !joinR) drawCircle(col, h / 2, center)
    }, contentAlignment = Alignment.Center) {
        val fill = when { period -> Crimson.copy(alpha = 0.85f); ph?.first == Phase.P3 && u.familyPlanning -> Color(0xFF3F8FD8).copy(alpha = 0.25f); else -> Color.Transparent }
        Box(Modifier.size(dot).clip(CircleShape).background(fill).then(if (isToday) Modifier.border(2.5.dp, if (period) Color.White else MainText, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
            Text("${day.dayOfMonth}", fontSize = if (compact) 20.sp else 24.sp, fontWeight = if (period || isToday) FontWeight.Bold else FontWeight.Medium,
                color = when { period -> Color.White; band == Band.PREDICTED -> Crimson; else -> MainText })
            if (ph?.first == Phase.P5) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp).size(6.dp).background(Accent, CircleShape))
        }
        if (isToday) Text("오늘", Modifier.align(Alignment.BottomCenter), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private val LEVEL = listOf("", "약하게", "보통", "강하게")

@Composable
fun ColumnScope.Record(o: Onme) {
    val today = o.today()
    val order = if (o.user.onboarding?.situation == Situation.PERIMENOPAUSE) listOf(Symptom.HOT_STUFFY) + (SYMPTOM_OPTIONS - Symptom.HOT_STUFFY) else SYMPTOM_OPTIONS
    val saved = o.user.symptoms.filter { it.date == today }
    val ph = phaseOn(o.user, today)
    val dayN = if (ph?.first == Phase.P1) ph.second else null
    val level = { s: Symptom -> saved.firstOrNull { it.symptom == s }?.intensity ?: 0 }
    val c = compact
    Head(o, "${today.format(MD)} 기록")
    val flow = if (dayN != null) o.user.flows.firstOrNull { it.date == today }?.level ?: 0 else null
    var flowBig by remember { mutableStateOf((flow ?: 0) == 0) }
    var flowTaps by remember { mutableIntStateOf(0) }
    LaunchedEffect(flowTaps) { if (flowTaps > 0) { delay(2000); flowBig = false } }
    val grow by animateFloatAsState(if (flowBig) 1f else 0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow), label = "flowSize")
    @Composable fun Flow(m: Modifier = Modifier) = FlowPicker(m, flow) { flowBig = true; flowTaps++; o.setFlow(it) }
    @Composable fun PeriodTiles(m: Modifier, tile: Modifier) {
        PeriodStat(dayN, m)
        if (dayN == null) PeriodBtn(Ic.DROP, "오늘 시작", true, false, tile) { o.startPeriod() }
        else PeriodBtn(Ic.CHECK, "오늘 끝남", true, false, tile) { o.endPeriod() }
    }
    if (c) Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PeriodTiles(Modifier.weight(1.2f).fillMaxHeight(), Modifier.weight(1f).fillMaxHeight())
        }
        Flow()
        Symptoms(o, order, level, 2, Modifier.fillMaxWidth().weight(1f))
    } else Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.width(340.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PeriodStat(dayN, Modifier.fillMaxWidth().weight(0.4f + 0.6f * (1f - grow)))
            Flow(Modifier.weight(0.45f + 0.55f * grow))
            if (dayN == null) PeriodBtn(Ic.DROP, "오늘 시작", true, false, Modifier.fillMaxWidth().height(104.dp)) { o.startPeriod() }
            else PeriodBtn(Ic.CHECK, "오늘 끝남", true, false, Modifier.fillMaxWidth().height(104.dp)) { o.endPeriod() }
        }
        Symptoms(o, order, level, 3, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun PeriodStat(day: Int?, modifier: Modifier) {
    val on = day != null
    Column(
        modifier.clip(R32).background(if (on) Brush.verticalGradient(listOf(Point.copy(alpha = 0.34f), Point.copy(alpha = 0.14f))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.6f))))
            .border(1.5.dp, if (on) Point.copy(alpha = 0.7f) else MainText.copy(alpha = 0.06f), R32).padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("생리", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = if (on) Accent else SubText)
        Spacer(Modifier.height(6.dp))
        Text(if (on) "${day}일째" else "기록 없음", fontSize = if (compact) 36.sp else 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp, color = if (on) Accent else MainText)
        if (!compact) {
            Spacer(Modifier.height(10.dp))
            Text(if (on) "끝나면 아래에서 눌러 주세요" else "시작하면 아래에서 눌러 주세요", fontSize = 19.sp, color = SubText, lineHeight = 26.sp)
        }
    }
}

private val FLOW = listOf("눌러서 기록", "적음", "보통", "많음")

@Composable
private fun FlowPicker(modifier: Modifier, level: Int?, onPick: (Int) -> Unit) {
    val on = level != null
    val v = level ?: 0
    val tint = listOf(Color.Unspecified, Color(0xFFD9707E), Color(0xFFC0283C), Color(0xFF8E1526))[v]
    val ink = if (v > 0) Color.White else MainText
    val fill = if (v == 0) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.62f))) else Brush.verticalGradient(listOf(tint.copy(alpha = 0.9f), tint))
    Column(
        modifier.fillMaxWidth().then(if (compact) Modifier.height(96.dp) else Modifier).alpha(if (on) 1f else 0.45f)
            .then(if (on) Modifier.tap { onPick((v + 1) % 4) } else Modifier)
            .clip(R32).background(fill).border(1.5.dp, if (v > 0) Color.White.copy(alpha = 0.35f) else MainText.copy(alpha = 0.06f), R32)
            .padding(horizontal = 26.dp, vertical = 22.dp),
    ) {
        Text("생리량", fontSize = 21.sp, fontWeight = FontWeight.Medium, color = if (v > 0) Color.White.copy(alpha = 0.85f) else SubText)
        Text(if (on) FLOW[v] else "시작하면 기록할 수 있어요", fontSize = if (on) (if (compact) 30.sp else 40.sp) else 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp, color = ink)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (on) Text("누를 때마다 바뀌어요", fontSize = 19.sp, color = if (v > 0) Color.White.copy(alpha = 0.85f) else SubText)
                Row { (1..3).forEach { k -> Box(Modifier.padding(end = 6.dp).size(12.dp).clip(CircleShape).background(if (v > 0) Color.White.copy(alpha = if (k <= v) 1f else 0.3f) else MainText.copy(alpha = 0.12f))) } }
            }
            Box(Modifier.alpha(0.75f)) { Glyph(Ic.DROP, ink, if (compact) 48.dp else 76.dp, play = v, stroke = 0.9f) }
        }
    }
}

@Composable
private fun PeriodBtn(icon: AnimIcon, label: String, primary: Boolean, done: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = when { primary -> Deep; done -> Point.copy(alpha = 0.16f); else -> Color.White.copy(alpha = 0.7f) }
    val ink = if (primary) Color.White else if (done) Accent else MainText
    Row(
        modifier.alpha(if (enabled) 1f else 0.4f).then(if (enabled) Modifier.tap(onClick) else Modifier).clip(R32).background(bg)
            .border(1.dp, if (primary) Accent.copy(alpha = 0.55f) else MainText.copy(alpha = 0.08f), R32).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(icon, if (primary) Color.White else Accent, 32.dp, play = done)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = if (compact) 22.sp else 26.sp, fontWeight = FontWeight.SemiBold, color = ink)
    }
}

@Composable
private fun Symptoms(o: Onme, order: List<Symptom>, level: (Symptom) -> Int, cols: Int, modifier: Modifier) {
    val picked = order.map { it to level(it) }.filter { it.second > 0 }
    Column(modifier.padding(start = 8.dp, top = 4.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("오늘 느낀 것", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            if (!compact) Text("한 번 누르면 약하게, 두 번 보통, 세 번 강하게", fontSize = 20.sp, color = SubText)
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            order.chunked(cols).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { s -> SymptomTile(s, SymptomCopy.LABEL.getValue(s), level(s), Modifier.weight(1f).fillMaxHeight()) { o.setSymptom(s, (level(s) + 1) % 4) } }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (picked.isEmpty()) "누르면 바로 저장돼요" else picked.joinToString { "${SymptomCopy.LABEL.getValue(it.first)} ${LEVEL[it.second]}" },
                Modifier.weight(1f).padding(end = 20.dp), fontSize = 21.sp, color = SubText, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Glyph(Ic.CHECK, Accent, 20.dp); Spacer(Modifier.width(8.dp)); Text("자동 저장돼요", fontSize = 19.sp, color = Accent)
        }
    }
}

@Composable
private fun SymptomTile(symptom: Symptom, label: String, v: Int, modifier: Modifier, onClick: () -> Unit) {
    val on = v > 0
    val deep = listOf(Color.Unspecified, Color(0xFF4F8F76), Color(0xFF3F8069), Color(0xFF285E4B))[v]
    val fill = if (!on) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.62f)))
        else Brush.verticalGradient(listOf(deep.copy(alpha = 0.92f), deep))
    val edge = if (on) Color.White.copy(alpha = 0.35f) else MainText.copy(alpha = 0.06f)
    val ink = if (on) Color.White else MainText
    Column(modifier.tap(onClick).clip(R26).background(fill).border(1.5.dp, edge, R26).padding(horizontal = 22.dp, vertical = 18.dp)) {
        Text(label, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = ink, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (on) Text(LEVEL[v], fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
                Row { (1..3).forEach { k -> Box(Modifier.padding(end = 6.dp).size(12.dp).clip(CircleShape).background(if (on) Color.White.copy(alpha = if (k <= v) 1f else 0.3f) else MainText.copy(alpha = 0.12f))) } }
            }
            Box(Modifier.alpha(0.75f)) { Glyph(Ic.SYMPTOM.getValue(symptom), if (on) Color.White else SubText, if (compact) 56.dp else 76.dp, play = v, stroke = 0.9f) }
        }
    }
}

private data class Answer(val label: String, val hint: String, val fill: Color, val ink: Color, val icon: AnimIcon)
private val ANSWERS = listOf(
    Answer("맞았어요", "예상 범위 안", Color(0xFF8FD3B0), Color(0xFF0F3D2C), Ic.CHECK),
    Answer("더 빨랐어요", "범위보다 일찍", Color(0xFFF4D37A), Color(0xFF4A3A08), Ic.BACK),
    Answer("더 늦었어요", "범위보다 늦게", Color(0xFFEFA3A6), Color(0xFF5A1C20), Ic.CHEV),
)

@Composable
fun Modal(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val c = compact
    BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(MainText.copy(alpha = 0.25f)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.padding(horizontal = if (c) 20.dp else 32.dp).widthIn(max = 960.dp).fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, null) {}
                .darkGlass(RoundedCornerShape(44.dp), Color.White.copy(alpha = 0.96f)).padding(if (c) 32.dp else 48.dp),
        ) {
            content()
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = if (c) 12.dp else 20.dp, y = if (c) (-12).dp else (-20).dp)
                    .tap(onDismiss).size(56.dp).background(MainText.copy(alpha = 0.07f), CircleShape).border(1.dp, MainText.copy(alpha = 0.16f), CircleShape).semantics { contentDescription = "닫기" },
                contentAlignment = Alignment.Center,
            ) { Glyph(Ic.CLOSE, MainText, 26.dp) }
        }
    }
}

@Composable
fun PredictionModal(o: Onme, a: LocalDate, b: LocalDate) {
    val c = compact
    Modal(onDismiss = { o.dismissPrediction() }) {
        Column(verticalArrangement = Arrangement.spacedBy(if (c) 24.dp else 32.dp)) {
            Text("예상 범위", fontSize = 20.sp, color = SubText)
            Text("${a.format(MD)} ~ ${b.format(MD)}", fontSize = if (c) 44.sp else 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Text("이 범위 안에 시작했나요?", fontSize = if (c) 26.sp else 30.sp, fontWeight = FontWeight.SemiBold)
            @Composable fun AnswerBtn(x: Answer, m: Modifier) = Row(
                m.tap { o.answerPrediction(x.label) }.height(if (c) 104.dp else 120.dp).clip(RoundedCornerShape(30.dp)).background(x.fill).border(1.dp, x.ink.copy(alpha = 0.25f), RoundedCornerShape(30.dp)).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).background(Color.White.copy(alpha = 0.55f), CircleShape), contentAlignment = Alignment.Center) { Glyph(x.icon, x.ink, 22.dp) }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(x.label, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = x.ink)
                    Text(x.hint, fontSize = 19.sp, color = x.ink.copy(alpha = 0.75f))
                }
            }
            if (c) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ANSWERS.forEach { AnswerBtn(it, Modifier.fillMaxWidth()) } }
            else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { ANSWERS.forEach { AnswerBtn(it, Modifier.weight(1f)) } }
        }
    }
}

@Composable
fun ColumnScope.Settings(o: Onme) {
    val c = o.user.consents
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    Head(o, "설정")
    Split(Modifier.fillMaxWidth().weight(1f), compact) { side ->
        Panel(side(1f), scroll = true) {
            H("개인정보·동의")
            if (o.user.cycleTracking) Toggle("주기·증상으로 케어 맞춤", c.healthPersonalization, Why.HEALTH) { o.setConsents(c.copy(healthPersonalization = it)) }
            Toggle("케어 평가로 추천 학습", c.learnFromFeedback, Why.LEARN) { o.setConsents(c.copy(learnFromFeedback = it)) }
            Toggle("위치 (주변 장소·급할 때)", c.location, Why.LOCATION) { o.setConsents(c.copy(location = it)) }
            Toggle("차량 설정 제어", c.vehicleControl, Why.VEHICLE) { o.setConsents(c.copy(vehicleControl = it)) }
            Toggle("음악 서비스 연결", c.musicService, Why.MUSIC) { o.setConsents(c.copy(musicService = it)) }
            Toggle("상황 알림 카드", c.proactiveCards, Why.CARDS) { o.setConsents(c.copy(proactiveCards = it)) }
            Toggle("음성 안내", o.voiceOn, "알림과 휴식 안내를 차량 음성으로 읽어 드려요. 조용한 환경을 켜면 이번 주행 동안 꺼져요.") { o.voiceOn = it }
            if (o.user.cycleTracking) Toggle("배란 추정 표시 (가족계획)", o.user.familyPlanning, Why.FP) { o.setFamilyPlanning(it) }
            Text("모든 기록은 이 차량의 내 프로필에만 암호화해 저장돼요.", fontSize = 19.sp, color = SubText)
        }
        Panel(side(1f), scroll = true) {
            H("데이터")
            if (o.user.cycleTracking) Btn("초기 질문 다시", Modifier.fillMaxWidth(), Kind.Quiet) { o.go(Screen.ONBOARDING) }
            Btn("데이터 내보내기", Modifier.fillMaxWidth(), Kind.Quiet) { context.startActivity(o.exportIntent()) }
            if (confirmDelete) Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(SosRed.copy(alpha = 0.12f)).border(1.dp, SosRed.copy(alpha = 0.4f), RoundedCornerShape(28.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("모든 기록과 설정을 지울까요? 되돌릴 수 없어요.", fontSize = 21.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Btn("삭제", Modifier.weight(1f), Kind.Danger) { confirmDelete = false; o.deleteAll() }
                    Btn("취소", Modifier.weight(1f), Kind.Quiet) { confirmDelete = false }
                }
            } else Btn("전체 삭제", Modifier.fillMaxWidth(), Kind.Danger) { confirmDelete = true }
            Spacer(Modifier.height(20.dp))
            H("시연 모드")
            Toggle("시연 모드", o.demo) { o.demo = it }
            if (o.demo) {
                val k = o.demoCaps
                Toggle("시트 열선", k.seatHeat) { o.demoCaps = k.copy(seatHeat = it) }
                Toggle("시트 통풍", k.seatVent) { o.demoCaps = k.copy(seatVent = it) }
                Toggle("온도", k.hvacTemp) { o.demoCaps = k.copy(hvacTemp = it) }
                Toggle("바람 세기", k.hvacFan) { o.demoCaps = k.copy(hvacFan = it) }
                Toggle("실내 조명", k.ambientLight) { o.demoCaps = k.copy(ambientLight = it) }
                Toggle("시트 위치", k.seatPosition) { o.demoCaps = k.copy(seatPosition = it) }
                Toggle("내비 연동", k.navigation) { o.demoCaps = k.copy(navigation = it) }
                Toggle("음악 서비스", o.demoMusic) { o.demoMusic = it }
                Text("급할 때 찾기 결과", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("ok" to "정상", "weak" to "위치 부정확", "none" to "결과 없음", "gps" to "위치 오류", "net" to "네트워크 오류").forEach { (v, l) -> Opt(l, o.demoSos == v) { o.demoSos = v } }
                }
                Text("가상 위치: 서울시청", fontSize = 19.sp, color = SubText)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Btn("사용자 A 불러오기", Modifier.weight(1f), Kind.Quiet) { o.loadUserA() }
                    Btn("시연 초기화", Modifier.weight(1f), Kind.Quiet) { o.resetDemo() }
                }
            }
        }
    }
}
