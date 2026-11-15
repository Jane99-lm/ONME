package com.onme.core

import com.onme.core.CareId.*
import com.onme.core.GoalId.*
import com.onme.core.MoodId.*
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CycleTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)
    private val a = DemoData.userA()
    private val parked = CareContext(DrivingState.PARKED, VehicleCapabilities(), locationOk = true, musicSupported = true, today = d(11, 3))

    @Test fun `CY1 forecast for user A`() {
        val f = predictCycle(a)!!
        assertEquals(30, f.muDays); assertEquals(2, f.wDays)
        assertEquals(d(12, 2), f.expectedStart)
        assertEquals(d(11, 30) to d(12, 4), f.rangeStart to f.rangeEnd)
        assertEquals(d(11, 18), f.ovulationEstimate)
        assertEquals(false, f.irregular); assertEquals(3, f.accuracyStage)
    }

    @Test fun `CY2 phases for user A`() {
        fun span(from: LocalDate, to: LocalDate, p: Phase) =
            generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }.forEach { assertEquals(p, phaseOn(a, it)?.first, "$it") }
        span(d(11, 2), d(11, 6), Phase.P1)
        span(d(11, 7), d(11, 15), Phase.P2)
        span(d(11, 16), d(11, 19), Phase.P3)
        span(d(11, 20), d(11, 25), Phase.P4)
        span(d(11, 26), d(12, 1), Phase.P5)
        assertEquals(Phase.P1 to 2, phaseOn(a, d(11, 3)))
        assertEquals(Phase.P5 to -3, phaseOn(a, d(11, 29)))
    }

    @Test fun `CY3 ready kit date`() = assertEquals(d(11, 28), readyKitDate(predictCycle(a)!!))

    @Test fun `CY4 today abdominal log`() {
        val u = a.copy(symptoms = a.symptoms + SymptomLog(d(11, 3), Symptom.ABDOMINAL, 2))
        assertEquals(90, cycleSymptomScore(u, COMFORT, d(11, 3)))
        val r = assertIs<Recommendation.Main>(recommend(M03, G03, u, parked))
        assertEquals(listOf(REPLAY to 82.0, COMFORT to 81.5, MUSIC to 77.5), r.ranked.take(3).map { it.care to it.score })
        // Spec lists only COMFORT, but MUSIC is 4.5 behind (< NEAR_TIE_GAP) — 12.1 #9 shows MUSIC in the same spot.
        assertEquals(REPLAY, r.main.care); assertEquals(listOf(COMFORT, MUSIC), r.alternatives.map { it.care })
    }

    @Test fun `CY5 pattern only`() {
        assertEquals(65, cycleSymptomScore(a, COMFORT, d(11, 3)))
        val r = assertIs<Recommendation.Main>(recommend(M03, G03, a, parked))
        assertEquals(79.0, r.ranked.first { it.care == COMFORT }.score)
        assertEquals(REPLAY, r.main.care); assertEquals(listOf(COMFORT, MUSIC), r.alternatives.map { it.care }) // see CY4

    }

    /**
     * CY6. The spec lists COMFORT 78.4 here, but that is the P=50 value from 12.1 #16; user A's COMFORT
     * has P=80 (3 positives, same as CY4), so the formula gives 84.4. We assert the formula, which changes
     * the expected screens — flagged for the spec owner.
     */
    @Test fun `CY6 premenstrual sensitivity`() {
        val day = d(11, 29)
        assertEquals(65, cycleSymptomScore(a, QUIET, day))
        val r = assertIs<Recommendation.Main>(recommend(M06, G01, a, parked.copy(today = day)))
        assertEquals(85.0, r.ranked.first { it.care == QUIET }.score)
        assertEquals(80.5, r.ranked.first { it.care == RESET }.score)
        assertEquals(84.4, r.ranked.first { it.care == COMFORT }.score) // spec: 78.4 (see KDoc)
        assertEquals(QUIET, r.main.care)
        assertEquals(listOf(COMFORT, RESET), r.alternatives.map { it.care })
        val drv = assertIs<Recommendation.Main>(recommend(M06, G01, a, parked.copy(today = day, drivingState = DrivingState.DRIVING)))
        assertTrue(drv.ranked.none { it.care == RESET })
        assertEquals(QUIET, drv.main.care)
    }

    @Test fun `CY7 follicular phase has neutral C`() {
        CareId.entries.forEach { assertEquals(50, cycleSymptomScore(a, it, d(11, 10)), "$it") }
        val r = assertIs<Recommendation.Main>(recommend(M01, G02, a, parked.copy(today = d(11, 10))))
        assertEquals(MUSIC to 80.5, r.main.care to r.main.score)
    }

    @Test fun `CY8 user B early signals`() {
        val day = d(11, 2)
        val consents = Consents(true, true, true, true, true, true)
        val skipped = UserData(periods = listOf(PeriodRecord(day)), consents = consents)
        val answered = skipped.copy(onboarding = OnboardingAnswers(
            lastStart = day, earlySignals = setOf(EarlySignal(Symptom.ABDOMINAL, SignalTiming.DURING)), thermal = ThermalPref.COLD_SENSITIVE,
        ))
        val ctx = parked.copy(today = day)
        fun rec(u: UserData) = assertIs<Recommendation.Main>(recommend(M03, G03, u, ctx))

        rec(skipped).let { r ->
            assertTrue(r.ranked.none { it.care == REPLAY })
            assertEquals(71.5, r.ranked.first { it.care == COMFORT }.score)
            assertEquals(MUSIC to 77.5, r.main.care to r.main.score); assertTrue(r.alternatives.isEmpty())
        }
        rec(answered).let { r ->
            assertEquals(60, preferenceScore(answered, COMFORT)); assertEquals(60, cycleSymptomScore(answered, COMFORT, day))
            assertEquals(74.5, r.ranked.first { it.care == COMFORT }.score)
            assertEquals(MUSIC, r.main.care); assertEquals(listOf(COMFORT), r.alternatives.map { it.care })
        }
        rec(answered.copy(symptoms = listOf(SymptomLog(day, Symptom.ABDOMINAL, 2)))).let { r ->
            assertEquals(COMFORT to 77.5, r.main.care to r.main.score)
        }
    }

    @Test fun `CY9 one cycle blended with Q2`() {
        val u = UserData(periods = listOf(PeriodRecord(d(10, 1)), PeriodRecord(d(11, 1))),
            onboarding = OnboardingAnswers(lastStart = d(11, 1), usualCycleBucket = CycleBucket.D25_28))
        assertEquals(28, predictCycle(u)!!.muDays)
    }

    @Test fun `CY10 implausible lengths excluded`() {
        val starts = listOf(d(1, 1), d(1, 15), d(3, 17), d(4, 16))
        val l = cycleLengths(starts)
        assertEquals(listOf(14, 61), l.excluded)
        assertEquals(listOf(30), l.valid)
    }

    @Test fun `CY11 irregular cycles`() {
        val starts = listOf(d(8, 1), d(8, 25), d(9, 30), d(10, 28))
        val logs = starts.drop(1).map { SymptomLog(it.minusDays(2), Symptom.IRRITABLE, 2) }
        val u = UserData(periods = starts.map { PeriodRecord(it) }, symptoms = logs, consents = Consents(healthPersonalization = true))
        val f = predictCycle(u)!!
        assertTrue(f.irregular); assertTrue(f.wDays >= 5)
        val all = generateSequence(d(10, 28)) { it.plusDays(1) }.take(40).mapNotNull { phaseOn(u, it)?.first }.toSet()
        assertTrue(Phase.P3 !in all && Phase.P4 !in all)
        assertEquals(Phase.P5, phaseOn(u, f.expectedStart.minusDays(2))?.first)
        assertEquals(50, cycleSymptomScore(u, QUIET, f.expectedStart.minusDays(2)))
    }

    @Test fun `CY12 early signal ignored once three cycles exist`() {
        val u = a.copy(symptoms = emptyList(), onboarding = a.onboarding!!.copy(earlySignals = setOf(EarlySignal(Symptom.IRRITABLE, SignalTiming.BEFORE))))
        assertEquals(50, cycleSymptomScore(u, QUIET, d(11, 29)))
    }

    @Test fun `CY13 situation handling`() {
        fun with(s: Situation) = a.copy(onboarding = a.onboarding!!.copy(situation = s))
        assertNull(predictCycle(with(Situation.HORMONAL_PILL))!!.ovulationEstimate)
        val preg = with(Situation.PREGNANT)
        assertTrue(predictCycle(preg)!!.paused)
        assertEquals(Phase.PAUSED, phaseOn(preg, d(11, 3))?.first)
        assertEquals(65, cycleSymptomScore(a, COMFORT, d(11, 3)))
        assertEquals(50, cycleSymptomScore(preg, COMFORT, d(11, 3)))
    }

    @Test fun `CY14 overdue`() {
        assertEquals(Phase.OVERDUE to 1, phaseOn(a, d(12, 3)))
        CareId.entries.forEach { assertEquals(50, cycleSymptomScore(a, it, d(12, 3))) }
    }

    @Test fun `D13 specialist notice after three severe cycles`() {
        assertTrue(!specialistAdvised(a))
        val severe = a.copy(symptoms = a.symptoms + listOf(d(8, 3), d(9, 1), d(10, 3)).map { SymptomLog(it, Symptom.ABDOMINAL, 3) })
        assertTrue(specialistAdvised(severe))
    }
}
