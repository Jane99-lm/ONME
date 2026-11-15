package com.onme.core

// User-facing text. DriveCopy = everything that can appear while driving: no cycle words,
// symptom names or scores (0.1-5, UI3). No medical-efficacy claims anywhere (0.1-8).

object Copy {
    const val APP = "온미"
    const val PARK_FIRST = "안전하게 정차한 뒤 이용할 수 있어요"
    const val FATIGUE_NOTICE = "피로가 느껴질 때는 안전한 곳에 정차해 쉬는 것이 먼저예요. 케어가 휴식을 대신할 수는 없어요."
    const val UNSUPPORTED = "이 차량에서는 지원되지 않는 기능이에요"
    const val DEMO_CAR = "차량이 설정을 저장하지 않아 시연용 가상 차량에 적용했어요"
    const val COMFORT_WARM = "따뜻한 시트를 준비했어요"
    const val COMFORT_COOL = "시원한 통풍을 준비했어요"
    const val GENERAL = "지금 할 수 있는 케어예요"
    const val APPLIED = "적용했어요"
    const val REVERT = "되돌리기"

    val MAIN = mapOf(
        CareId.COMFORT to COMFORT_WARM,
        CareId.QUIET to "오늘은 조용한 차량 환경을 추천해요",
        CareId.RESET to "잠시 멈춘 지금, 3분만 쉬어가요",
        CareId.REPLAY to "지난번 편안했던 설정을 다시 준비했어요",
        CareId.PLACE to "근처에 잠시 들를 만한 곳이 있어요",
        CareId.REST to "안전한 곳에 정차해 쉬어가세요",
        CareId.MUSIC to "지금 기분에 어울리는 플레이리스트가 있어요",
    )
    val CARE_NAME = mapOf(
        CareId.COMFORT to "편안한 시트·공조", CareId.QUIET to "조용한 환경", CareId.RESET to "잠깐 휴식",
        CareId.REPLAY to "지난 설정 다시", CareId.PLACE to "들를 곳", CareId.REST to "쉬어가기", CareId.MUSIC to "플레이리스트",
    )
    /** [primary, secondary] button labels (7.7). */
    val BUTTONS = mapOf(
        CareId.COMFORT to ("적용" to "취소"), CareId.QUIET to ("적용" to "취소"), CareId.RESET to ("시작" to "닫기"),
        CareId.REPLAY to ("다시 적용" to "취소"), CareId.PLACE to ("경유지 추가" to "나중에"), CareId.REST to ("확인" to ""),
        CareId.MUSIC to ("열기" to "닫기"),
    )
    val MOOD = mapOf(
        MoodId.M01 to "😊 좋아요", MoodId.M02 to "😌 평온해요", MoodId.M03 to "😐 보통이에요",
        MoodId.M04 to "😮‍💨 지쳤어요", MoodId.M05 to "😢 울적해요", MoodId.M06 to "😣 예민해요",
    )
    val GOAL = mapOf(GoalId.G01 to "차분해지기", GoalId.G02 to "기분 전환", GoalId.G03 to "기분 유지")
    val RATING = mapOf(Rating.POSITIVE to "좋았어요", Rating.NEUTRAL to "보통", Rating.NEGATIVE to "별로였어요")
    const val FEEDBACK_Q = "방금 케어는 어땠나요?"
    const val REST_DRIVING = "안전한 곳에서 잠시 쉬어가세요"
    const val REST_PARKED = "10분 정도 쉬거나 가볍게 걸어보세요"
    const val RESET_GUIDE = "숨을 천천히 들이쉬고, 더 길게 내쉬어요"

    // Trigger cards (9)
    const val TR2 = "쉬어갈 곳을 경로에 추가할까요?"
    const val TR3 = "준비물을 확인해 보세요"
    const val LATER = "나중에"
    const val OK = "적용"

    // Parked-only screens may name cycle phases/symptoms (6.4 note); never on cards.
    const val ESTIMATE_DISCLAIMER = "예상 범위는 추정이며 실제와 다를 수 있어요. 피임이나 임신 판단에 사용할 수 없어요."
    const val SPECIALIST = "강한 불편이 반복되고 있어요. 전문의와 상담해 보는 것도 좋아요."
    const val OVERDUE = "기록을 확인해 주세요"
    const val READY_KIT_ITEMS = "생리용품 · 여벌 속옷 · 물티슈 · 따뜻한 물"
}

object SymptomCopy {
    val LABEL = mapOf(
        Symptom.ABDOMINAL to "복부 불편", Symptom.LOWER_BACK to "허리 불편", Symptom.HOT_STUFFY to "더위·답답함",
        Symptom.IRRITABLE to "예민함", Symptom.HEADACHE to "두통", Symptom.SENSORY_DISCOMFORT to "감각 불편",
        Symptom.FATIGUE to "피로", Symptom.SLEEPY to "졸림", Symptom.LOW_MOOD to "울적함",
        Symptom.HEAVY_FLOW to "생리량 많음", Symptom.TOILET_WORRY to "화장실 걱정",
    )
}

object SosCopy {
    const val TITLE = "급할 때 찾기"
    val CATEGORY = mapOf(SosCategory.TOILET to "화장실", SosCategory.STORE to "편의점")
    const val STORE_NOTE = "생리용품 재고는 확인되지 않아요"
    const val NONE = "근처에서 찾지 못했어요"
    const val RETRY = "다시 검색"
    const val OTHER = "반대 카테고리 검색"
    const val ESTIMATED = "추정"
    const val LOCATION_WEAK = "위치 정확도가 낮아 결과가 다를 수 있어요"
    const val GPS_ERROR = "위치를 확인할 수 없어요. 위치 권한과 GPS 상태를 확인해 주세요"
    const val NET_ERROR = "장소 정보를 불러오지 못했어요. 네트워크를 확인해 주세요"
    const val TRY_AGAIN = "다시 시도"
    const val NAV_FALLBACK = "내비에서 이 장소를 검색해 주세요"
    const val SENT = "내비로 보냈어요"

    fun open(s: OpenStatus) = when (s) {
        OpenStatus.OPEN -> "개방시간상 이용 가능"
        OpenStatus.UNKNOWN -> "운영시간 미확인"
        OpenStatus.CLOSED, OpenStatus.PERMANENTLY_CLOSED -> "이용 불가"
    }
    fun parking(s: ParkingStatus) = when (s) {
        ParkingStatus.DEDICATED -> "전용 주차 공간 있음"
        ParkingStatus.NEARBY -> "근처 주차 공간 있음"
        ParkingStatus.UNKNOWN, ParkingStatus.UNSAFE -> "주차 정보 미확인"
    }
    fun access(s: AccessStatus) = when (s) {
        AccessStatus.PUBLIC -> "누구나 이용"
        AccessStatus.CONDITIONAL -> "조건부 이용"
        AccessStatus.UNKNOWN, AccessStatus.BLOCKED -> "이용 조건 미확인"
    }
    fun reliability(r: Double) = when {
        r >= 100 -> "최근 현장 확인"
        r >= 80 -> "여러 출처 일치"
        r >= 60 -> "공식 출처"
        else -> "정보 확인 필요"
    }

    private fun up(x: Double) = kotlin.math.ceil(x).toInt()

    /** "차량 6분 + 도보 약 2분" — split by leg, unknown legs named as unknown (8.3). */
    fun eta(e: EtaResult): String {
        val drive = e.drive?.let { "차량 약 ${up(it)}분" } ?: "차량 이동 시간 미확인"
        val walk = e.walk?.let { "도보 약 ${up(it)}분" } ?: "도보 시간 미확인"
        val total = e.total?.let { "약 ${up(it)}분" + if (e.estimated) " ($ESTIMATED)" else "" }
        return listOfNotNull(total, "$drive · $walk").joinToString(" · ")
    }
}

object DriveCopy {
    /** Every static string that can be on screen while DRIVING (UI3 test input). */
    fun all(): List<String> = buildList {
        add(Copy.APP); add(Copy.PARK_FIRST); add(Copy.FATIGUE_NOTICE); add(Copy.UNSUPPORTED)
        add(Copy.COMFORT_WARM); add(Copy.COMFORT_COOL); add(Copy.GENERAL); add(Copy.APPLIED); add(Copy.REVERT)
        addAll(Copy.MAIN.values); addAll(Copy.CARE_NAME.values); addAll(Copy.MOOD.values); addAll(Copy.GOAL.values)
        Copy.BUTTONS.values.forEach { add(it.first); add(it.second) }
        add(Copy.REST_DRIVING); add(Copy.TR2); add(Copy.LATER); add(Copy.OK)
        OnmeConfig.PLAYLIST_TAGS.values.forEach { addAll(it.values) }
        OnmeConfig.PLACE_KINDS.values.forEach { addAll(it) }
        add(SosCopy.TITLE); addAll(SosCopy.CATEGORY.values); add(SosCopy.STORE_NOTE); add(SosCopy.NONE)
        add(SosCopy.RETRY); add(SosCopy.OTHER); add(SosCopy.LOCATION_WEAK); add(SosCopy.GPS_ERROR); add(SosCopy.NET_ERROR)
        add(SosCopy.TRY_AGAIN); add(SosCopy.NAV_FALLBACK); add(SosCopy.SENT)
        OpenStatus.entries.forEach { add(SosCopy.open(it)) }
        ParkingStatus.entries.forEach { add(SosCopy.parking(it)) }
        AccessStatus.entries.forEach { add(SosCopy.access(it)) }
        listOf(100.0, 80.0, 60.0, 0.0).forEach { add(SosCopy.reliability(it)) }
    }.filter { it.isNotEmpty() }
}
