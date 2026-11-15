package com.onme.core

import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafetyTest {
    @Test fun `UI1 parked-only screens blocked while driving`() {
        val blocked = Screen.entries.filter { !canOpen(it, DrivingState.DRIVING) }.toSet()
        assertEquals(setOf(Screen.ONBOARDING, Screen.RESET, Screen.FEEDBACK, Screen.CALENDAR, Screen.RECORD, Screen.SETTINGS), blocked)
        Screen.entries.forEach { assertTrue(canOpen(it, DrivingState.PARKED)) }
    }

    @Test fun `UI2 driving closes parked-only screen`() {
        assertEquals(Screen.HOME, screenAfter(Screen.RECORD, DrivingState.DRIVING))
        assertEquals(Screen.SOS, screenAfter(Screen.SOS, DrivingState.DRIVING))
        assertEquals(Screen.RECORD, screenAfter(Screen.RECORD, DrivingState.PARKED))
    }

    @Test fun `UI3 drive-visible copy has no cycle words, symptom names or scores`() {
        val banned = listOf("생리", "배란", "가임", "주기", "점수", "%") + SymptomCopy.LABEL.values
        val score = Regex("""\d+(\.\d+)?(?!\s*(분|km|m|곳|단계))""")
        // Spec-mandated wording: fatigue notice (7.6) names tiredness; store note (8.3) names the product.
        val mandated = mapOf(Copy.FATIGUE_NOTICE to "피로", SosCopy.STORE_NOTE to "생리")
        DriveCopy.all().forEach { s ->
            banned.filter { mandated[s] != it }.forEach { b -> assertTrue(b !in s, "\"$s\" contains \"$b\"") }
            assertTrue(!score.containsMatchIn(s), "\"$s\" shows a number")
        }
    }

    @Test fun `UI4 unknown driving state is treated as driving`() {
        val ctx = CareContext(DrivingState.UNKNOWN, VehicleCapabilities(), true, true, LocalDate.of(2026, 11, 4))
        assertTrue(!isFeasible(CareId.RESET, MoodId.M05, UserData(), ctx))
        assertTrue(!canOpen(Screen.RECORD, DrivingState.UNKNOWN))
    }

    @Test fun `UI5 snoozed card stays hidden for 24h`() {
        val now = Instant.parse("2026-11-04T00:00:00Z")
        val u = UserData(consents = Consents(proactiveCards = true))
        assertTrue(Cards.canShow(u, Cards.TR2, now, shownThisDrive = 0))
        val snoozed = Cards.snooze(u, Cards.TR2, now)
        assertTrue(!Cards.canShow(snoozed, Cards.TR2, now.plusSeconds(23 * 3600), 0))
        assertTrue(Cards.canShow(snoozed, Cards.TR2, now.plusSeconds(24 * 3600), 0))
        assertTrue(!Cards.canShow(u, Cards.TR2, now, shownThisDrive = 2))
        assertTrue(!Cards.canShow(UserData(), Cards.TR2, now, 0))
    }

    @Test fun `TR2 long drive threshold follows Q7`() {
        val u = UserData(consents = Consents(proactiveCards = true))
        assertTrue(!Cards.longDrive(u, 89)); assertTrue(Cards.longDrive(u, 90))
        val frequent = u.copy(onboarding = OnboardingAnswers(lastStart = LocalDate.of(2026, 11, 1), drivingPattern = DrivingPattern.LONG_DISTANCE))
        assertTrue(Cards.longDrive(frequent, 60))
    }

    @Test fun `user data round-trips through JSON`() {
        val a = DemoData.userA()
        assertEquals(a, UserStore.decode(UserStore.encode(a)))
        DemoData.profiles.map { it.seed(LocalDate.of(2026, 11, 5)) }.forEach { assertEquals(it, UserStore.decode(UserStore.encode(it))) }
    }
}
