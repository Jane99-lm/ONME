package com.onme.app

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import com.onme.core.Phase

data class JadeLook(val solidity: Float, val fluid: Float, val membrane: Float, val glow: Float, val transparency: Float, val scale: Float, val rose: Float)

private val LOOKS = mapOf(
    Phase.P1 to JadeLook(.90f, .10f, .15f, .30f, .20f, .98f, .065f),
    Phase.P2 to JadeLook(.72f, .38f, .40f, .50f, .42f, 1f, 0f),
    Phase.P3 to JadeLook(.32f, .72f, .85f, .77f, .70f, 1.03f, 0f),
    Phase.P4 to JadeLook(.55f, .45f, .60f, .58f, .52f, 1.01f, 0f),
    Phase.P5 to JadeLook(.75f, .22f, .35f, .38f, .30f, .99f, .055f),
)
private val NEUTRAL = JadeLook(.65f, .20f, .28f, .40f, .35f, 1f, 0f)

fun jadeLook(phase: Phase?): JadeLook = when (phase) {
    Phase.OVERDUE -> LOOKS.getValue(Phase.P5)
    null, Phase.PAUSED -> NEUTRAL
    else -> LOOKS.getValue(phase)
}

private const val JADE = """
uniform float2 res;
uniform float t;
uniform float solidity;
uniform float fluid;
uniform float membrane;
uniform float glow;
uniform float transp;
uniform float scl;
uniform float rose;
uniform float3 tint;

float hash(float3 p) { return fract(sin(dot(p, float3(127.1, 311.7, 74.7))) * 43758.5453); }
float noise(float3 p) {
    float3 i = floor(p); float3 f = fract(p); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + float3(1, 0, 0)), f.x), mix(hash(i + float3(0, 1, 0)), hash(i + float3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(i + float3(0, 0, 1)), hash(i + float3(1, 0, 1)), f.x), mix(hash(i + float3(0, 1, 1)), hash(i + float3(1, 1, 1)), f.x), f.y), f.z);
}
float fbm(float3 p) { return 0.5 * noise(p) + 0.25 * noise(p * 2.07 + 3.1) + 0.125 * noise(p * 4.13 + 7.7); }
float3 rotY(float3 p, float a) { float c = cos(a), s = sin(a); return float3(c * p.x + s * p.z, p.y, -s * p.x + c * p.z); }
float3 rotX(float3 p, float a) { float c = cos(a), s = sin(a); return float3(p.x, c * p.y - s * p.z, s * p.y + c * p.z); }

float2 sphere(float3 ro, float3 rd, float3 c, float r) {
    float3 oc = ro - c; float b = dot(oc, rd); float h = b * b - dot(oc, oc) + r * r;
    if (h < 0.0) return float2(-1.0);
    h = sqrt(h); return float2(-b - h, -b + h);
}

float3 env(float3 d) {
    float3 c = mix(float3(0.80, 0.91, 0.87), float3(0.98, 1.0, 0.99), smoothstep(-0.5, 0.9, d.y));
    c += 1.8 * smoothstep(0.955, 0.995, dot(d, normalize(float3(-0.45, 0.75, 0.55))));
    c += 0.7 * smoothstep(0.965, 0.995, dot(d, normalize(float3(0.75, 0.25, 0.6))));
    c += 0.25 * smoothstep(0.90, 0.99, dot(d, normalize(float3(0.0, -0.8, 0.6))));
    return c;
}

float wob(float3 d) {
    float m = max(fluid, 0.4);
    return m * (0.035 * (noise(d * 1.6 + float3(0.0, 0.25 * t, 0.15 * t)) - 0.5) * 2.0 + 0.014 * sin(3.0 * d.y + 2.0 * d.x + t));
}

half4 main(float2 fc) {
    float2 p = (fc - res * 0.5) / (min(res.x, res.y) * 0.5);
    p.y = -p.y;
    float3 ro = float3(0.0, 0.0, 3.4);
    float3 rd = normalize(float3(p * 0.335, -1.0));
    float3 C = float3(0.0);
    float Rw = 1.0 * scl;

    float closest = length(cross(C - ro, rd));
    float3 near = normalize(ro + rd * dot(C - ro, rd) - C + float3(0.0001));
    float R = Rw * (1.0 + wob(near));
    float alpha = smoothstep(R + 0.006, R - 0.004, closest);
    if (alpha <= 0.0) return half4(0.0);

    float2 hw = sphere(ro, rd, C, R);
    float3 hp = ro + rd * max(hw.x, 0.0);
    float3 n = normalize(hp - C);
    n = normalize(n + 0.10 * max(fluid, 0.4) * float3(sin(3.1 * hp.y + t), sin(2.7 * hp.z + 1.3 * t), sin(2.9 * hp.x - 0.8 * t)));

    float cosi = clamp(dot(-rd, n), 0.0, 1.0);
    float F = 0.02 + 0.98 * pow(1.0 - cosi, 5.0);
    float3 refl = env(reflect(rd, n));

    float3 rr = refract(rd, n, 1.0 / 1.333);
    float3 J = float3(0.045 * sin(0.7 * t), 0.04 * cos(0.9 * t), 0.0);
    float Rj = 0.44 * Rw;
    float open = clamp((transp - 0.20) / 0.50, 0.0, 1.0);
    float3 pigment = tint;
    float3 glowC = mix(float3(0.55, 0.86, 0.75), float3(0.75, 1.0, 0.91), open);
    float3 L = normalize(float3(-0.45, 0.75, 0.55));

    float3 inside;
    float2 hj = sphere(hp, rr, J, Rj);
    float2 exitW = sphere(hp, rr, C, R);
    if (hj.x > 0.0) {
        float3 pj = hp + rr * hj.x;
        float3 nj = normalize(pj - J);
        float3 q = rotX(rotY(nj, 0.35 * t), 0.3 + 0.2 * sin(0.4 * t));
        float cloud = fbm(q * 2.4 + 1.7);
        float vein = pow(1.0 - abs(2.0 * fbm(q * 3.6 + float3(4.0, 1.0, 2.0)) - 1.0), 6.0);
        float3 base = mix(pigment * 0.82, mix(pigment, float3(1.0), 0.32), smoothstep(0.25, 0.75, cloud));
        base = mix(base, float3(1.0, 0.98, 0.99), vein * 0.28);
        float ndl = dot(nj, L);
        float3 col = base * (0.62 + 0.38 * max(ndl, 0.0) + 0.12 * clamp((ndl + 0.5) / 1.5, 0.0, 1.0));
        float view = clamp(dot(nj, -rr), 0.0, 1.0);
        col += glowC * (0.15 + glow * 0.35) * pow(1.0 - view, 1.6);
        col += glowC * (0.10 + glow * 0.20) * (0.5 + 0.5 * fbm(q * 1.3 + 0.2 * t));
        float rough = mix(18.0, 70.0, 1.0 - solidity * 0.6);
        col += pow(max(dot(reflect(rr, nj), L), 0.0), rough) * 0.45;
        col += 0.12 * env(reflect(rr, nj)) * pow(1.0 - view, 3.0);
        float3 water = float3(0.90, 0.97, 0.94);
        inside = mix(col, water, 0.04 + 0.08 * (1.0 - view));
    } else {
        float3 ep = hp + rr * exitW.y;
        float3 en = -normalize(ep - C);
        float3 rd3 = refract(rr, en, 1.333);
        if (dot(rd3, rd3) < 0.001) rd3 = reflect(rr, en);
        float len = exitW.y;
        inside = env(rd3) * exp(-len * float3(0.08, 0.03, 0.05));
        float3 jc = J - hp;
        float dperp = length(cross(jc, rr));
        inside += pigment * 0.18 * exp(-max(dperp - Rj, 0.0) * 6.0);
        float caust = 0.5 + 0.5 * sin(7.0 * ep.x + 3.0 * sin(5.0 * ep.y + t) + t) * sin(6.0 * ep.y - 2.0 * sin(4.0 * ep.z - 0.7 * t));
        inside += float3(1.0) * 0.07 * caust;
    }
    float3 col = mix(inside, refl, F);
    col += pow(max(dot(reflect(rd, n), L), 0.0), 220.0) * 1.2;
    col += pow(1.0 - cosi, 3.0) * float3(0.85, 0.97, 0.92) * 0.25;
    col = col / (1.0 + col * 0.25);
    return half4(half3(col * alpha), alpha);
}
"""

@Composable
fun JadeOrb(look: JadeLook, animate: Boolean, tint: Color = Color(0xFF7FD1AD), modifier: Modifier = Modifier) {
    val mid = remember(look) { look.copy(solidity = (look.solidity + .55f) / 2, transparency = (look.transparency + .45f) / 2) }
    if (Build.VERSION.SDK_INT < 33) {
        Canvas(modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFA8E6D5), Color(0xFF397D70), Color(0xFF142C24)), Offset(size.width * 0.38f, size.height * 0.32f), size.minDimension * 0.5f), size.minDimension * 0.4f)
        }
        return
    }
    val reduce = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var time by remember { mutableFloatStateOf(0f) }
    val run = animate && !reduce
    LaunchedEffect(run) {
        if (!run) return@LaunchedEffect
        val start = withFrameNanos { it } - (time / (Math.PI.toFloat() * 2f / 8f) * 1e9f).toLong()
        var last = 0L
        while (true) withFrameNanos { if (it - last >= 32_000_000L) { last = it; time = (it - start) / 1e9f * (Math.PI.toFloat() * 2f / 8f) } }
    }
    val shader = remember { RuntimeShader(JADE) }
    val brush = remember { ShaderBrush(shader) }
    Canvas(modifier.fillMaxSize()) {
        shader.setFloatUniform("res", size.width, size.height)
        shader.setFloatUniform("t", time)
        shader.setFloatUniform("solidity", mid.solidity)
        shader.setFloatUniform("fluid", look.fluid)
        shader.setFloatUniform("membrane", look.membrane)
        shader.setFloatUniform("glow", look.glow)
        shader.setFloatUniform("transp", mid.transparency)
        shader.setFloatUniform("scl", look.scale)
        shader.setFloatUniform("rose", look.rose)
        shader.setFloatUniform("tint", tint.red, tint.green, tint.blue)
        drawRect(brush)
    }
}

private val PASTELS = listOf(0f to Color(0xFFE9A9B4), .18f to Color(0xFFEFB99F), .42f to Color(0xFF7FD1AD), .62f to Color(0xFF8CC6E6), .85f to Color(0xFFB3A3E0), 1f to Color(0xFFE9A9B4))

fun pastelAt(p: Float): Color {
    val x = ((p % 1f) + 1f) % 1f
    val i = PASTELS.indexOfLast { it.first <= x }.coerceIn(0, PASTELS.size - 2)
    val (a, ca) = PASTELS[i]; val (b, cb) = PASTELS[i + 1]
    val f = ((x - a) / (b - a)).let { it * it * (3 - 2 * it) }
    return androidx.compose.ui.graphics.lerp(ca, cb, f)
}

@Composable
fun JadeWater(progress: Float, animate: Boolean, look: JadeLook, modifier: Modifier = Modifier) = JadeOrb(look, animate, pastelAt(progress), modifier)

fun cycleProgress(u: com.onme.core.UserData, today: java.time.LocalDate): Float {
    if (!u.cycleTracking) return .5f
    val last = com.onme.core.allStarts(u).lastOrNull() ?: return .5f
    val mu = com.onme.core.predictCycle(u)?.muDays ?: 28
    val days = java.time.temporal.ChronoUnit.DAYS.between(last, today).toInt()
    return (((days % mu) + mu) % mu).toFloat() / mu
}
