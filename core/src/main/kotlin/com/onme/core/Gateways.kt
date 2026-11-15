package com.onme.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

// 2.3 external integrations — interfaces + mocks first. Real Pleos adapters replace mocks after ch.11 checks.

data class ApplyResult(val ok: Boolean, val applied: VehicleSettings?)

interface VehicleGateway {
    fun capabilities(): VehicleCapabilities
    suspend fun readCabin(): CabinState
    /** Only ever called after the user tapped apply (0.1-4, MC12). */
    suspend fun apply(settings: VehicleSettings): ApplyResult
}
interface PlaceProvider { suspend fun search(cat: SosCategory, center: GeoPoint, radiusM: Int): List<RawPlace> }
interface RouteProvider { suspend fun driveEta(from: GeoPoint, to: GeoPoint): EtaResult }
interface NavigationLauncher { fun isSupported(): Boolean; suspend fun sendDestination(name: String, point: GeoPoint, asWaypoint: Boolean): Boolean }
data class Playlist(val id: String, val title: String, val tag: String)
interface MusicProvider { fun isSupported(): Boolean; suspend fun playlistsFor(tag: String): List<Playlist>; suspend fun open(p: Playlist): Boolean }
interface LocationSource { suspend fun current(): LocationFix }

/** Applies an approved change, remembering the previous cabin state for [revert] (7.7). */
class CareRunner(private val vehicle: VehicleGateway) {
    private var before: Pair<CabinState, VehicleSettings>? = null

    suspend fun approve(settings: VehicleSettings): ApplyResult {
        val prev = vehicle.readCabin()
        return vehicle.apply(settings).also { if (it.ok) before = prev to settings }
    }

    val canRevert get() = before != null

    suspend fun revert(): ApplyResult? {
        val (prev, changed) = before ?: return null
        before = null
        return vehicle.apply(VehicleSettings(
            seatHeatLevel = prev.seatHeatLevel.takeIf { changed.seatHeatLevel != null },
            seatVentLevel = prev.seatVentLevel.takeIf { changed.seatVentLevel != null },
            cabinTempC = prev.cabinTempC.takeIf { changed.cabinTempC != null },
            fanLevel = prev.fanLevel.takeIf { changed.fanLevel != null },
            ambientDim = prev.ambientDim.takeIf { changed.ambientDim != null },
            seatProfileId = prev.seatProfileId.takeIf { changed.seatProfileId != null },
        ))
    }
}

/** Simulated car for demo mode and tests; capabilities toggled from the demo panel. */
class MockVehicleGateway(var caps: VehicleCapabilities = VehicleCapabilities()) : VehicleGateway {
    val cabin = MutableStateFlow(CabinState())
    var applyCount = 0; private set
    override fun capabilities() = caps
    override suspend fun readCabin() = cabin.value
    override suspend fun apply(settings: VehicleSettings): ApplyResult {
        applyCount++
        val s = supportedPart(settings, caps, DrivingState.PARKED) ?: return ApplyResult(false, null)
        cabin.value = cabin.value.let { c ->
            c.copy(
                seatHeatLevel = s.seatHeatLevel ?: c.seatHeatLevel, seatVentLevel = s.seatVentLevel ?: c.seatVentLevel,
                cabinTempC = s.cabinTempC ?: c.cabinTempC, fanLevel = s.fanLevel ?: c.fanLevel,
                ambientDim = s.ambientDim ?: c.ambientDim, seatProfileId = s.seatProfileId ?: c.seatProfileId,
            )
        }
        return ApplyResult(true, s)
    }
}

/** Crow-flies distance at an assumed 30 km/h urban speed. ponytail: mock only; real route ETA is A9. */
class StraightLineRoutes(private val metersPerMin: Double = 500.0) : RouteProvider {
    override suspend fun driveEta(from: GeoPoint, to: GeoPoint): EtaResult {
        val d = haversineM(from, to) * 1.3 / metersPerMin
        return EtaResult(d, null, null, d, estimated = true)
    }
}

class MockPlaceProvider(private val places: () -> List<RawPlace>) : PlaceProvider {
    override suspend fun search(cat: SosCategory, center: GeoPoint, radiusM: Int) =
        places().filter { it.category == cat && haversineM(center, it.point) <= radiusM }
}

class MockMusicProvider(var supported: Boolean = true) : MusicProvider {
    var opened: Playlist? = null; private set
    override fun isSupported() = supported
    override suspend fun playlistsFor(tag: String) = listOf("1", "2", "3").map { Playlist("$tag-$it", "$tag $it", tag) }
    override suspend fun open(p: Playlist): Boolean { opened = p; return true }
}

class MockNavigationLauncher(var supported: Boolean = true) : NavigationLauncher {
    val sent = MutableStateFlow<String?>(null)
    override fun isSupported() = supported
    override suspend fun sendDestination(name: String, point: GeoPoint, asWaypoint: Boolean): Boolean {
        if (!supported) return false
        sent.value = (if (asWaypoint) "경유지: " else "목적지: ") + name
        return true
    }
}

class FixedLocation(var point: GeoPoint, private val now: () -> Instant = Instant::now) : LocationSource {
    override suspend fun current() = LocationFix(point, 5.0, now())
}

// ---- 3.1 screens and driving restriction ---------------------------------------------------

enum class Screen(val parkedOnly: Boolean) {
    LOADING(false), PROFILE(false), ONBOARDING(true), HOME(false), GOAL(false), RESULT(false), CONFIRM(false), RESET(true),
    FEEDBACK(true), CALENDAR(true), RECORD(true), SOS(false), SETTINGS(true),
}

fun canOpen(s: Screen, state: DrivingState) = !s.parkedOnly || state == DrivingState.PARKED

/** Leaving PARKED closes parked-only screens back to HOME (drafts are kept by the caller). */
fun screenAfter(s: Screen, state: DrivingState) = if (canOpen(s, state)) s else Screen.HOME

// ---- 9 trigger cards ------------------------------------------------------------------------

object Cards {
    const val TR2 = "TR2"; const val TR3 = "TR3"

    /** D12: snoozed 24h after [나중에]; TR2 at most 2 per drive; consent required. */
    fun canShow(u: UserData, id: String, now: Instant, shownThisDrive: Int): Boolean =
        u.consents.proactiveCards &&
            (id == TR3 || shownThisDrive < OnmeConfig.MAX_CARDS_PER_DRIVE) &&
            u.cardSnoozedUntil[id]?.isAfter(now) != true

    fun snooze(u: UserData, id: String, now: Instant) =
        u.copy(cardSnoozedUntil = u.cardSnoozedUntil + (id to now.plusSeconds(OnmeConfig.CARD_SNOOZE_HOURS * 3600)))

    fun longDrive(u: UserData, drivingMinutes: Long) = drivingMinutes >=
        if (u.onboarding?.drivingPattern == DrivingPattern.LONG_DISTANCE) OnmeConfig.LONG_DRIVE_MIN_FREQUENT else OnmeConfig.LONG_DRIVE_MIN

    fun readyKit(u: UserData, ctx: CareContext) =
        ctx.parked && predictCycle(u)?.let { !it.paused && readyKitDate(it) == ctx.today } == true
}
