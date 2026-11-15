package com.onme.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Suppress("EnumEntryName")
enum class P { scale, scaleX, scaleY, rotate, x, y, opacity, strokeDashoffset, pathLength }

class Track(val p: P, val values: FloatArray, val times: FloatArray, val dur: Float, val delay: Float, val ease: Easing, val rest: Float) {
    val end get() = delay + dur
    fun at(t: Float): Float {
        if (t >= end) return rest
        val x = ((t - delay) / dur).coerceIn(0f, 1f)
        val i = (1 until times.size).firstOrNull { x <= times[it] } ?: (times.size - 1)
        val a = times[i - 1]; val b = times[i]
        val f = if (b > a) ease.transform((x - a) / (b - a)) else 1f
        return values[i - 1] + (values[i] - values[i - 1]) * f
    }
}

class Node(val d: String?, val tracks: List<Track>, val origin: Offset?, val dash: FloatArray?, val kids: List<Node>) {
    val path by lazy { d?.let { PathParser().parsePathString(it).toPath() } }
    val bounds: Rect by lazy {
        path?.getBounds() ?: kids.map { it.bounds }.reduce { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
    }
    val length by lazy { path?.let { PathMeasure().apply { setPath(it, false) }.length } ?: 0f }
    val end: Float get() = maxOf(tracks.maxOfOrNull { it.end } ?: 0f, kids.maxOfOrNull { it.end } ?: 0f)
}

class AnimIcon(val nodes: List<Node>) {
    val length by lazy { nodes.maxOf { it.end } }
}

private fun DrawScope.draw(n: Node, t: Float, color: Color, alpha: Float, stroke: Float) {
    var sx = 1f; var sy = 1f; var rot = 0f; var tx = 0f; var ty = 0f; var a = 1f; var off = 0f; var drawn: Float? = null
    n.tracks.forEach {
        val v = it.at(t)
        when (it.p) {
            P.scale -> { sx *= v; sy *= v }
            P.scaleX -> sx *= v
            P.scaleY -> sy *= v
            P.rotate -> rot = v
            P.x -> tx = v
            P.y -> ty = v
            P.opacity -> a = v
            P.strokeDashoffset -> off = v
            P.pathLength -> drawn = v
        }
    }
    val pivot = n.origin ?: n.bounds.center
    withTransform({ translate(tx, ty); rotate(rot, pivot); scale(sx, sy, pivot) }) {
        n.path?.let { p ->
            val effect = drawn?.let { PathEffect.dashPathEffect(floatArrayOf(n.length, n.length), n.length * (1f - it)) }
                ?: n.dash?.let { PathEffect.dashPathEffect(if (it.size % 2 == 1) it + it else it, off) }
            drawPath(p, color, alpha = (a * alpha).coerceIn(0f, 1f), style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))
        }
        n.kids.forEach { draw(it, t, color, a * alpha, stroke) }
    }
}

const val STROKE = 1.6f

@Composable
fun Glyph(icon: AnimIcon, color: Color, size: Dp = 34.dp, play: Any? = null, stroke: Float = STROKE) {
    val time = remember(icon) { Animatable(icon.length) }
    LaunchedEffect(icon, play) {
        if (play != null && play != false) { time.snapTo(0f); time.animateTo(icon.length, tween((icon.length * 1000).toInt(), easing = LinearEasing)) }
    }
    Canvas(Modifier.size(size)) {
        val k = this.size.minDimension / 24f
        val t = time.value
        scale(k, k, pivot = Offset.Zero) { icon.nodes.forEach { draw(it, t, color, 1f, stroke) } }
    }
}
