package com.onme.core

import com.onme.core.SosCategory.*
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SosTest {
    private fun toilet(t: Double?, o: OpenStatus = OpenStatus.OPEN, a: AccessStatus = AccessStatus.PUBLIC, r: Double, p: ParkingStatus = ParkingStatus.UNKNOWN, name: String = "") =
        SosCandidate(TOILET, t, o, a, p, r, name = name)
    private fun store(t: Double?, p: ParkingStatus, r: Double, o: OpenStatus = OpenStatus.OPEN, name: String = "") =
        SosCandidate(STORE, t, o, AccessStatus.UNKNOWN, p, r, name = name)

    @Test fun `SOS1 time score`() = assertEquals(
        listOf(95.0, 90.0, 85.0, 80.0, 75.0, 70.0, 50.0, 25.0, 0.0, 0.0),
        listOf(1, 2, 3, 4, 5, 6, 10, 15, 20, 25).map { timeScore(it.toDouble()) },
    )

    @Test fun `SOS2-3 toilets`() {
        val a = toilet(3.0, a = AccessStatus.CONDITIONAL, r = 60.0, name = "A")
        val b = toilet(5.0, r = 80.0, name = "B")
        assertEquals(84.5, scoreCandidate(a)); assertEquals(86.5, scoreCandidate(b))
        assertEquals(listOf("B", "A"), rankCandidates(listOf(a, b)).map { it.first.name })
    }

    @Test fun `SOS4-5 stores`() {
        val a = store(3.0, ParkingStatus.UNKNOWN, 80.0, name = "A")
        val b = store(5.0, ParkingStatus.DEDICATED, 80.0, name = "B")
        assertEquals(73.5, scoreCandidate(a)); assertEquals(86.5, scoreCandidate(b))
        assertEquals(listOf("B", "A"), rankCandidates(listOf(a, b)).map { it.first.name })
    }

    @Test fun `SOS6 exclusions`() {
        assertNull(scoreCandidate(toilet(3.0, o = OpenStatus.CLOSED, r = 60.0)))
        assertNull(scoreCandidate(toilet(3.0, o = OpenStatus.PERMANENTLY_CLOSED, r = 60.0)))
        assertNull(scoreCandidate(toilet(3.0, a = AccessStatus.BLOCKED, r = 60.0)))
        assertNull(scoreCandidate(store(3.0, ParkingStatus.UNSAFE, 60.0)))
        assertNull(scoreCandidate(toilet(3.0, p = ParkingStatus.UNSAFE, r = 60.0)))
    }

    @Test fun `SOS7 unknown eta never becomes zero`() {
        assertNull(scoreCandidate(toilet(null, r = 60.0)))
        assertNull(totalEta(5.0, null, 2.0, null, EtaPolicy.STRICT).total)
        assertNull(totalEta(null, 1.0, 1.0, null, EtaPolicy.ESTIMATE).total)
        val est = totalEta(5.0, null, null, 134.0, EtaPolicy.ESTIMATE)
        assertEquals(5.0 + 1.0 + 134.0 * 1.3 / 67.0, est.total!!, 1e-9); assertTrue(est.estimated)
    }

    @Test fun `SOS8 unknown opening hours`() {
        val c = toilet(3.0, o = OpenStatus.UNKNOWN, r = 60.0)
        assertEquals(69.5, scoreCandidate(c))
        assertEquals("운영시간 미확인", SosCopy.open(OpenStatus.UNKNOWN))
    }

    @Test fun `SOS9 tie broken by eta`() {
        val slow = toilet(6.0, a = AccessStatus.PUBLIC, r = 60.0, name = "6")
        val fast = toilet(4.0, a = AccessStatus.CONDITIONAL, r = 80.0, name = "4")
        assertEquals(scoreCandidate(slow), scoreCandidate(fast))
        assertEquals(listOf("4", "6"), rankCandidates(listOf(slow, fast)).map { it.first.name })
    }

    private val center = GeoPoint(37.5663, 126.9779)
    private val now = Instant.parse("2026-11-04T03:00:00Z")

    private fun placeAt(id: String, meters: Double) = RawPlace(
        id, "화장실$id", TOILET, center.offsetMeters(north = meters), OpeningHours(alwaysOpen = true),
        access = AccessStatus.PUBLIC, parking = ParkingStatus.DEDICATED, parkMin = 1.0, walkM = 50.0, source = SourceInfo(official = true),
    )

    private class CountingPlaces(val places: List<RawPlace>) : PlaceProvider {
        val radii = mutableListOf<Int>()
        override suspend fun search(cat: SosCategory, center: GeoPoint, radiusM: Int): List<RawPlace> {
            radii += radiusM
            return places.filter { it.category == cat && haversineM(center, it.point) <= radiusM }
        }
    }

    private class Fixed(vararg val fixes: LocationFix) : LocationSource {
        var calls = 0
        override suspend fun current() = fixes[minOf(calls++, fixes.lastIndex)]
    }

    @Test fun `SOS10 expands radius until three eligible`() = runBlocking {
        val places = CountingPlaces(listOf(placeAt("1", 300.0), placeAt("2", 800.0), placeAt("3", 2500.0)))
        val r = findSos(TOILET, Fixed(LocationFix(center, 10.0, now)), places, StraightLineRoutes(), now)
        assertEquals(listOf(1000, 3000), places.radii)
        assertEquals(3, r.results.size)
    }

    @Test fun `SOS11 top three only`() = runBlocking {
        val places = CountingPlaces((1..5).map { placeAt("$it", it * 100.0) })
        val r = findSos(TOILET, Fixed(LocationFix(center, 10.0, now)), places, StraightLineRoutes(), now)
        assertEquals(listOf("화장실1", "화장실2", "화장실3"), r.results.map { it.first.name })
    }

    @Test fun `SOS12 stale fix is re-measured once`() = runBlocking {
        val loc = Fixed(LocationFix(center, 10.0, now.minusSeconds(40)), LocationFix(center, 10.0, now))
        val r = findSos(TOILET, loc, CountingPlaces(listOf(placeAt("1", 100.0))), StraightLineRoutes(), now)
        assertEquals(2, loc.calls); assertTrue(!r.locationWarning)
        val stale = Fixed(LocationFix(center, 10.0, now.minusSeconds(40)))
        assertTrue(findSos(TOILET, stale, CountingPlaces(emptyList()), StraightLineRoutes(), now).locationWarning)
        assertEquals(2, stale.calls)
    }

    @Test fun `dedup by normalized name within 50m`() {
        val a = placeAt("1", 100.0); val b = a.copy(id = "2", name = " 화장실 1 ", point = center.offsetMeters(north = 130.0))
        assertEquals(1, dedup(listOf(a, b)).size)
    }

    @Test fun `opening hours judged at arrival`() {
        val zone = java.time.ZoneId.of("Asia/Seoul")
        val h = OpeningHours(windows = java.time.DayOfWeek.entries.associateWith { listOf(java.time.LocalTime.of(9, 0) to java.time.LocalTime.of(12, 10)) })
        // 2026-11-04T03:00Z = 12:00 KST
        assertEquals(OpenStatus.OPEN, openStatusAt(h, now.plusSeconds(5 * 60), zone))
        assertEquals(OpenStatus.CLOSED, openStatusAt(h, now.plusSeconds(15 * 60), zone))
        assertEquals(OpenStatus.UNKNOWN, openStatusAt(null, now, zone))
    }
}
