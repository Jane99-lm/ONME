package com.onme.core

import com.onme.core.OnmeConfig as K
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.*

// SOS Location Finder V2.1 (spec ch.8). Exclusions first, then scores (0.1-1). Unknown ETA ≠ 0 min (0.1-9).

enum class SosCategory { TOILET, STORE }
enum class OpenStatus { OPEN, UNKNOWN, CLOSED, PERMANENTLY_CLOSED }
enum class AccessStatus { PUBLIC, CONDITIONAL, UNKNOWN, BLOCKED }
enum class ParkingStatus { DEDICATED, NEARBY, UNKNOWN, UNSAFE }
enum class EtaPolicy { STRICT, ESTIMATE }

data class GeoPoint(val lat: Double, val lng: Double) {
    fun offsetMeters(north: Double = 0.0, east: Double = 0.0) =
        GeoPoint(lat + north / 111_320.0, lng + east / (111_320.0 * cos(Math.toRadians(lat))))
}
data class LocationFix(val point: GeoPoint, val accuracyM: Double, val at: Instant)
data class SourceInfo(val fieldVerifiedAt: Instant? = null, val multiSourceAt: Instant? = null, val official: Boolean = false)

/** alwaysOpen, or per-day windows (close < open = past midnight). Empty windows = unknown. */
data class OpeningHours(val alwaysOpen: Boolean = false, val windows: Map<DayOfWeek, List<Pair<LocalTime, LocalTime>>> = emptyMap())

data class RawPlace(
    val id: String, val name: String, val category: SosCategory, val point: GeoPoint,
    val hours: OpeningHours?, val permanentlyClosed: Boolean = false,
    val access: AccessStatus = AccessStatus.UNKNOWN, val parking: ParkingStatus = ParkingStatus.UNKNOWN,
    /** Minutes from arrival to parked, if known. */
    val parkMin: Double? = null,
    /** Parking → entrance straight-line metres, if known. */
    val walkM: Double? = null,
    val source: SourceInfo = SourceInfo(),
)

data class EtaResult(val drive: Double?, val park: Double?, val walk: Double?, val total: Double?, val estimated: Boolean)

data class SosCandidate(
    val category: SosCategory,
    val etaMinutes: Double?,
    val openStatus: OpenStatus,
    val accessStatus: AccessStatus,
    val parkingStatus: ParkingStatus,
    val reliability: Double,
    val lastVerifiedAt: Instant? = null,
    val name: String = "",
    val place: RawPlace? = null,
    val eta: EtaResult? = null,
)

fun haversineM(a: GeoPoint, b: GeoPoint): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.lat - a.lat); val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}

private fun round2(x: Double) = Math.round(x * 100.0) / 100.0

fun timeScore(etaMin: Double): Double = max(0.0, 100.0 - K.T_PER_MIN * max(0.0, etaMin))
fun openScore(s: OpenStatus): Double? = when (s) { OpenStatus.OPEN -> 100.0; OpenStatus.UNKNOWN -> 30.0; else -> null }
fun accessScore(s: AccessStatus): Double? = when (s) { AccessStatus.PUBLIC -> 100.0; AccessStatus.CONDITIONAL -> 60.0; AccessStatus.UNKNOWN -> 30.0; AccessStatus.BLOCKED -> null }
fun parkingScore(s: ParkingStatus): Double? = when (s) { ParkingStatus.DEDICATED -> 100.0; ParkingStatus.NEARBY -> 70.0; ParkingStatus.UNKNOWN -> 40.0; ParkingStatus.UNSAFE -> null }

fun reliabilityScore(src: SourceInfo, now: Instant): Double = when {
    src.fieldVerifiedAt != null && Duration.between(src.fieldVerifiedAt, now).toDays() <= 30 -> 100.0
    src.multiSourceAt != null && Duration.between(src.multiSourceAt, now).toDays() <= 90 -> 80.0
    src.official -> 60.0
    else -> 30.0
}

/** Open status at the *expected arrival* time. */
fun openStatusAt(hours: OpeningHours?, arrival: Instant, zone: ZoneId): OpenStatus {
    if (hours == null) return OpenStatus.UNKNOWN
    if (hours.alwaysOpen) return OpenStatus.OPEN
    if (hours.windows.isEmpty()) return OpenStatus.UNKNOWN
    val t = arrival.atZone(zone)
    val now = t.toLocalTime(); val today = t.dayOfWeek; val yesterday = today.minus(1)
    val open = hours.windows[today].orEmpty().any { (o, c) -> if (c > o) now >= o && now < c else now >= o } ||
        hours.windows[yesterday].orEmpty().any { (o, c) -> c <= o && now < c }
    return if (open) OpenStatus.OPEN else OpenStatus.CLOSED
}

/** D10: STRICT drops any unknown leg; ESTIMATE fills park/walk with assumptions and flags them. Drive is never assumed. */
fun totalEta(drive: Double?, park: Double?, walk: Double?, walkDistanceM: Double?, policy: EtaPolicy): EtaResult {
    if (policy == EtaPolicy.STRICT || drive == null) {
        val total = if (drive != null && park != null && walk != null) drive + park + walk else null
        return EtaResult(drive, park, walk, total, estimated = false)
    }
    val p = park ?: K.ETA_PARK_UNKNOWN_MIN
    val w = walk ?: ((walkDistanceM ?: K.WALK_UNKNOWN_M) * K.WALK_DETOUR / K.WALK_M_PER_MIN)
    return EtaResult(drive, p, w, drive + p + w, estimated = park == null || walk == null)
}

fun scoreCandidate(c: SosCandidate): Double? {
    val eta = c.etaMinutes ?: return null
    val t = timeScore(eta)
    val o = openScore(c.openStatus) ?: return null
    if (c.parkingStatus == ParkingStatus.UNSAFE) return null // D11: both categories
    val r = c.reliability.coerceIn(0.0, 100.0)
    return when (c.category) {
        SosCategory.TOILET -> {
            val a = accessScore(c.accessStatus) ?: return null
            round2(K.TOILET_W_T * t + K.TOILET_W_O * o + K.TOILET_W_A * a + K.TOILET_W_R * r)
        }
        SosCategory.STORE -> {
            val p = parkingScore(c.parkingStatus)!!
            round2(K.STORE_W_T * t + K.STORE_W_P * p + K.STORE_W_O * o + K.STORE_W_R * r)
        }
    }
}

fun rankCandidates(cs: List<SosCandidate>): List<Pair<SosCandidate, Double>> =
    cs.mapNotNull { c -> scoreCandidate(c)?.let { c to it } }
        .sortedWith(
            compareByDescending<Pair<SosCandidate, Double>> { it.second }
                .thenBy { it.first.etaMinutes }
                .thenByDescending { it.first.lastVerifiedAt }, // D20
        )
        .take(K.SOS_TOP_N)

private fun normalize(name: String) = name.lowercase().filter { it.isLetterOrDigit() }

fun dedup(places: List<RawPlace>): List<RawPlace> = places.fold(mutableListOf()) { acc, p ->
    if (acc.none { normalize(it.name) == normalize(p.name) && haversineM(it.point, p.point) <= K.SOS_DEDUP_M }) acc += p
    acc
}

/** Static exclusions known before routing (used to decide radius expansion). */
private fun staticallyEligible(p: RawPlace) = !p.permanentlyClosed && p.parking != ParkingStatus.UNSAFE &&
    !(p.category == SosCategory.TOILET && p.access == AccessStatus.BLOCKED)

data class SosResult(val results: List<Pair<SosCandidate, Double>>, val locationWarning: Boolean, val radiusM: Int, val origin: GeoPoint)

private fun LocationFix.good(now: Instant) =
    Duration.between(at, now).seconds <= K.LOCATION_MAX_AGE_SEC && accuracyM <= K.LOCATION_MAX_ACCURACY_M

/** 8.1 end-to-end. Throws on GPS/network failure — the UI names the cause and offers retry (8.3). */
suspend fun findSos(
    cat: SosCategory, location: LocationSource, places: PlaceProvider, routes: RouteProvider,
    now: Instant, zone: ZoneId = ZoneId.of("Asia/Seoul"), policy: EtaPolicy = K.ETA_POLICY,
): SosResult {
    var fix = location.current()
    if (!fix.good(now)) fix = location.current() // one re-measure (SOS12)
    val warn = !fix.good(now)

    var found = emptyList<RawPlace>(); var radius = K.SOS_RADII_M.first()
    for (r in K.SOS_RADII_M) {
        radius = r
        found = dedup(places.search(cat, fix.point, r)).filter(::staticallyEligible)
        if (found.size >= K.SOS_MIN_ELIGIBLE) break
    }
    val nearest = found.sortedBy { haversineM(fix.point, it.point) }.take(K.SOS_ROUTE_BUFFER)
    val candidates = nearest.map { p ->
        val eta = totalEta(routes.driveEta(fix.point, p.point).drive, p.parkMin, p.walkM?.let { it * K.WALK_DETOUR / K.WALK_M_PER_MIN }, null, policy)
        val arrival = now.plusSeconds(((eta.total ?: eta.drive ?: 0.0) * 60).toLong())
        SosCandidate(
            category = cat, etaMinutes = eta.total,
            openStatus = if (p.permanentlyClosed) OpenStatus.PERMANENTLY_CLOSED else openStatusAt(p.hours, arrival, zone),
            accessStatus = p.access, parkingStatus = p.parking,
            reliability = reliabilityScore(p.source, now),
            lastVerifiedAt = p.source.fieldVerifiedAt ?: p.source.multiSourceAt,
            name = p.name, place = p, eta = eta,
        )
    }
    return SosResult(rankCandidates(candidates), warn, radius, fix.point)
}
