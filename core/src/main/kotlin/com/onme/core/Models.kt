@file:UseSerializers(LocalDateSerializer::class, InstantSerializer::class)

package com.onme.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate

enum class MoodId { M01, M02, M03, M04, M05, M06 }
enum class GoalId { G01, G02, G03 }
enum class CareId(val order: Int) { COMFORT(1), QUIET(2), RESET(3), REPLAY(4), PLACE(5), REST(6), MUSIC(7) }
enum class DrivingState { PARKED, DRIVING, UNKNOWN }
enum class Phase { P1, P2, P3, P4, P5, OVERDUE, PAUSED }
enum class Rating { POSITIVE, NEUTRAL, NEGATIVE }

enum class Symptom {
    ABDOMINAL, LOWER_BACK, HOT_STUFFY, IRRITABLE, HEADACHE, @Deprecated("removed from the app; kept so old saved logs still load") SENSORY_DISCOMFORT,
    FATIGUE, SLEEPY, LOW_MOOD, HEAVY_FLOW, TOILET_WORRY
}

@Suppress("DEPRECATION")
val SYMPTOM_OPTIONS = Symptom.entries - Symptom.SENSORY_DISCOMFORT - Symptom.HEAVY_FLOW


enum class CycleBucket { D21_24, D25_28, D29_32, D33_35, D36_PLUS, UNKNOWN }
enum class Regularity { SAME, FEW_DAYS, VARIES, UNKNOWN }
enum class DurationBucket { D3_4, D5_6, D7_PLUS, UNKNOWN }
enum class SignalTiming { BEFORE, DURING, BOTH }
enum class ThermalPref { COLD_SENSITIVE, HEAT_SENSITIVE, NORMAL }
enum class DrivingPattern { COMMUTE, LONG_DISTANCE, OCCASIONAL }
enum class Situation { HORMONAL_PILL, PREGNANT, POSTPARTUM, PERIMENOPAUSE, NONE, NO_ANSWER }

@Serializable data class SymptomLog(val date: LocalDate, val symptom: Symptom, val intensity: Int /*0..3*/)
@Serializable data class FlowLog(val date: LocalDate, val level: Int /*1 적음, 2 보통, 3 많음*/)
@Serializable data class PeriodRecord(val start: LocalDate, val end: LocalDate? = null)
@Serializable data class EarlySignal(val symptom: Symptom, val timing: SignalTiming)

@Serializable
data class OnboardingAnswers(
    val importedStarts: List<LocalDate>? = null,
    val lastStart: LocalDate,
    val lastStartUncertain: Boolean = false,
    val previousStarts: List<LocalDate> = emptyList(),
    val usualCycleBucket: CycleBucket? = null,
    val regularity: Regularity? = null,
    val durationBucket: DurationBucket? = null,
    val earlySignals: Set<EarlySignal> = emptySet(),
    val thermal: ThermalPref? = null,
    val preferredSeatHeatLevel: Int? = null,
    val drivingPattern: DrivingPattern? = null,
    val situation: Situation? = null,
)

data class CycleForecast(
    val expectedStart: LocalDate, val rangeStart: LocalDate, val rangeEnd: LocalDate,
    val muDays: Int, val wDays: Int, val periodDays: Int,
    val ovulationEstimate: LocalDate?, val irregular: Boolean, val nCycles: Int,
    val accuracyStage: Int,
    val paused: Boolean = false,
)

@Serializable
data class VehicleCapabilities(
    val seatHeat: Boolean = true, val seatVent: Boolean = true, val hvacTemp: Boolean = true, val hvacFan: Boolean = true,
    val ambientLight: Boolean = true, val seatPosition: Boolean = true, val navigation: Boolean = true, val overlayCard: Boolean = false,
)

/** null = leave unchanged. */
@Serializable
data class VehicleSettings(
    val seatHeatLevel: Int? = null, val seatVentLevel: Int? = null, val cabinTempC: Double? = null,
    val fanLevel: Int? = null, val ambientDim: Boolean? = null, val seatProfileId: String? = null,
)

@Serializable
data class CabinState(
    val cabinTempC: Double = 22.0, val seatHeatLevel: Int = 0, val seatVentLevel: Int = 0,
    val fanLevel: Int = 3, val ambientDim: Boolean = false, val seatProfileId: String? = null,
)

@Serializable
data class Consents(
    val healthPersonalization: Boolean = false,
    val learnFromFeedback: Boolean = false,
    val location: Boolean = false,
    val vehicleControl: Boolean = false,
    val musicService: Boolean = false,
    val proactiveCards: Boolean = false,
)

@Serializable
data class CareSession(
    val id: String, val at: Instant, val mood: MoodId, val goal: GoalId,
    val ranked: List<Pair<CareId, Double>>, val shownMain: CareId?, val chosen: CareId?,
    val applied: VehicleSettings?, val phase: Phase?, val phaseOffset: Int?,
    val rating: Rating? = null,
)

@Serializable
data class ComfortPreset(val id: String, val name: String, val settings: VehicleSettings, val createdFrom: String /*ONBOARDING|USER|REPLAY*/)

/** Everything ONME stores about one driver profile. Persisted encrypted by the app. */
@Serializable
data class UserData(
    val onboarding: OnboardingAnswers? = null,
    val periods: List<PeriodRecord> = emptyList(),
    val symptoms: List<SymptomLog> = emptyList(),
    val flows: List<FlowLog> = emptyList(),
    val sessions: List<CareSession> = emptyList(),
    val presets: List<ComfortPreset> = emptyList(),
    val consents: Consents = Consents(),
    val familyPlanning: Boolean = false,
    val ovulationTest: LocalDate? = null,
    val cardSnoozedUntil: Map<String, Instant> = emptyMap(),
    val specialistNoticeShown: Boolean = false,
    val firstPredictionAsked: Boolean = false,
    val predictionMisses: Int = 0,
    /** false = profile doesn't track a cycle (e.g. husband): no onboarding, calendar or records. */
    val cycleTracking: Boolean = true,
    val predictionFeedback: List<String> = emptyList(), // quality metric (6.5)
    val pendingRange: Pair<LocalDate, LocalDate>? = null, // range shown before the first recorded start (6.5)
)

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
