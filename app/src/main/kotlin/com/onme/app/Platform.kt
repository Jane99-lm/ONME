package com.onme.app

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.onme.core.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

class OnmeApplication : Application() {
    lateinit var onme: Onme; private set
    override fun onCreate() {
        super.onCreate()
        onme = Onme(this)
    }
}

/**
 * 10.1: health data stays on device, AES-GCM encrypted with a non-exportable Android Keystore key.
 * AAOS keeps app data per Android user; inside the app each driver profile also gets its own file (10.1-3).
 */
class SecureStore(context: Context, profileId: String) {
    private val file = File(context.filesDir, "onme-$profileId.bin")

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    fun load(): UserData? {
        if (!file.exists()) return null
        return try {
            val bytes = file.readBytes()
            val c = Cipher.getInstance(AES).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12)) }
            UserStore.decode(String(c.doFinal(bytes, 12, bytes.size - 12)))
        } catch (e: Exception) {
            // Never log contents (10.1-5). Keep the unreadable file aside instead of overwriting it.
            Log.w(TAG, "stored data unreadable: ${e.javaClass.simpleName}")
            file.renameTo(File(file.parentFile, "onme.unreadable.${System.currentTimeMillis()}"))
            null
        }
    }

    fun save(u: UserData) {
        val c = Cipher.getInstance(AES).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val tmp = File(file.parentFile, "onme.tmp")
        tmp.writeBytes(c.iv + c.doFinal(UserStore.encode(u).toByteArray()))
        tmp.renameTo(file)
    }

    fun deleteAll() { file.delete() }

    private companion object { const val ALIAS = "onme-data"; const val AES = "AES/GCM/NoPadding"; const val TAG = "SecureStore" }
}

/** A8: Android GPS (Pleos FusedLocation didn't resolve). Throws when there's no permission or fix. */
class AndroidLocation(private val context: Context) : LocationSource {
    fun permitted() = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    override suspend fun current(): LocationFix {
        check(permitted()) { "no location permission" }
        val lm = context.getSystemService(LocationManager::class.java)
        val loc = withTimeout(10_000) {
            suspendCancellableCoroutine { cont ->
                lm.getCurrentLocation(LocationManager.GPS_PROVIDER, null, context.mainExecutor) { cont.resume(it) }
            }
        } ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: error("no fix")
        return LocationFix(GeoPoint(loc.latitude, loc.longitude), loc.accuracy.toDouble(), Instant.ofEpochMilli(loc.time))
    }
}

/**
 * A7 not verified yet: hands a geo: intent to whatever navigation app is installed.
 * Waypoint vs destination is up to that app. ponytail: swap for the Pleos navigation SDK once confirmed.
 */
class GeoIntentNavigation(private val context: Context) : NavigationLauncher {
    private fun intent(name: String, p: GeoPoint) =
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:${p.lat},${p.lng}?q=${p.lat},${p.lng}(${Uri.encode(name)})")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    override fun isSupported() = intent("", GeoPoint(0.0, 0.0)).resolveActivity(context.packageManager) != null
    override suspend fun sendDestination(name: String, point: GeoPoint, asWaypoint: Boolean): Boolean =
        runCatching { context.startActivity(intent(name, point)) }.isSuccess
}

class PleosNavigation(context: Context) : NavigationLauncher {
    private val helper = ai.pleos.playground.navi.helper.NaviHelper(context.applicationContext)
    @Volatile private var connected = false
    @Volatile private var routeActive = false
    @Volatile private var pending: kotlinx.coroutines.CompletableDeferred<ai.pleos.playground.navi.constants.NaviErrorCode?>? = null
    @Volatile var lastError: String? = null; private set

    private val listener = object : ai.pleos.playground.navi.helper.listener.NaviHelperEventListener {
        override fun onNavigationStatus(navigationStatus: ai.pleos.playground.navi.constants.NavigationStatus) {
            routeActive = navigationStatus == ai.pleos.playground.navi.constants.NavigationStatus.ROUTE_DRIVE
        }
        override fun onRouteStarted(routeStartInfo: ai.pleos.playground.navi.data.RouteStartInfo) { routeActive = true; pending?.complete(null) }
        override fun onWaypointChanged(waypointChangedInfo: ai.pleos.playground.navi.data.WaypointChangedInfo) { pending?.complete(null) }
        override fun onRouteCancelled() { routeActive = false }
        override fun onDestinationArrived(destinationArrivedInfo: ai.pleos.playground.navi.data.DestinationArrivedInfo) { routeActive = false }
        override fun onError(errorCode: ai.pleos.playground.navi.constants.NaviErrorCode) { pending?.complete(errorCode) }
    }

    init {
        runCatching {
            helper.initialize(
                onConnected = { connected = true; helper.addListener(listener) },
                onFailed = { connected = false; Log.w(TAG, "NaviHelper unavailable: ${it.javaClass.simpleName}") },
            )
        }
    }

    override fun isSupported() = connected

    override suspend fun sendDestination(name: String, point: GeoPoint, asWaypoint: Boolean): Boolean {
        if (!connected) return false
        val wait = kotlinx.coroutines.CompletableDeferred<ai.pleos.playground.navi.constants.NaviErrorCode?>().also { pending = it }
        val sent = runCatching {
            if (asWaypoint && routeActive) helper.addWaypoint(ai.pleos.playground.navi.data.RequestWaypointInfo(
                latitude = point.lat, longitude = point.lng, waypointIndex = ai.pleos.playground.navi.constants.WaypointIndex.FIRST,
                poiId = "", poiName = name, poiSubId = "0", address = name,
            ))
            else helper.requestRoute(ai.pleos.playground.navi.data.RouteInfo(
                latitude = point.lat, longitude = point.lng, displayLatitude = null, displayLongitude = null,
                poiName = name, poiId = "", poiSubId = "0", address = name,
            ))
        }.isSuccess
        if (!sent) { lastError = NAVI_FAILED; return false }
        val error = kotlinx.coroutines.withTimeoutOrNull(5_000) { wait.await() }
        pending = null
        lastError = error?.let(::message)
        return error == null
    }

    private fun message(e: ai.pleos.playground.navi.constants.NaviErrorCode) = when (e) {
        ai.pleos.playground.navi.constants.NaviErrorCode.CAUSE_LOW_BATTERY -> "배터리가 부족해 경로를 안내할 수 없어요"
        ai.pleos.playground.navi.constants.NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> "너무 멀어 경로를 찾을 수 없어요"
        ai.pleos.playground.navi.constants.NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> "목적지가 지금 위치와 너무 가까워요"
        else -> NAVI_FAILED
    }

    private companion object {
        const val TAG = "PleosNavigation"
        const val NAVI_FAILED = "경로를 찾지 못했어요. 잠시 후 다시 시도해 주세요"
    }
}
