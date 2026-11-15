package com.onme.app

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

private const val BEAR_W = 1040f
private const val BEAR_H = 1160f
private const val BALL_X = 512f
private const val BALL_Y = 770f
private const val BALL_R = 178f

private const val BALL = """
uniform float2 c;
uniform float r;
uniform float t;
uniform float3 deep;
uniform float3 milky;

float hash(float3 p) { return fract(sin(dot(p, float3(127.1, 311.7, 74.7))) * 43758.5453); }
float noise(float3 p) {
    float3 i = floor(p); float3 f = fract(p); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + float3(1, 0, 0)), f.x), mix(hash(i + float3(0, 1, 0)), hash(i + float3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(i + float3(0, 0, 1)), hash(i + float3(1, 0, 1)), f.x), mix(hash(i + float3(0, 1, 1)), hash(i + float3(1, 1, 1)), f.x), f.y), f.z);
}
float fbm(float3 p) { return 0.5 * noise(p) + 0.25 * noise(p * 2.07 + 3.1) + 0.125 * noise(p * 4.13 + 7.7); }

half4 main(float2 fc) {
    float2 p = (fc - c) / r;
    float d = length(p);
    if (d > 1.0) return half4(0.0);
    float marble = fbm(float3(p * 1.6, 0.05 * t));
    float3 col = mix(deep, milky, exp(-d * d * 2.2) * (0.75 + 0.25 * sin(0.8 * t)));
    col = mix(col, col * 1.18 + 0.04, smoothstep(0.45, 0.75, marble) * 0.5);
    float spec = exp(-dot(p - float2(-0.38, -0.42), p - float2(-0.38, -0.42)) * 18.0);
    col += spec * 0.35;
    col += pow(d, 5.0) * deep * 0.5;
    float a = smoothstep(1.0, 0.94, d) * 0.72;
    return half4(half3(col * a), a);
}
"""

enum class OrbTone(val deep: Color, val milky: Color, val glow: Color) {
    JADE(Color(0xFF216B42), Color(0xFF9EDBA8), Color(0xFF4FBF6A)),
    CRIMSON(Color(0xFF730A1A), Color(0xFFEB6B75), Color(0xFFC0283C)),
    BLUE(Color(0xFF14406E), Color(0xFF8EC2EE), Color(0xFF3F8FD8)),
}

@Composable
fun BearOrb(animate: Boolean, tone: OrbTone, modifier: Modifier = Modifier) {
    val deep by animateColorAsState(tone.deep, tween(1200), label = "deep")
    val milky by animateColorAsState(tone.milky, tween(1200), label = "milky")
    val glow by animateColorAsState(tone.glow, tween(1200), label = "glow")
    Box(modifier.aspectRatio(BEAR_W / BEAR_H, matchHeightConstraintsFirst = true).drawBehind {
        val c = Offset(size.width * 0.5f, size.height * 0.6f)
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.4f), glow.copy(alpha = 0.13f), Color.Transparent), c, size.width * 0.95f), size.width * 0.95f, c)
    }) {
        Image(painterResource(R.drawable.bear_base), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        if (Build.VERSION.SDK_INT >= 33) Ball(animate, deep, milky)
        Image(painterResource(R.drawable.bear_hands), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@androidx.annotation.RequiresApi(33)
@Composable
private fun Ball(animate: Boolean, deep: Color, milky: Color) {
    val reduce = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate && !reduce) {
        if (!animate || reduce) return@LaunchedEffect
        val start = withFrameNanos { it } - (time * 1e9f).toLong()
        var last = 0L
        while (true) withFrameNanos { if (it - last >= 32_000_000L) { last = it; time = (it - start) / 1e9f } }
    }
    val shader = remember { RuntimeShader(BALL) }
    val brush = remember { ShaderBrush(shader) }
    Canvas(Modifier.fillMaxSize()) {
        val k = size.width / BEAR_W
        shader.setFloatUniform("c", BALL_X * k, BALL_Y * k)
        shader.setFloatUniform("r", BALL_R * k)
        shader.setFloatUniform("t", time)
        shader.setFloatUniform("deep", deep.red, deep.green, deep.blue)
        shader.setFloatUniform("milky", milky.red, milky.green, milky.blue)
        drawRect(brush)
    }
}
