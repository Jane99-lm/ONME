package com.onme.core

import com.onme.core.OnmeConfig as K
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import kotlin.math.ceil
import kotlin.math.sqrt

// Cycle prediction, phases and the C score (spec ch.6). Estimates only — never for contraception.

data class CycleLengths(val valid: List<Int>, val excluded: List<Int>)

private fun daysBetween(a: LocalDate, b: LocalDate) = DAYS.between(a, b).toInt()
private fun roundHalfUp(x: Double) = BigDecimal(x).setScale(0, RoundingMode.HALF_UP).toInt()

/** Q0 imports + Q1 + Q2 dates + recorded starts, deduplicated and sorted. */
fun allStarts(u: UserData): List<LocalDate> = buildSet {
    u.onboarding?.let { o -> o.importedStarts?.let(::addAll); add(o.lastStart); addAll(o.previousStarts) }
    u.periods.forEach { add(it.start) }
}.sorted()

/** 6.1 — consecutive gaps of the latest ≤7 starts; implausible gaps returned in [CycleLengths.excluded]. */
fun cycleLengths(starts: List<LocalDate>): CycleLengths {
    val gaps = starts.sorted().takeLast(K.CYCLE_LOOKBACK + 1).zipWithNext(::daysBetween)
    val (valid, excluded) = gaps.partition { it in K.CYCLE_MIN_VALID..K.CYCLE_MAX_VALID }
    return CycleLengths(valid, excluded)
}

private fun median(xs: List<Int>): Int {
    val s = xs.sorted(); val m = s.size / 2
    return if (s.size % 2 == 1) s[m] else roundHalfUp((s[m - 1] + s[m]) / 2.0)
}

private fun sampleStdev(xs: List<Int>): Double {
    val mean = xs.average()
    return sqrt(xs.sumOf { (it - mean) * (it - mean) } / (xs.size - 1))
}

private fun periodLength(p: PeriodRecord) = p.end?.let { daysBetween(p.start, it) + 1 }

/** 6.2. null when no start date is known at all. */
fun predictCycle(u: UserData): CycleForecast? {
    val starts = allStarts(u).ifEmpty { return null }
    val o = u.onboarding
    val n = cycleLengths(starts).valid
    val mu0 = o?.usualCycleBucket?.let { K.MU0[it] } ?: K.DEFAULT_MU
    val k = if (o?.usualCycleBucket != null && o.usualCycleBucket != CycleBucket.UNKNOWN) K.K_WITH_BUCKET else K.K_WITHOUT_BUCKET
    val w0 = o?.regularity?.let { K.W0[it] } ?: K.DEFAULT_W

    var mu: Int; var w: Int
    when {
        n.isEmpty() -> { mu = mu0; w = w0 }
        n.size < 3 -> { mu = roundHalfUp((n.sum() + k * mu0) / (n.size + k).toDouble()); w = w0 }
        else -> { mu = median(n); w = maxOf(K.W_MIN, ceil(sampleStdev(n)).toInt()) }
    }
    if (n.size < 3 && o?.lastStartUncertain == true) w += 1
    val irregular = (n.size >= 3 && n.max() - n.min() >= K.IRREGULAR_SPREAD_DAYS) ||
        o?.regularity == Regularity.VARIES || o?.usualCycleBucket == CycleBucket.D36_PLUS ||
        o?.situation == Situation.PERIMENOPAUSE
    if (irregular) w = maxOf(w, K.IRREGULAR_W_MIN)

    val e = starts.last().plusDays(mu.toLong())
    val periodDays = u.periods.mapNotNull(::periodLength).takeIf { it.isNotEmpty() }?.let(::median)
        ?: o?.durationBucket?.let { K.PERIOD_DAYS[it] } ?: K.DEFAULT_PERIOD_DAYS
    val ovulation = when {
        o?.situation == Situation.HORMONAL_PILL -> null // TODO(spec): withdrawal-bleed forecast is out of MVP
        u.ovulationTest != null && u.ovulationTest > starts.last() -> u.ovulationTest
        else -> e.minusDays(K.LUTEAL_DAYS.toLong())
    }
    return CycleForecast(
        expectedStart = e, rangeStart = e.minusDays(w.toLong()), rangeEnd = e.plusDays(w.toLong()),
        muDays = mu, wDays = w, periodDays = periodDays, ovulationEstimate = ovulation,
        irregular = irregular, nCycles = n.size, accuracyStage = if (n.isEmpty()) 1 else if (n.size < 3) 2 else 3,
        paused = o?.situation == Situation.PREGNANT || o?.situation == Situation.POSTPARTUM,
    )
}

fun readyKitDate(f: CycleForecast): LocalDate = f.rangeStart.minusDays(K.READY_KIT_DAYS_BEFORE_RANGE.toLong())

/** 6.3 — phase + offset (P1: D1.., P5/OVERDUE: relative to E). */
fun phaseOf(day: LocalDate, f: CycleForecast, lastStart: LocalDate, lastEnd: LocalDate?): Pair<Phase, Int?> {
    if (f.paused) return Phase.PAUSED to null
    val e = f.expectedStart
    val o = f.ovulationEstimate ?: e.minusDays(K.LUTEAL_DAYS.toLong()) // pill users: boundaries only, P3 never displayed
    val inPeriod = if (lastEnd != null) day <= lastEnd else day < lastStart.plusDays(f.periodDays.toLong())
    return when {
        inPeriod -> Phase.P1 to daysBetween(lastStart, day) + 1
        day >= e -> Phase.OVERDUE to daysBetween(e, day)
        day >= e.minusDays(K.P5_DAYS.toLong()) -> Phase.P5 to daysBetween(e, day)
        f.irregular -> Phase.P2 to null
        day <= o.minusDays(3) -> Phase.P2 to null
        day <= o.plusDays(1) -> Phase.P3 to null
        else -> Phase.P4 to null
    }
}

/**
 * Phase of any day: completed cycles use their actual next start as E (6.3: past P5 is re-based
 * on the confirmed start); the current cycle uses the forecast. null before the first known start.
 */
fun phaseOn(u: UserData, day: LocalDate): Pair<Phase, Int?>? {
    val f = predictCycle(u) ?: return null
    val starts = allStarts(u)
    val i = starts.indexOfLast { it <= day }.takeIf { it >= 0 } ?: return null
    val start = starts[i]
    val end = u.periods.firstOrNull { it.start == start }?.end
    val next = starts.getOrNull(i + 1)
    val cf = if (next == null) f else f.copy(expectedStart = next, ovulationEstimate = next.minusDays(K.LUTEAL_DAYS.toLong()))
    return phaseOf(day, cf, start, end)
}

/** 5.5 heat preference: Q6 cold, a positive heated COMFORT/REPLAY session, or a heated preset. */
fun heatPreferred(u: UserData): Boolean =
    u.onboarding?.thermal == ThermalPref.COLD_SENSITIVE ||
        u.sessions.any { it.rating == Rating.POSITIVE && it.chosen in setOf(CareId.COMFORT, CareId.REPLAY) && (it.applied?.seatHeatLevel ?: 0) > 0 } ||
        u.presets.any { (it.settings.seatHeatLevel ?: 0) > 0 }

/** 5.5 table incl. the heat-preference condition (D6). */
fun related(symptom: Symptom, care: CareId, u: UserData): Boolean =
    care in K.SYMPTOM_CARES.getValue(symptom) && ((symptom to care) !in K.HEAT_CONDITIONAL || heatPreferred(u))

fun todaySymptoms(u: UserData, today: LocalDate) = u.symptoms.filter { it.date == today && it.intensity > 0 }

/** 6.4 — the C score. Only rises above 50 on what the user actually logged or answered (0.1-7). */
fun cycleSymptomScore(u: UserData, care: CareId, today: LocalDate): Int {
    if (!u.consents.healthPersonalization) return K.C_DEFAULT
    if (todaySymptoms(u, today).any { related(it.symptom, care, u) }) return K.C_TODAY_REPORTED
    val f = predictCycle(u) ?: return K.C_DEFAULT
    val (phase, off) = phaseOn(u, today) ?: return K.C_DEFAULT
    if (phase !in K.C_ELEVATED_PHASES || off == null) return K.C_DEFAULT
    if (phase == Phase.P5 && f.irregular) return K.C_DEFAULT

    if (f.nCycles >= K.PATTERN_LOOKBACK_CYCLES) {
        val starts = allStarts(u)
        val current = starts.indexOfLast { it <= today }
        val completed = (maxOf(0, current - K.PATTERN_LOOKBACK_CYCLES) until current).map { starts[it] to starts[it + 1] }
        val hits = completed.count { (s, next) ->
            u.symptoms.any { log ->
                log.intensity > 0 && log.date >= s && log.date < next && related(log.symptom, care, u) &&
                    when (phase) {
                        Phase.P1 -> kotlin.math.abs(daysBetween(s, log.date) + 1 - off) <= K.PATTERN_TOLERANCE_DAYS
                        else -> daysBetween(next, log.date).let { o -> o >= -K.P5_DAYS && kotlin.math.abs(o - off) <= K.PATTERN_TOLERANCE_DAYS }
                    }
            }
        }
        return if (hits >= K.PATTERN_MIN_HITS) K.C_PATTERN else K.C_DEFAULT
    }
    val timings = if (phase == Phase.P1) setOf(SignalTiming.DURING, SignalTiming.BOTH) else setOf(SignalTiming.BEFORE, SignalTiming.BOTH)
    val early = u.onboarding?.earlySignals.orEmpty().any { it.timing in timings && related(it.symptom, care, u) }
    return if (early) K.C_EARLY_SIGNAL else K.C_DEFAULT
}

/** D13 — intensity-3 logs in 3 consecutive cycles → suggest a specialist once (parked screen only). */
fun specialistAdvised(u: UserData): Boolean {
    val starts = allStarts(u)
    val severe = starts.indices.map { i ->
        val end = starts.getOrNull(i + 1) ?: LocalDate.MAX
        u.symptoms.any { it.intensity >= K.SPECIALIST_INTENSITY && it.date >= starts[i] && it.date < end }
    }
    return severe.windowed(K.SPECIALIST_CYCLES).any { w -> w.all { it } }
}
