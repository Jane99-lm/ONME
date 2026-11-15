package com.onme.core

import com.onme.core.CareId.*
import com.onme.core.OnmeConfig as K
import java.time.LocalDate

// Mood Care STEP 5–6 (spec ch.7). Safety/consent gates run before any scoring (0.1-1).

data class CareContext(
    val drivingState: DrivingState,
    val caps: VehicleCapabilities,
    val locationOk: Boolean,
    val musicSupported: Boolean,
    val today: LocalDate,
    /** Cares whose apply() failed this session (7.7). */
    val sessionExcluded: Set<CareId> = emptySet(),
) {
    /** UNKNOWN counts as DRIVING (2.4). */
    val parked get() = drivingState == DrivingState.PARKED
}

data class Scored(val care: CareId, val score: Double)

sealed interface Recommendation {
    val ranked: List<Scored>
    data class Main(val main: Scored, val alternatives: List<Scored>, override val ranked: List<Scored>) : Recommendation
    data class SafetyFirst(val main: Scored, val alternatives: List<Scored>, val notice: String, override val ranked: List<Scored>) : Recommendation
    data class GeneralChoices(val choices: List<CareId>, override val ranked: List<Scored>) : Recommendation
}

fun defaultGoal(mood: MoodId): GoalId = K.DEFAULT_GOAL.getValue(mood)
fun goalsFor(mood: MoodId): List<GoalId> = K.ALLOWED_GOALS[mood] ?: GoalId.entries

/** Settings restricted to what this car supports; the seat profile only while parked (7.2). null if nothing is left. */
fun supportedPart(s: VehicleSettings, caps: VehicleCapabilities, state: DrivingState): VehicleSettings? {
    val r = VehicleSettings(
        seatHeatLevel = s.seatHeatLevel.takeIf { caps.seatHeat },
        seatVentLevel = s.seatVentLevel.takeIf { caps.seatVent },
        cabinTempC = s.cabinTempC.takeIf { caps.hvacTemp },
        fanLevel = s.fanLevel.takeIf { caps.hvacFan },
        ambientDim = s.ambientDim.takeIf { caps.ambientLight },
        seatProfileId = s.seatProfileId.takeIf { caps.seatPosition && state == DrivingState.PARKED },
    )
    return r.takeIf { it != VehicleSettings() }
}

private fun positiveSnapshots(u: UserData) = u.sessions.filter { it.rating == Rating.POSITIVE && it.applied != null }

/** 7.2 — the D gate: feasible (D=100) or excluded entirely. */
fun isFeasible(care: CareId, mood: MoodId, u: UserData, ctx: CareContext): Boolean {
    if (care in ctx.sessionExcluded) return false
    val c = u.consents; val caps = ctx.caps
    return when (care) {
        COMFORT -> c.vehicleControl && (caps.seatHeat || caps.seatVent || caps.hvacTemp)
        QUIET -> c.vehicleControl && (caps.hvacFan || caps.ambientLight)
        RESET -> ctx.parked
        REPLAY -> c.vehicleControl && positiveSnapshots(u).any { supportedPart(it.applied!!, caps, ctx.drivingState) != null }
        PLACE -> c.location && ctx.locationOk && (ctx.parked || caps.navigation)
        REST -> true
        MUSIC -> c.musicService && ctx.musicSupported && mood != MoodId.M04
    }
}

/** 7.3 — the P score from the latest 5 ratings (NEUTRAL counted). */
fun preferenceScore(u: UserData, care: CareId): Int {
    val thermal = u.onboarding?.thermal in setOf(ThermalPref.COLD_SENSITIVE, ThermalPref.HEAT_SENSITIVE)
    val base = if (care == COMFORT && thermal) K.P_THERMAL_BASE else K.P_DEFAULT
    if (!u.consents.learnFromFeedback) return base
    val ratings = u.sessions.filter { it.chosen == care && it.rating != null }.sortedBy { it.at }.takeLast(K.P_WINDOW).map { it.rating }
    if (ratings.isEmpty()) return base
    val l = ratings.count { it == Rating.POSITIVE }; val n = ratings.count { it == Rating.NEGATIVE }
    return (K.P_DEFAULT + K.P_POS_STEP * l + K.P_NEG_STEP * n).coerceIn(0, 100)
}

/** 7.4 — integer S×10 avoids 0.3×93 = 27.900000000000002. */
fun score(mood: MoodId, goal: GoalId, care: CareId, p: Int, c: Int): Double {
    val s10 = K.W_M * K.M.getValue(mood).getValue(care) + K.W_G * K.G.getValue(goal).getValue(care) + K.W_P * p + K.W_C * c + K.W_D * K.D_FEASIBLE
    return s10 / 10.0
}

/** 7.5 */
fun fatigueRisk(mood: MoodId, today: List<SymptomLog>): Boolean =
    mood == MoodId.M04 || today.any { it.symptom == Symptom.SLEEPY || (it.symptom == Symptom.FATIGUE && it.intensity >= K.FATIGUE_MIN_INTENSITY) }

/** 7.6 — core ranking on already-gated inputs. */
fun recommend(mood: MoodId, goal: GoalId, feasible: Set<CareId>, p: (CareId) -> Int, c: (CareId) -> Int, fatigue: Boolean): Recommendation {
    val ranked = CareId.entries.filter { it in feasible }
        .map { Scored(it, score(mood, goal, it, p(it), c(it))) }
        .sortedWith(compareByDescending<Scored> { it.score }.thenBy { it.care.order })
    if (fatigue) {
        val rest = ranked.firstOrNull { it.care == REST } ?: Scored(REST, score(mood, goal, REST, p(REST), c(REST)))
        val alts = ranked.filter { it.care !in setOf(REST, MUSIC, PLACE) }.take(K.MAX_ALTERNATIVES) // D4
        return Recommendation.SafetyFirst(rest, alts, Copy.FATIGUE_NOTICE, ranked)
    }
    if (ranked.isEmpty() || ranked[0].score < K.MAIN_THRESHOLD) return Recommendation.GeneralChoices(ranked.map { it.care }, ranked)
    val main = ranked[0]
    val alts = ranked.drop(1).take(K.MAX_ALTERNATIVES).filter { main.score - it.score < K.NEAR_TIE_GAP } // D2
    return Recommendation.Main(main, alts, ranked)
}

/** 7.6 — full pipeline from stored user data. */
fun recommend(mood: MoodId, goal: GoalId, u: UserData, ctx: CareContext): Recommendation = recommend(
    mood, goal,
    feasible = CareId.entries.filter { isFeasible(it, mood, u, ctx) }.toSet(),
    p = { preferenceScore(u, it) },
    c = { cycleSymptomScore(u, it, ctx.today) },
    fatigue = fatigueRisk(mood, todaySymptoms(u, ctx.today)),
)

// ---- 7.7 care actions ----------------------------------------------------------------------

data class CareAction(
    val care: CareId,
    val headline: String,
    /** Vehicle change to apply after the user taps the button (already filtered to supported items). */
    val settings: VehicleSettings? = null,
    val minutes: Int? = null,
    val playlistTag: String? = null,
    val placeKinds: List<String> = emptyList(),
    val sosToilet: Boolean = false,
    val replayOf: String? = null,
    /** QUIET also silences ONME cards/voice for the rest of this drive. */
    val quietDrive: Boolean = false,
)

private fun savedComfort(u: UserData) = u.presets.lastOrNull { it.createdFrom != "REPLAY" }?.settings

fun buildCareAction(care: CareId, mood: MoodId, goal: GoalId, u: UserData, ctx: CareContext, cabin: CabinState): CareAction {
    fun fit(s: VehicleSettings) = supportedPart(s, ctx.caps, ctx.drivingState)
    val today = todaySymptoms(u, ctx.today)
    return when (care) {
        COMFORT -> {
            val saved = savedComfort(u)
            val cool = today.any { it.symptom == Symptom.HOT_STUFFY } || u.onboarding?.thermal == ThermalPref.HEAT_SENSITIVE
            val s = if (cool) VehicleSettings(seatVentLevel = saved?.seatVentLevel ?: K.COMFORT_DEFAULT_HEAT, cabinTempC = saved?.cabinTempC)
            else VehicleSettings(seatHeatLevel = saved?.seatHeatLevel ?: u.onboarding?.preferredSeatHeatLevel ?: K.COMFORT_DEFAULT_HEAT, cabinTempC = saved?.cabinTempC)
            CareAction(care, if (cool) Copy.COMFORT_COOL else Copy.COMFORT_WARM, settings = fit(s))
        }
        QUIET -> CareAction(care, Copy.MAIN.getValue(care), quietDrive = true,
            settings = fit(VehicleSettings(fanLevel = maxOf(1, cabin.fanLevel - 1), ambientDim = true)))
        RESET -> CareAction(care, Copy.MAIN.getValue(care), minutes = K.RESET_MINUTES)
        REPLAY -> {
            val phase = phaseOn(u, ctx.today)?.first
            val snaps = positiveSnapshots(u).sortedByDescending { it.at }
            val pick = snaps.firstOrNull { phase != null && it.phase == phase } ?: snaps.firstOrNull()
            CareAction(care, Copy.MAIN.getValue(care), settings = pick?.applied?.let(::fit), replayOf = pick?.id)
        }
        PLACE -> {
            val toilet = today.any { it.symptom == Symptom.HEAVY_FLOW || it.symptom == Symptom.TOILET_WORRY }
            CareAction(care, Copy.MAIN.getValue(care), placeKinds = K.PLACE_KINDS.getValue(goal), sosToilet = toilet)
        }
        REST -> CareAction(care, Copy.MAIN.getValue(care), minutes = K.REST_PARKED_MINUTES.takeIf { ctx.parked })
        MUSIC -> CareAction(care, Copy.MAIN.getValue(care), playlistTag = K.PLAYLIST_TAGS[mood]?.get(goal))
    }
}
