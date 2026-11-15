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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

private const val BEAR_W = 900f
private const val BEAR_H = 1180f
private const val BALL_X = 442f
private const val BALL_Y = 840f
private const val BALL_R = 178f

private const val BALL = """
uniform float2 c;
uniform float r;
uniform float t;

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
    float tw = 0.35 * t + 1.6 * (1.0 - d);
    float2 q = float2(cos(tw) * p.x - sin(tw) * p.y, sin(tw) * p.x + cos(tw) * p.y);
    float cloud = smoothstep(0.38, 0.72, fbm(float3(q * 3.2, 0.12 * t)));
    float vein = pow(1.0 - abs(fbm(float3(q * 2.2 + 5.0, 0.09 * t + 2.0)) * 2.0 - 1.0), 7.0);
    float core = exp(-d * d * 5.0) * (0.85 + 0.15 * sin(1.2 * t));
    float3 col = mix(float3(0.2, 0.62, 0.3), float3(0.72, 1.0, 0.6), cloud);
    col += vein * float3(0.75, 1.0, 0.7) * 0.6;
    col = mix(col, float3(0.98, 1.0, 0.85), core);
    col += pow(d, 6.0) * float3(0.16, 0.32, 0.2);
    float a = smoothstep(1.0, 0.9, d) * 0.66;
    return half4(half3(col * a), a);
}
"""

@Composable
fun BearOrb(animate: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(BEAR_W / BEAR_H, matchHeightConstraintsFirst = true)) {
        Image(painterResource(R.drawable.bear_base), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        if (Build.VERSION.SDK_INT >= 33) Ball(animate)
        Image(painterResource(R.drawable.bear_hands), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@androidx.annotation.RequiresApi(33)
@Composable
private fun Ball(animate: Boolean) {
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
        drawRect(brush)
    }
}
