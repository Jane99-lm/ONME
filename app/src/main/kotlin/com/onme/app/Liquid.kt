package com.onme.app

import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

private const val LIQUID = """
uniform float2 res;
uniform float t;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float noise(float2 p) {
    float2 i = floor(p); float2 f = fract(p); float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1, 0)), u.x), mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), u.x), u.y);
}
float fbm(float2 p) {
    float v = 0.0; float a = 0.5;
    for (int i = 0; i < 4; i++) { v += a * noise(p); p = p * 2.03 + float2(1.7, 9.2); a *= 0.5; }
    return v;
}
float height(float2 p, float2 q) { return fbm(p + 1.8 * q + float2(t * 0.025, 0.0)); }

half4 main(float2 fc) {
    float2 p = fc / res.y * 1.25;
    float2 q = float2(fbm(p + float2(0.0, t * 0.06)), fbm(p + float2(5.2, 1.3) - t * 0.05));
    float h = height(p, q);
    float e = 0.006;
    float hx = height(p + float2(e, 0.0), q);
    float hy = height(p + float2(0.0, e), q);
    float3 n = normalize(float3((h - hx) / e, (h - hy) / e, 3.2));

    float3 l = normalize(float3(-0.45, -0.65, 0.9));
    float diff = clamp(dot(n, l), 0.0, 1.0);
    float spec = pow(clamp(dot(reflect(-l, n), float3(0.0, 0.0, 1.0)), 0.0, 1.0), 90.0);
    float rim = pow(1.0 - n.z, 2.0);

    float3 pale = float3(0.933, 0.969, 0.953);
    float3 jade = float3(0.616, 0.835, 0.741);
    float3 deep = float3(0.639, 0.780, 0.690);
    float3 col = mix(pale, jade, smoothstep(0.35, 0.85, h) * 0.55);
    col = mix(col, deep, rim * 0.9);
    col *= 0.93 + 0.09 * diff;
    float band = 1.0 - smoothstep(0.0, 0.045, abs(fract(h * 7.0) - 0.5));
    col += band * 0.018;
    col += spec * 0.30;
    float2 c = fc / res - 0.5;
    col = mix(col, pale, 0.35 * (1.0 - smoothstep(0.0, 0.45, length(c * float2(1.6, 1.0)))));
    return half4(half3(col), 1.0);
}
"""

val Beige = Color(0xFFF5F1E7)

private class Liquid { var time by mutableFloatStateOf(12f); var origin = Offset.Zero; var size = androidx.compose.ui.geometry.Size.Zero }
private val LocalLiquid = staticCompositionLocalOf<Liquid?> { null }

@Composable
fun LiquidGround(animate: Boolean, beige: Boolean, content: @Composable BoxScope.() -> Unit) {
    val reduceMotion = Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    if (beige) return Box(Modifier.fillMaxSize().background(Beige), content = content)
    if (Build.VERSION.SDK_INT < 33) {
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Pale)
            drawRect(Brush.radialGradient(listOf(Point.copy(alpha = 0.30f), Color.Transparent), Offset(size.width * 0.12f, size.height * 0.05f), size.width * 0.70f))
        }, content = content)
        return
    }
    val liquid = remember { Liquid() }
    val run = animate && !reduceMotion
    LaunchedEffect(run) {
        if (!run) return@LaunchedEffect
        val start = withFrameNanos { it } - (liquid.time * 1e9f).toLong()
        var last = 0L
        while (true) withFrameNanos { if (it - last >= 32_000_000L) { last = it; liquid.time = (it - start) / 1e9f } }
    }
    CompositionLocalProvider(LocalLiquid provides liquid) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { liquid.origin = it.positionInRoot(); liquid.size = it.size.toSize() }) {
            Box(
                Modifier.fillMaxSize(0.5f).graphicsLayer {
                    scaleX = 2f; scaleY = 2f; transformOrigin = TransformOrigin(0f, 0f); compositingStrategy = CompositingStrategy.Offscreen
                }.then(waterModifier(liquid, Offset.Zero, ownSize = true))
            )
            content()
        }
    }
}

@RequiresApi(33)
@Composable
private fun waterModifier(liquid: Liquid, offset: Offset, ownSize: Boolean = false): Modifier {
    val shader = remember { RuntimeShader(LIQUID) }
    val brush = remember { ShaderBrush(shader) }
    return Modifier.drawBehind {
        val full = if (ownSize || liquid.size == androidx.compose.ui.geometry.Size.Zero) size else liquid.size
        shader.setFloatUniform("res", full.width, full.height)
        shader.setFloatUniform("t", liquid.time)
        translate(-offset.x, -offset.y) { drawRect(brush, size = full) }
    }
}

@Composable
fun GlassPane(shape: Shape, modifier: Modifier = Modifier, strength: Float = 1f, content: @Composable BoxScope.() -> Unit) {
    val liquid = LocalLiquid.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned { pos = it.positionInRoot() }.clip(shape)) {
        if (liquid != null && Build.VERSION.SDK_INT >= 33) {
            Box(Modifier.matchParentSize().blur(26.dp, BlurredEdgeTreatment.Unbounded).then(waterModifier(liquid, pos - liquid.origin)))
        }
        Box(Modifier.matchParentSize().drawBehind {
            drawRect(Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.58f * strength), 0.55f to Color.White.copy(alpha = 0.26f * strength), 1f to Color.White.copy(alpha = 0.16f * strength),
                start = Offset(size.width * 0.2f, 0f), end = Offset(size.width * 0.8f, size.height),
            ))
            drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Point.copy(alpha = 0.16f)))
            drawRect(Brush.linearGradient(
                0.38f to Color.Transparent, 0.46f to Color.White.copy(alpha = 0.32f * strength), 0.52f to Color.White.copy(alpha = 0.06f), 0.60f to Color.Transparent,
                start = Offset(-size.width * 0.3f, 0f), end = Offset(size.width * 1.1f, size.height * 0.9f),
            ))
        }.border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.15f), StartRim)), shape))
        content()
    }
}

fun Modifier.glass(shape: Shape, strength: Float = 1f) = this
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.62f * strength), Color.White.copy(alpha = 0.34f * strength))), shape)
    .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.15f), StartRim)), shape)

fun Modifier.bead(tint: Color) = this
    .drawBehind {
        val rim = Color(tint.red * 0.86f, tint.green * 0.88f, tint.blue * 0.88f)
        drawCircle(Brush.radialGradient(listOf(rim.copy(alpha = 0.45f), Color.Transparent), center + Offset(0f, size.height * 0.12f), size.minDimension * 0.62f))
        drawCircle(Brush.radialGradient(0f to lerpWhite(tint, 0.75f), 0.5f to tint.copy(alpha = 0.92f), 1f to rim, center = Offset(size.width * 0.36f, size.height * 0.30f), radius = size.minDimension * 0.78f))
        drawCircle(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.White.copy(alpha = 0.45f)))
        rotate(-24f, Offset(size.width * 0.40f, size.height * 0.24f)) {
            drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), Color.Transparent), Offset(size.width * 0.40f, size.height * 0.24f), size.minDimension * 0.24f),
                topLeft = Offset(size.width * 0.16f, size.height * 0.09f), size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * 0.30f))
        }
    }
    .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.0f))), CircleShape)

private val StartRim = Color(0xFF3F8069).copy(alpha = 0.12f)
private fun lerpWhite(c: Color, f: Float) = Color(c.red + (1 - c.red) * f, c.green + (1 - c.green) * f, c.blue + (1 - c.blue) * f)
