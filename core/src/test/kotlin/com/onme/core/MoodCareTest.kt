package com.onme.core

import com.onme.core.CareId.*
import com.onme.core.GoalId.*
import com.onme.core.MoodId.*
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MoodCareTest {
    private val all = CareId.entries.toSet()

    /** 12.1 base conditions: P=50, C=50, all feasible (M04: MUSIC never feasible, 7.2). */
    private fun base(mood: MoodId, goal: GoalId, feasible: Set<CareId> = all, fatigue: Boolean = mood == M04) =
        recommend(mood, goal, if (mood == M04) feasible - MUSIC else feasible, { 50 }, { 50 }, fatigue)

    private fun Recommendation.top3() = ranked.take(3).map { it.care to it.score }

    private data class Golden(val mood: MoodId, val goal: GoalId, val top: List<Pair<CareId, Double>>, val main: CareId?, val alts: List<CareId>, val safety: Boolean = false)

    private val golden = listOf(
        Golden(M01, G01, listOf(MUSIC to 77.5, COMFORT to 76.0, QUIET to 74.5), MUSIC, listOf(COMFORT, QUIET)),
        Golden(M01, G02, listOf(MUSIC to 80.5, PLACE to 79.0, COMFORT to 65.5), MUSIC, listOf(PLACE)),
        Golden(M01, G03, listOf(REPLAY to 80.5, MUSIC to 79.0, COMFORT to 74.5), REPLAY, listOf(MUSIC)),
        Golden(M02, G01, listOf(QUIET to 79.0, MUSIC to 76.0, RESET to 71.5), QUIET, listOf(MUSIC)),
        Golden(M02, G02, listOf(MUSIC to 79.0, PLACE to 71.5, REPLAY to 68.5), MUSIC, listOf()),
        Golden(M02, G03, listOf(REPLAY to 83.5, MUSIC to 77.5, QUIET to 71.5), REPLAY, listOf()),
        Golden(M03, G01, listOf(MUSIC to 76.0, QUIET to 74.5, COMFORT to 73.0), MUSIC, listOf(QUIET, COMFORT)),
        Golden(M03, G02, listOf(MUSIC to 79.0, PLACE to 77.5, REPLAY to 67.0), MUSIC, listOf(PLACE)),
        Golden(M03, G03, listOf(REPLAY to 82.0, MUSIC to 77.5, COMFORT to 71.5), REPLAY, listOf(MUSIC)),
        Golden(M04, G01, listOf(REST to 80.5, RESET to 74.5, COMFORT to 70.0), REST, listOf(RESET, COMFORT), safety = true),
        Golden(M04, G02, listOf(RESET to 70.0, REST to 67.0, PLACE to 65.5), REST, listOf(RESET, COMFORT), safety = true),
        Golden(M04, G03, listOf(REST to 70.0, COMFORT to 68.5, REPLAY to 65.5), REST, listOf(COMFORT, REPLAY), safety = true),
        Golden(M05, G01, listOf(RESET to 82.0, QUIET to 76.0, MUSIC to 76.0), RESET, listOf()),
        Golden(M05, G02, listOf(PLACE to 79.0, MUSIC to 79.0, RESET to 77.5), PLACE, listOf(MUSIC, RESET)),
        Golden(M05, G03, listOf(MUSIC to 77.5, COMFORT to 70.0, QUIET to 68.5), MUSIC, listOf()),
        Golden(M06, G01, listOf(QUIET to 83.5, RESET to 80.5, COMFORT to 78.4), QUIET, listOf(RESET)),
        Golden(M06, G02, listOf(MUSIC to 77.5, RESET to 76.0, PLACE to 71.5), MUSIC, listOf(RESET)),
        Golden(M06, G03, listOf(COMFORT to 76.9, QUIET to 76.0, MUSIC to 76.0), COMFORT, listOf(QUIET, MUSIC)),
    )

    @Test fun `12_1 eighteen base combinations`() {
        golden.forEachIndexed { i, g ->
            val r = base(g.mood, g.goal)
            assertEquals(g.top, r.top3(), "#${i + 1} ${g.mood}·${g.goal} ranking")
            when (r) {
                is Recommendation.SafetyFirst -> {
                    assertTrue(g.safety, "#${i + 1} unexpected SafetyFirst")
                    assertEquals(REST, r.main.care)
                    assertEquals(g.alts, r.alternatives.map { it.care }, "#${i + 1} alternatives")
                }
                is Recommendation.Main -> {
                    assertEquals(g.main, r.main.care, "#${i + 1} main")
                    assertEquals(g.alts, r.alternatives.map { it.care }, "#${i + 1} alternatives")
                }
                is Recommendation.GeneralChoices -> error("#${i + 1} unexpected GeneralChoices")
            }
        }
    }

    @Test fun `float trap - integer arithmetic`() = assertEquals(76.9, score(M06, G03, COMFORT, 50, 50))

    @Test fun `MC1 QUIET with P80 C90`() = assertEquals(93.5, score(M06, G01, QUIET, 80, 90))

    @Test fun `MC2 driving drops RESET, tie broken by care order`() {
        val r = assertIs<Recommendation.Main>(recommend(M05, G01, all - RESET, { 50 }, { 50 }, false))
        assertEquals(QUIET to 76.0, r.main.care to r.main.score)
        assertEquals(listOf(MUSIC to 76.0, COMFORT to 71.5), r.alternatives.map { it.care to it.score })
    }

    private val drivingCtx = CareContext(DrivingState.DRIVING, VehicleCapabilities(), locationOk = true, musicSupported = true, today = LocalDate.of(2026, 11, 10))
    private val consented = UserData(consents = Consents(true, true, true, true, true, true))

    @Test fun `MC3 tired while driving`() {
        val r = assertIs<Recommendation.SafetyFirst>(recommend(M04, G02, consented, drivingCtx))
        assertEquals(REST, r.main.care)
        assertTrue(r.notice.isNotBlank())
        assertTrue(r.alternatives.none { it.care in setOf(RESET, MUSIC, PLACE) })
    }

    @Test fun `MC4 sleepy log triggers fatigue rule`() {
        val today = LocalDate.of(2026, 11, 10)
        val u = consented.copy(symptoms = listOf(SymptomLog(today, Symptom.SLEEPY, 1)))
        val r = recommend(M03, G03, u, drivingCtx.copy(drivingState = DrivingState.PARKED, today = today))
        assertEquals(REST, assertIs<Recommendation.SafetyFirst>(r).main.care)
    }

    @Test fun `MC5 low scores give general choices`() {
        val ctx = drivingCtx.copy(caps = VehicleCapabilities(false, false, false, false, false, false, navigation = true), musicSupported = false)
        val r = assertIs<Recommendation.GeneralChoices>(recommend(M01, G01, consented, ctx))
        assertEquals(listOf(PLACE to 59.5, REST to 53.5), r.ranked.map { it.care to it.score })
    }

    @Test fun `MC6 no location consent drops PLACE`() {
        val r = assertIs<Recommendation.Main>(recommend(M02, G02, all - PLACE, { 50 }, { 50 }, false))
        assertEquals(MUSIC, r.main.care)
        assertEquals(68.5, r.ranked[1].score)
        assertTrue(r.alternatives.isEmpty())
        val u = consented.copy(consents = consented.consents.copy(location = false))
        assertTrue(!isFeasible(PLACE, M02, u, drivingCtx.copy(drivingState = DrivingState.PARKED)))
    }

    @Test fun `MC7 threshold 70 inclusive`() = assertEquals(70.0, base(M04, G03).ranked.first().score)

    @Test fun `MC8 goal change recalculates`() {
        assertEquals(REPLAY, assertIs<Recommendation.Main>(base(M03, G03)).main.care)
        assertEquals(MUSIC, assertIs<Recommendation.Main>(base(M03, G02)).main.care)
    }

    @Test fun `MC8b good mood only offers keeping the mood`() {
        assertEquals(listOf(G03), goalsFor(M01))
        assertEquals(listOf(G02, G03), goalsFor(M02))
        assertEquals(listOf(G01), goalsFor(M06))
        assertTrue(defaultGoal(M02) in goalsFor(M02))
        assertEquals(GoalId.entries, goalsFor(M03))
        assertEquals(G03, defaultGoal(M01))
        assertEquals(G02, defaultGoal(M03))
        assertEquals(GoalId.entries, goalsFor(M03))
    }

    @Test fun `MC9 preference score`() {
        fun p(vararg r: Rating) = preferenceScore(consented.copy(sessions = r.mapIndexed { i, x -> session(COMFORT, x, i.toLong()) }), COMFORT)
        assertEquals(30, p(Rating.POSITIVE, Rating.NEGATIVE, Rating.NEGATIVE))
        assertEquals(100, p(*Array(5) { Rating.POSITIVE }))
        assertEquals(0, p(*Array(4) { Rating.NEGATIVE }))
        // only the latest 5 count (NEUTRAL included)
        assertEquals(50, p(Rating.POSITIVE, *Array(5) { Rating.NEUTRAL }))
    }

    @Test fun `MC10 no positive applied snapshot drops REPLAY`() {
        val parked = drivingCtx.copy(drivingState = DrivingState.PARKED)
        assertTrue(!isFeasible(REPLAY, M01, consented, parked))
        val u = consented.copy(sessions = listOf(session(COMFORT, Rating.POSITIVE, 0)))
        assertTrue(isFeasible(REPLAY, M01, u, parked))
    }

    @Test fun `MC11 withdrawing health consent pins C at 50`() {
        val a = DemoData.userA()
        val off = a.copy(consents = a.consents.copy(healthPersonalization = false))
        val day = LocalDate.of(2026, 11, 3)
        CareId.entries.forEach { assertEquals(50, cycleSymptomScore(off, it, day)) }
    }

    @Test fun `MC12 nothing is applied before the user taps apply`() = kotlinx.coroutines.runBlocking {
        val car = MockVehicleGateway()
        val runner = CareRunner(car)
        val action = buildCareAction(COMFORT, M03, G03, consented, drivingCtx.copy(drivingState = DrivingState.PARKED), car.readCabin())
        assertEquals(0, car.applyCount)
        runner.approve(action.settings!!)
        assertEquals(1, car.applyCount)
    }

    @Test fun `seat profile only applied when parked`() {
        val s = VehicleSettings(seatHeatLevel = 2, seatProfileId = "p1")
        assertEquals(null, supportedPart(s, VehicleCapabilities(), DrivingState.DRIVING)!!.seatProfileId)
        assertEquals("p1", supportedPart(s, VehicleCapabilities(), DrivingState.PARKED)!!.seatProfileId)
    }

    companion object {
        fun session(care: CareId, rating: Rating?, n: Long, heat: Int = 2) = CareSession(
            id = "s$n", at = java.time.Instant.parse("2026-01-01T00:00:00Z").plusSeconds(n * 3600),
            mood = M03, goal = G03, ranked = emptyList(), shownMain = care, chosen = care,
            applied = VehicleSettings(seatHeatLevel = heat), phase = null, phaseOffset = null, rating = rating,
        )
    }
}
