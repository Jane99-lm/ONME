package com.onme.core

import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

object UserStore {
    private val json = Json { ignoreUnknownKeys = true; allowStructuredMapKeys = true }
    fun encode(u: UserData): String = json.encodeToString(UserData.serializer(), u)
    fun decode(s: String): UserData = json.decodeFromString(UserData.serializer(), s)
}

/** A driver profile. 1st-round build: profiles arrive pre-made (phone app sets them up later). */
data class Profile(val id: String, val name: String, val role: String, val seed: (LocalDate) -> UserData)

/** Demo-mode fixtures (spec 9.4 user A = 12.3 test input) and mock places around the demo location. */
object DemoData {
    val SEOUL_CITY_HALL = GeoPoint(37.5663, 126.9779)
    private val allConsents = Consents(true, true, true, true, true, true)

    /** Cycles 30,28,31,29,32,30; period 5 days; last start 2026-11-02. */
    fun userA(): UserData {
        val last = LocalDate.of(2026, 11, 2)
        val starts = listOf(30, 32, 29, 31, 28, 30).runningFold(last) { d, l -> d.minusDays(l.toLong()) }.reversed()
        val prev3 = starts.takeLast(4) // the last 3 completed cycles: starts[0..2] with next starts[1..3]
        val logs = buildList {
            prev3.dropLast(1).forEach { s ->
                add(SymptomLog(s, Symptom.ABDOMINAL, 2)); add(SymptomLog(s.plusDays(1), Symptom.ABDOMINAL, 2))
            }
            prev3.take(2).forEach { add(SymptomLog(it, Symptom.HEAVY_FLOW, 2)) }
            prev3.drop(1).take(2).forEach { next ->
                (1..3).forEach { k ->
                    add(SymptomLog(next.minusDays(k.toLong()), Symptom.IRRITABLE, 2))
                    add(SymptomLog(next.minusDays(k.toLong()), Symptom.HEADACHE, 1))
                }
            }
        }
        val sessions = (0 until 3).map { i ->
            CareSession(
                id = "demo-comfort-$i", at = Instant.parse("2026-10-0${3 + i}T08:00:00Z"),
                mood = MoodId.M03, goal = GoalId.G03, ranked = emptyList(),
                shownMain = CareId.COMFORT, chosen = CareId.COMFORT,
                applied = VehicleSettings(seatHeatLevel = 2), phase = Phase.P1, phaseOffset = i + 1, rating = Rating.POSITIVE,
            )
        }
        return UserData(
            onboarding = OnboardingAnswers(lastStart = last, usualCycleBucket = CycleBucket.D29_32, regularity = Regularity.SAME, durationBucket = DurationBucket.D5_6),
            periods = starts.map { PeriodRecord(it, it.plusDays(4)) },
            symptoms = logs, sessions = sessions, consents = allConsents,
        )
    }

    /** ponytail: fixed 3 profiles until the phone app (post round 1) creates and syncs them. */
    val profiles = listOf(
        Profile("p1", "프로필 01", "나") { userA() },
        Profile("p2", "프로필 02", "남편") {
            UserData(cycleTracking = false, consents = allConsents.copy(healthPersonalization = false),
                presets = listOf(ComfortPreset("p2-base", "기본 편안함", VehicleSettings(seatVentLevel = 1, cabinTempC = 21.0), "USER")))
        },
        Profile("p3", "프로필 03", "가족") { today -> userB(today) },
    )

    /** 12.3 CY8 user B: no records yet, answered Q5 (abdominal during period) and Q6 (cold, heat 2); today is D1. */
    fun userB(today: LocalDate) = UserData(
        onboarding = OnboardingAnswers(lastStart = today, earlySignals = setOf(EarlySignal(Symptom.ABDOMINAL, SignalTiming.DURING)),
            thermal = ThermalPref.COLD_SENSITIVE, preferredSeatHeatLevel = 2),
        periods = listOf(PeriodRecord(today)), consents = allConsents,
        presets = listOf(ComfortPreset("onboarding", "기본 편안함", VehicleSettings(seatHeatLevel = 2), "ONBOARDING")),
    )

    private val daily = DayOfWeek.entries.associateWith { listOf(LocalTime.of(6, 0) to LocalTime.of(23, 0)) }

    /** Mock SOS places around [center]: mixes of open/unknown hours, access and parking states. */
    fun places(center: GeoPoint, now: Instant = Instant.now()): List<RawPlace> {
        fun at(n: Double, e: Double) = center.offsetMeters(n, e)
        val recent = SourceInfo(fieldVerifiedAt = now.minusSeconds(5 * 86400))
        val multi = SourceInfo(multiSourceAt = now.minusSeconds(40 * 86400))
        val official = SourceInfo(official = true)
        return listOf(
            RawPlace("t1", "역 공중화장실", SosCategory.TOILET, at(250.0, 120.0), OpeningHours(alwaysOpen = true), access = AccessStatus.PUBLIC, parking = ParkingStatus.NEARBY, walkM = 120.0, source = official),
            RawPlace("t2", "공원 공중화장실", SosCategory.TOILET, at(-180.0, -350.0), OpeningHours(windows = daily), access = AccessStatus.PUBLIC, parking = ParkingStatus.DEDICATED, parkMin = 1.0, walkM = 60.0, source = multi),
            RawPlace("t3", "상가 화장실", SosCategory.TOILET, at(700.0, 900.0), null, access = AccessStatus.CONDITIONAL, parking = ParkingStatus.UNKNOWN, source = recent),
            RawPlace("t4", "지하 광장 화장실", SosCategory.TOILET, at(100.0, -80.0), OpeningHours(alwaysOpen = true), access = AccessStatus.PUBLIC, parking = ParkingStatus.UNSAFE, source = official),
            RawPlace("t5", "근린공원 화장실", SosCategory.TOILET, at(-900.0, -1400.0), OpeningHours(windows = daily), access = AccessStatus.PUBLIC, parking = ParkingStatus.DEDICATED, walkM = 80.0, source = official),
            RawPlace("s1", "편의점 (북쪽)", SosCategory.STORE, at(150.0, 200.0), OpeningHours(alwaysOpen = true), parking = ParkingStatus.UNKNOWN, source = multi),
            RawPlace("s2", "편의점 (북동쪽)", SosCategory.STORE, at(450.0, 300.0), OpeningHours(alwaysOpen = true), parking = ParkingStatus.DEDICATED, parkMin = 1.0, walkM = 30.0, source = recent),
            RawPlace("s3", "편의점 (남서쪽)", SosCategory.STORE, at(-300.0, -600.0), OpeningHours(windows = daily), parking = ParkingStatus.NEARBY, walkM = 150.0, source = official),
            RawPlace("s4", "편의점 (동쪽)", SosCategory.STORE, at(-120.0, 380.0), OpeningHours(alwaysOpen = true), parking = ParkingStatus.UNSAFE, source = official),
        )
    }
}
