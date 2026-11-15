package com.onme.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.onme.core.UserData
import com.onme.core.allStarts
import com.onme.core.predictCycle
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val ORIGIN = "https://appassets.androidplatform.net/"

fun cycleModelUrl(u: UserData, today: LocalDate): String {
    val f = if (u.cycleTracking) predictCycle(u) else null
    val mu = (f?.muDays ?: 28).coerceIn(15, 60)
    val first = today.withDayOfMonth(1)
    val last = allStarts(u).lastOrNull().takeIf { u.cycleTracking } ?: first
    val rel = ChronoUnit.DAYS.between(first, last).toInt()
    val start = Math.floorMod(rel, mu) + 1
    return "${ORIGIN}onme/index.html?month=${today.format(DateTimeFormatter.ofPattern("yyyy-MM"))}&day=${today.dayOfMonth}" +
        "&period=${start.coerceIn(1, today.lengthOfMonth())}&cycle=$mu&periodDays=${(f?.periodDays ?: 5).coerceIn(1, 10)}" +
        "&theme=light"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CycleModel(url: String, restricted: Boolean, modifier: Modifier = Modifier) {
    val state = remember { arrayOf<String?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                        val u = request.url
                        if (u.scheme == "https" && u.host == "appassets.androidplatform.net" && u.path?.startsWith("/onme/") == true) {
                            runCatching { return WebResourceResponse("text/html", "UTF-8", ctx.assets.open(u.path!!.removePrefix("/"))) }
                        }
                        return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                    override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                        (view.parent as? android.view.ViewGroup)?.removeView(view); view.destroy(); return true
                    }
                }
            }
        },
        update = { w ->
            if (state[0] != url) { state[0] = url; w.loadUrl("$url&restricted=${if (restricted) 1 else 0}") }
            else w.evaluateJavascript("window.onmeMotion?.setDrivingRestricted($restricted)", null)
        },
        onRelease = { it.stopLoading(); it.destroy() },
    )
}
