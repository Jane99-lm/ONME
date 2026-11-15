package com.onme.core

import com.onme.core.CareId.*

/**
 * Every weight, threshold and lookup table (spec 0.1-10, ch.5, ch.13 defaults).
 * Product-design starting values, NOT clinically validated — expected to change.
 */
object OnmeConfig {
    // 5.1 Mood Care weights, as integers of S×10 = 3M + 3G + 2P + C + D
    const val W_M = 3; const val W_G = 3; const val W_P = 2; const val W_C = 1; const val W_D = 1
    const val MAIN_THRESHOLD = 70.0
    const val NEAR_TIE_GAP = 5.0
    const val MAX_ALTERNATIVES = 2
    const val P_DEFAULT = 50; const val C_DEFAULT = 50; const val D_FEASIBLE = 100
    const val P_WINDOW = 5; const val P_POS_STEP = 10; const val P_NEG_STEP = -15
    const val P_THERMAL_BASE = 60 // D7: COMFORT base when Q6 cold/heat answered and no ratings yet

    private fun row(vararg v: Int) = CareId.entries.zip(v.toList()).toMap()

    // 5.2 M — mood × care
    val M: Map<MoodId, Map<CareId, Int>> = mapOf(
        MoodId.M01 to row(85, 65, 55, 85, 80, 10, 90),
        MoodId.M02 to row(65, 80, 60, 95, 55, 20, 85),
        MoodId.M03 to row(75, 65, 50, 90, 75, 30, 85),
        MoodId.M04 to row(65, 45, 70, 35, 35, 100, 20),
        MoodId.M05 to row(70, 70, 95, 35, 80, 55, 85),
        MoodId.M06 to row(93, 95, 90, 40, 55, 50, 80), // D16: 93 kept as written
    )

    // 5.3 G — goal × care
    val G: Map<GoalId, Map<CareId, Int>> = mapOf(
        GoalId.G01 to row(85, 100, 95, 55, 35, 85, 85),
        GoalId.G02 to row(50, 25, 80, 50, 100, 40, 95),
        GoalId.G03 to row(80, 75, 45, 100, 55, 50, 90),
    )

    // 3.2 / 7.1 default goal per mood (M04 → G01 for calculation, D3)
    val DEFAULT_GOAL = mapOf(
        MoodId.M01 to GoalId.G03, MoodId.M02 to GoalId.G03, MoodId.M03 to GoalId.G02,
        MoodId.M04 to GoalId.G01, MoodId.M05 to GoalId.G02, MoodId.M06 to GoalId.G01,
    )
    val ALLOWED_GOALS = mapOf(MoodId.M01 to listOf(GoalId.G03), MoodId.M02 to listOf(GoalId.G02, GoalId.G03), MoodId.M06 to listOf(GoalId.G01))

    // 5.4 C
    const val C_TODAY_REPORTED = 90; const val C_PATTERN = 65; const val C_EARLY_SIGNAL = 60
    const val PATTERN_TOLERANCE_DAYS = 1; const val PATTERN_LOOKBACK_CYCLES = 3; const val PATTERN_MIN_HITS = 2
    val C_ELEVATED_PHASES = setOf(Phase.P1, Phase.P5)

    // 5.5 symptom → related cares. Heat-conditional pairs need heatPreferred (D6).
    val SYMPTOM_CARES: Map<Symptom, Set<CareId>> = mapOf(
        Symptom.ABDOMINAL to setOf(COMFORT),
        Symptom.LOWER_BACK to setOf(COMFORT, REPLAY),
        Symptom.HOT_STUFFY to setOf(COMFORT),
        Symptom.IRRITABLE to setOf(QUIET), Symptom.HEADACHE to setOf(QUIET), Symptom.SENSORY_DISCOMFORT to setOf(QUIET),
        Symptom.FATIGUE to setOf(REST, RESET), Symptom.SLEEPY to setOf(REST, RESET),
        Symptom.LOW_MOOD to setOf(MUSIC, RESET),
        Symptom.HEAVY_FLOW to setOf(PLACE), Symptom.TOILET_WORRY to setOf(PLACE),
    )
    val HEAT_CONDITIONAL = setOf(Symptom.ABDOMINAL to COMFORT, Symptom.LOWER_BACK to COMFORT)

    // 7.5 fatigue
    const val FATIGUE_MIN_INTENSITY = 2

    // 5.6 cycle prediction
    const val DEFAULT_MU = 29; const val DEFAULT_W = 4; const val DEFAULT_PERIOD_DAYS = 5
    const val CYCLE_MIN_VALID = 15; const val CYCLE_MAX_VALID = 60; const val CYCLE_LOOKBACK = 6
    const val W_MIN = 2; const val IRREGULAR_SPREAD_DAYS = 10; const val IRREGULAR_W_MIN = 5
    const val LUTEAL_DAYS = 14
    const val P5_DAYS = 6
    const val READY_KIT_DAYS_BEFORE_RANGE = 2
    const val K_WITH_BUCKET = 2; const val K_WITHOUT_BUCKET = 1
    const val LONG_DRIVE_MIN = 90; const val LONG_DRIVE_MIN_FREQUENT = 60

    val MU0 = mapOf(
        CycleBucket.D21_24 to 23, CycleBucket.D25_28 to 27, CycleBucket.D29_32 to 30,
        CycleBucket.D33_35 to 34, CycleBucket.D36_PLUS to 38, CycleBucket.UNKNOWN to 29,
    )
    val W0 = mapOf(Regularity.SAME to 2, Regularity.FEW_DAYS to 3, Regularity.VARIES to 5, Regularity.UNKNOWN to 4)
    val PERIOD_DAYS = mapOf(DurationBucket.D3_4 to 4, DurationBucket.D5_6 to 5, DurationBucket.D7_PLUS to 7, DurationBucket.UNKNOWN to 5)

    // 5.7 SOS
    val SOS_RADII_M = listOf(1000, 3000, 5000, 10000)
    const val SOS_MIN_ELIGIBLE = 3; const val SOS_TOP_N = 3; const val SOS_ROUTE_BUFFER = 10
    const val SOS_DEDUP_M = 50.0
    const val TOILET_W_T = 0.50; const val TOILET_W_O = 0.30; const val TOILET_W_A = 0.15; const val TOILET_W_R = 0.05
    const val STORE_W_T = 0.50; const val STORE_W_P = 0.30; const val STORE_W_O = 0.15; const val STORE_W_R = 0.05
    const val T_PER_MIN = 5.0
    const val LOCATION_MAX_AGE_SEC = 30L; const val LOCATION_MAX_ACCURACY_M = 50.0
    val ETA_POLICY = EtaPolicy.ESTIMATE // D10
    const val ETA_PARK_UNKNOWN_MIN = 1.0 // D10 ESTIMATE
    const val WALK_DETOUR = 1.3; const val WALK_M_PER_MIN = 67.0
    const val WALK_UNKNOWN_M = 100.0 // TODO(spec): no parking→entrance distance at all; assumed, shown as "추정"

    // 9 triggers (D12)
    const val CARD_SNOOZE_HOURS = 24L
    const val MAX_CARDS_PER_DRIVE = 2

    // 7.7 care actions
    const val RESET_MINUTES = 3; const val RESET_MINUTES_LONG = 5 // D14
    const val REST_PARKED_MINUTES = 10 // D15
    const val COMFORT_DEFAULT_HEAT = 1

    // 5.8 playlist tags (examples; personal music preference wins when present). M04: MUSIC never shown.
    val PLAYLIST_TAGS: Map<MoodId, Map<GoalId, String>> = mapOf(
        MoodId.M01 to mapOf(GoalId.G01 to "잔잔한 어쿠스틱", GoalId.G02 to "밝은 팝·인디팝", GoalId.G03 to "좋아하는 활기찬 음악"),
        MoodId.M02 to mapOf(GoalId.G01 to "로파이·연주곡", GoalId.G02 to "산뜻한 인디팝", GoalId.G03 to "부드러운 어쿠스틱"),
        MoodId.M03 to mapOf(GoalId.G01 to "편안한 인스트루멘털", GoalId.G02 to "경쾌한 팝", GoalId.G03 to "평소 선호 음악"),
        MoodId.M05 to mapOf(GoalId.G01 to "따뜻한 어쿠스틱", GoalId.G02 to "밝은 기분 전환 음악", GoalId.G03 to "익숙하고 편안한 음악"),
        MoodId.M06 to mapOf(GoalId.G01 to "자극 적은 연주곡", GoalId.G02 to "부드러운 리듬의 인디팝", GoalId.G03 to "익숙한 저자극 음악"),
    )

    // D9 (assumption, needs confirmation): place kinds per goal
    val PLACE_KINDS = mapOf(
        GoalId.G01 to listOf("공원", "조용한 카페"),
        GoalId.G02 to listOf("카페", "전망 좋은 곳"),
        GoalId.G03 to listOf("자주 간 장소"),
    )

    // 10.3 / D13
    const val SPECIALIST_INTENSITY = 3; const val SPECIALIST_CYCLES = 3
}
