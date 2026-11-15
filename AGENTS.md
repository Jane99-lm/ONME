# 온미 | ONME — Pleos 앱 개발 통합 명세 (AI 코딩 에이전트용)

**문서 버전:** 1.0
**작성일:** 2026-11-04
**통합한 원본 문서**
- `HERA_Pleos_Connect_UX_Proposal.md` (UX 기획서, 2026-11-03)
- `algorithm/Yeoon_Cycle_Care_Calendar_Algorithm.md` v0.2 (STEP 0, 3–4: 초기 질문·주기 예측·C 점수)
- Mood Care 추천 알고리즘 STEP 5–6 v1.0 (MUSIC 포함)
- SOS Location Finder 알고리즘 V2.1
- `여온 - 주장.pdf` (기능별 연구 근거와 적용 범위)

> 이 문서를 AI 코딩 에이전트(Claude Code, Cursor 등)에 그대로 붙여 넣거나 저장소 루트에 `AGENTS.md`로 두고 작업을 시키면 된다. 0장은 에이전트에게 주는 지시문이고, 1장부터는 명세다.

---

## 0. 에이전트에게 주는 지시

너는 Hyundai **Pleos Connect**(Android Automotive OS 기반 차량 인포테인먼트) 위에서 동작하는 여성 운전자 케어 앱 **온미 | ONME**의 MVP를 만든다. 아래 규칙은 협상 불가다.

### 0.1 절대 규칙 (어기면 버그)

1. **안전이 점수보다 먼저다.** 안전·동의·권한 필터를 통과하지 못한 후보는 점수를 매기지 않고 제외한다. 높은 점수로 안전 조건을 상쇄하지 않는다.
2. **피로 상태(M04 또는 오늘 피로·졸림 기록)이면 메인 추천은 항상 REST**다. 점수와 무관하다.
3. **RESET(정차 휴식 케어)·시트 위치 변경·긴 입력·캘린더 조작·공유 설정은 정차(PARKED) 상태에서만** 실행·표시한다.
4. **차량 설정 변경, 경유지/목적지 전송, 외부 음악 서비스 열기는 사용자가 명시적으로 누른 뒤에만** 실행한다. 자동 실행 없음.
5. **차량 화면에 주기 구간 이름(“생리 전” 등)·증상명·점수 숫자를 노출하지 않는다.** 행동 문구만 보여준다.
6. **주기·배란은 “예상 범위/추정”으로만 표시**한다. 피임·임신 판단 용도 불가 고지를 붙인다.
7. **생리 주기만으로 증상·감정·통증을 추정하지 않는다.** C 점수는 사용자가 직접 기록하거나 답한 근거가 있을 때만 50보다 올라간다.
8. **의학적 효능을 주장하는 문구 금지.** (“통증 완화”, “치료” 등) 연구 근거는 발표 자료용이며 앱 문구가 아니다.
9. **SOS에서 확인되지 않은 ETA를 0분으로 취급하지 않는다.** “확실히 열려 있음” 같은 확정 표현을 쓰지 않는다.
10. **모든 가중치·임계값·룩업 표는 코드에 흩어 쓰지 말고 `core/config` 한 곳의 상수로 둔다.** 이 값들은 검증 전 초기값이라 바뀔 예정이다.

### 0.2 작업 방식

- **핵심 로직(`:core`)은 Android 의존성이 없는 순수 Kotlin**으로 만들고, 12장의 테스트를 **먼저** 작성한 뒤 구현한다. 12장 숫자가 모두 재현되어야 완료다.
- 차량·지도·음악 연동은 **인터페이스 + Mock 구현**으로 먼저 만든다. 실제 Pleos SDK 연동은 11장 검증 후 어댑터만 갈아 끼운다.
- **시연 모드(Demo Mode)** 를 반드시 넣는다: 주행/정차 전환, 차량 기능 지원 여부 토글, 가상 위치, 가상 주기 데이터(9.4 사용자 A) 로드, 시연 초기화.
- 13장의 “결정 필요” 항목은 표에 적힌 **기본 채택안**대로 구현하고, 상수/플래그로 바꿀 수 있게 둔다.
- 명세에 없는 동작이 필요하면 임의로 정하지 말고 `TODO(spec):` 주석과 함께 가장 보수적인(안전·비노출) 쪽으로 구현한다.

---

## 1. 제품 범위

### 1.1 한 줄 정의
사용자가 고른 **기분·케어 목표**와 직접 기록한 **생리 주기·증상**, **과거 만족도**, **차량 상태**를 종합해 실행 가능한 차량 케어를 **추천**하고, 급할 때 **화장실·편의점**을 빠르고 안전하게 찾아주는 Pleos 앱.

### 1.2 이름
- 앱 표기: **온미 | ONME** (앱 안 표기는 “온미”, 영문 워드마크 ONME. “여온”·UX 기획서의 “HERA”는 이전 이름. UI·코드·리소스에 HERA를 쓰지 않는다.)
- 패키지 예시: `com.onme.app` (확정 아님)

### 1.3 MVP 기능 (이번에 만든다)

| ID | 기능 | 출처 | 비고 |
|---|---|---|---|
| F1 | 초기 질문(온보딩) Q0–Q8 | 주기 알고리즘 3장 | 정차 중에만. Q0(타 앱 가져오기)는 Mock |
| F2 | 생리·증상 기록 | 주기 알고리즘 4장 | 정차 중 상세, 주행 중 불가 |
| F3 | 주기 예측·캘린더(P1–P5, 예정 범위) | 주기 알고리즘 5–6장 | |
| F4 | Mood Care 추천(감정 6 × 목표 3 → 케어 7) | Mood Care STEP 5–6 | 앱의 핵심 |
| F5 | 케어 실행(COMFORT/QUIET/RESET/REPLAY/PLACE/REST/MUSIC) | Mood Care 3장 | 사용자 승인 후 |
| F6 | 만족도 피드백 → P 점수 | Mood Care 4.4 | 정차 후 |
| F7 | SOS: 화장실/편의점 상위 3곳 | SOS V2.1 | |
| F8 | 상황 트리거 카드(출발 전, 장거리, Ready Kit) | UX 8장, 주기 8장 | 빈도 제한 |
| F9 | 개인정보·동의 설정 | UX 13장 | |
| F10 | 시연 모드 | UX 9장 | |

### 1.4 MVP 범위 밖 (만들지 않는다, 자리만 남김)
Circle Care(가족 공유), Family Planning(가임기 상세), Health Passport(진료용 내보내기), 모바일 동반 앱, 장애인 접근성 필터(데이터 확보 전), 음성 STT 입력. 단, **배란 추정 표시 on/off 플래그(Family Planning 사용자만)** 는 F3에 둔다.

---

## 2. 아키텍처

### 2.1 권장 스택 (Pleos SDK 확인 후 조정)
- Kotlin, Jetpack Compose, Android Automotive OS 앱
- 로컬 저장: Room + 암호화(SQLCipher 또는 EncryptedFile/Keystore). 서버 없음(MVP)
- 테스트: JUnit5 + kotest(선택). `:core`는 JVM 테스트만으로 검증

### 2.2 모듈

```text
:core        순수 Kotlin. 모델, 상수(config), 주기 예측, C/P 점수, Mood Care, SOS 점수, 트리거 규칙
:data        Room DB, 암호화, Repository (기록·피드백·동의·프로필)
:vehicle     VehicleGateway 인터페이스 / PleosVehicleGateway / MockVehicleGateway
:places      PlaceProvider, RouteProvider, NavigationLauncher 인터페이스 + Mock
:media       MusicProvider 인터페이스 + Mock (플레이리스트 표시/외부 앱 열기)
:app         Compose UI, 화면 상태 머신, 주행 제한 처리, 시연 모드
```

### 2.3 외부 연동 인터페이스 (모두 Mock 먼저)

```kotlin
interface VehicleGateway {
    fun capabilities(): VehicleCapabilities                 // 차종별 지원 기능
    fun drivingState(): Flow<DrivingState>                  // PARKED / DRIVING / UNKNOWN
    fun uxRestricted(): Flow<Boolean>                       // 플랫폼 주행 중 UI 제한
    suspend fun readCabin(): CabinState                     // 실내온도, 현재 열선/통풍/팬
    suspend fun apply(settings: VehicleSettings): ApplyResult  // 사용자 승인 후에만 호출
}
interface PlaceProvider { suspend fun search(cat: SosCategory, center: GeoPoint, radiusM: Int): List<RawPlace> }
interface RouteProvider { suspend fun driveEta(from: GeoPoint, to: GeoPoint): EtaResult }   // 실제 도로 경로 시간
interface NavigationLauncher { fun isSupported(): Boolean; suspend fun sendDestination(p: Place, asWaypoint: Boolean): Boolean }
interface MusicProvider { fun isSupported(): Boolean; suspend fun playlistsFor(tag: PlaylistTag, prefs: MusicPrefs): List<Playlist>; suspend fun open(p: Playlist): Boolean }
interface LocationSource { suspend fun current(): LocationFix }   // lat, lng, accuracyM, timestamp
```

### 2.4 주행 상태 판정

```text
DrivingState.PARKED  : 기어 P 또는 (주차 브레이크 ON 그리고 속도 0)
DrivingState.DRIVING : 그 외 속도 > 0 또는 기어 D/R
DrivingState.UNKNOWN : 신호를 읽을 수 없음 → **DRIVING과 동일하게 취급** (보수적)
```
플랫폼이 UX 제한(uxRestricted)을 알려주면 그 값을 우선한다.

---

## 3. 화면과 주행 제한

### 3.1 화면 목록

| 화면 | 내용 | 주행 중 | 정차 중 |
|---|---|---|---|
| S0 온보딩 | Q0–Q8 | ✕ (진입 차단, “정차 후 진행”) | ○ |
| S1 홈 | 기분 6개 버튼, SOS 버튼, 오늘의 케어 카드 | ○ (1탭 선택만) | ○ |
| S2 목표 선택 | 기본 목표 표시 + 3개 중 변경 (M01은 G03만, M02는 G02·G03만, M06은 G01만) | ○ (1탭) | ○ |
| S3 추천 결과 | 메인 1 + 선택지 최대 2, 또는 일반 케어 선택 | ○ (큰 버튼, 텍스트 최소) | ○ |
| S4 케어 실행 확인 | 적용 내용 요약, [적용] [취소] | ○ (1탭) | ○ |
| S5 RESET 휴식 가이드 | 호흡 안내 타이머 | ✕ | ○ |
| S6 피드백 | 좋았어요 / 보통 / 별로였어요 | ✕ | ○ |
| S7 캘린더 | 월 달력, 구간 색, 예정 범위 | ✕ | ○ |
| S8 기록 | 생리 시작/종료, 증상·강도 | ✕ | ○ |
| S9 SOS | 화장실/편의점 선택 → 상위 3곳 | ○ (2개 큰 버튼 + 최대 3행) | ○ |
| S10 설정 | 동의, 알림 빈도, 차량 연결 권한, 시연 모드 | ✕ | ○ |
| C1 트리거 카드 | 기존 화면 위 작은 카드 [적용] [나중에] | ○ (허용 시) | ○ |

- 주행 중 차단된 화면에 들어가려 하면 “안전하게 정차한 뒤 이용할 수 있어요”를 보여주고 아무 입력도 받지 않는다.
- 주행 중 상태로 바뀌면 열려 있던 정차 전용 화면은 즉시 닫고 홈으로 돌아간다(입력 중이던 기록은 임시 저장).
- 주행 중에는 내비 턴바이턴·경고보다 온미 카드를 위에 띄우지 않는다.
- 다른 앱 위 오버레이(C1) 가능 여부는 미확정(11장). 불가하면 앱 안에서만 표시하고 출발 전 카드로 대체한다.

### 3.2 홈 기분 버튼

| Mood ID | 라벨 | V | A | 기본 목표 |
|---|---|---|---|---|
| M01 | 😊 좋아요 | +2 | +1 | G03 |
| M02 | 😌 평온해요 | +1 | −1 | G03 |
| M03 | 😐 보통이에요 | 0 | 0 | G02 (2026-11-06 변경) |
| M04 | 😮‍💨 지쳤어요 | −1 | −2 | REST 우선 (계산용 목표 G01, 13장 D3) |
| M05 | 😢 울적해요 | −2 | −1 | G02 |
| M06 | 😣 예민해요 | −2 | +2 | G01 |

V/A는 기록·분석용 메타데이터일 뿐, 점수 계산에는 쓰지 않는다(M 값은 4.2 룩업에서 가져온다).

**목표 ID:** G01 차분해지기 / G02 기분 전환 / G03 기분 유지. 사용자가 목표를 바꾸면 즉시 재계산한다.
**기분별 목표 제한 (2026-11-06 규칙 변경):** M01 좋아요는 G03 기분 유지만, M02 평온해요는 G02 기분 전환·G03 기분 유지만, M06 예민해요는 G01 차분해지기만 보여주고 추천한다. 나머지 기분은 3개 모두 (`ALLOWED_GOALS`, `goalsFor(mood)`).

---

## 4. 데이터 모델

```kotlin
enum class MoodId { M01, M02, M03, M04, M05, M06 }
enum class GoalId { G01, G02, G03 }
enum class CareId(val order: Int) { COMFORT(1), QUIET(2), RESET(3), REPLAY(4), PLACE(5), REST(6), MUSIC(7) }  // C01..C07, 동점 정렬에 order 사용
enum class DrivingState { PARKED, DRIVING, UNKNOWN }
enum class Phase { P1, P2, P3, P4, P5, OVERDUE, PAUSED }
enum class Rating { POSITIVE, NEUTRAL, NEGATIVE }   // 좋았어요 / 보통 / 별로였어요

enum class Symptom { ABDOMINAL, LOWER_BACK, HOT_STUFFY, IRRITABLE, HEADACHE, SENSORY_DISCOMFORT,
                     FATIGUE, SLEEPY, LOW_MOOD, HEAVY_FLOW, TOILET_WORRY }

data class SymptomLog(val date: LocalDate, val symptom: Symptom, val intensity: Int /*0..3*/)
data class PeriodRecord(val start: LocalDate, val end: LocalDate?)

data class OnboardingAnswers(
    val importedStarts: List<LocalDate>?,     // Q0
    val lastStart: LocalDate,                 // Q1 (필수)
    val lastStartUncertain: Boolean,          // Q1 "정확하지 않아요"
    val previousStarts: List<LocalDate>,      // Q2 날짜 1–2개
    val usualCycleBucket: CycleBucket?,       // Q2 평소 주기 → μ₀
    val regularity: Regularity?,              // Q3 → w₀
    val durationBucket: DurationBucket?,      // Q4
    val earlySignals: Set<Pair<Symptom, SignalTiming>>, // Q5 (BEFORE / DURING / BOTH)
    val thermal: ThermalPref?,                // Q6 COLD_SENSITIVE / HEAT_SENSITIVE / NORMAL
    val preferredSeatHeatLevel: Int?,         // Q6 열선 단계
    val drivingPattern: DrivingPattern?,      // Q7 COMMUTE / LONG_DISTANCE / OCCASIONAL
    val situation: Situation?                 // Q8 HORMONAL_PILL / PREGNANT / POSTPARTUM / PERIMENOPAUSE / NONE / NO_ANSWER
)

data class CycleForecast(
    val expectedStart: LocalDate, val rangeStart: LocalDate, val rangeEnd: LocalDate,
    val muDays: Int, val wDays: Int, val periodDays: Int,
    val ovulationEstimate: LocalDate?, val irregular: Boolean, val nCycles: Int,
    val accuracyStage: Int  // 1 시작 / 2 학습 중 / 3 개인화
)

data class VehicleCapabilities(
    val seatHeat: Boolean, val seatVent: Boolean, val hvacTemp: Boolean, val hvacFan: Boolean,
    val ambientLight: Boolean, val seatPosition: Boolean, val navigation: Boolean, val overlayCard: Boolean
)
data class VehicleSettings(  // null = 변경하지 않음
    val seatHeatLevel: Int? = null, val seatVentLevel: Int? = null, val cabinTempC: Double? = null,
    val fanLevel: Int? = null, val ambientDim: Boolean? = null, val seatProfileId: String? = null
)

data class Consents(
    val healthPersonalization: Boolean,   // 주기·증상 사용 (끄면 C=50 고정)
    val learnFromFeedback: Boolean,       // 끄면 P=기본값 고정
    val location: Boolean,                // 끄면 PLACE, SOS 불가
    val vehicleControl: Boolean,          // 끄면 COMFORT/QUIET/REPLAY 불가
    val musicService: Boolean,            // 끄면 MUSIC 불가
    val proactiveCards: Boolean           // 트리거 카드
)

data class CareSession(
    val id: String, val at: Instant, val mood: MoodId, val goal: GoalId,
    val ranked: List<Pair<CareId, Double>>, val shownMain: CareId?, val chosen: CareId?,
    val applied: VehicleSettings?, val phase: Phase?, val phaseOffset: Int?,
    val rating: Rating?                    // 정차 후 피드백
)

data class ComfortPreset(val id: String, val name: String, val settings: VehicleSettings, val createdFrom: String /*ONBOARDING|USER|REPLAY*/)
```

---

## 5. 상수와 룩업 표 (`core/config`)

> 모두 **온미가 정한 제품 설계용 초기값**이다. 임상적으로 검증된 값이 아니다. 하나의 파일(`OnmeConfig.kt`)에 두고 테스트도 이 파일을 참조한다.

### 5.1 Mood Care 가중치

```text
W_M = 0.30, W_G = 0.30, W_P = 0.20, W_C = 0.10, W_D = 0.10
MAIN_THRESHOLD = 70.0        # 최고 점수 ≥ 70 이면 메인 카드
NEAR_TIE_GAP   = 5.0         # 1위와의 차이가 5 미만이면 선택지로 함께 표시
MAX_ALTERNATIVES = 2
P_DEFAULT = 50, C_DEFAULT = 50, D_FEASIBLE = 100
P_WINDOW = 5, P_POS_STEP = +10, P_NEG_STEP = −15
```

**정수 계산 규칙:** 부동소수 오차를 피하려고 `S×10 = 3M + 3G + 2P + C + D` 로 정수 계산한 뒤 10으로 나눈다. (예: 0.3×93은 27.900000000000002가 되므로 직접 곱하지 않는다.)

### 5.2 M — 감정별 케어 적합도

| 감정 | COMFORT | QUIET | RESET | REPLAY | PLACE | REST | MUSIC |
|---|---|---|---|---|---|---|---|
| M01 좋아요 | 85 | 65 | 55 | 85 | 80 | 10 | 90 |
| M02 평온해요 | 65 | 80 | 60 | 95 | 55 | 20 | 85 |
| M03 보통이에요 | 75 | 65 | 50 | 90 | 75 | 30 | 85 |
| M04 지쳤어요 | 65 | 45 | 70 | 35 | 35 | 100 | 20 |
| M05 울적해요 | 70 | 70 | 95 | 35 | 80 | 55 | 85 |
| M06 예민해요 | 93 | 95 | 90 | 40 | 55 | 50 | 80 |

### 5.3 G — 목표별 케어 적합도

| 목표 | COMFORT | QUIET | RESET | REPLAY | PLACE | REST | MUSIC |
|---|---|---|---|---|---|---|---|
| G01 차분해지기 | 85 | 100 | 95 | 55 | 35 | 85 | 85 |
| G02 기분 전환 | 50 | 25 | 80 | 50 | 100 | 40 | 95 |
| G03 기분 유지 | 80 | 75 | 45 | 100 | 55 | 50 | 90 |

### 5.4 C — 주기·증상 맥락 점수

```text
C_TODAY_REPORTED = 90   # 오늘 관련 증상 직접 기록
C_PATTERN        = 65   # 같은 구간·같은 정렬일 ±1일, 최근 3주기 중 2회 이상 (기록 3주기 이상일 때만)
C_EARLY_SIGNAL   = 60   # 기록 3주기 미만 + Q5에서 해당 시기 불편 응답
C_DEFAULT        = 50
PATTERN_TOLERANCE_DAYS = 1, PATTERN_LOOKBACK_CYCLES = 3, PATTERN_MIN_HITS = 2
C_ELEVATED_PHASES = {P1, P5}     # MVP에서 65·60 규칙은 P1·P5에만
```

### 5.5 증상 → 관련 케어

| 증상 | 관련 케어 | 90점 추가 조건 | 차량에서 하는 일 |
|---|---|---|---|
| ABDOMINAL 복부 불편 | COMFORT | **온열 선호**일 때만 (13장 D6) | 저장한 열선 단계 |
| LOWER_BACK 허리 불편 | COMFORT, REPLAY | COMFORT는 온열 선호일 때만 | 열선 + 정차 시 시트 프로필 |
| HOT_STUFFY 더위·답답함 | COMFORT | 없음 | 통풍(등받이+좌면) + 공조 |
| IRRITABLE, HEADACHE, SENSORY_DISCOMFORT | QUIET | 없음 | 팬 낮춤, 조명 낮춤, 온미 안내 최소화 |
| FATIGUE, SLEEPY | REST, RESET | 없음 | 안전한 정차·휴식 우선 (+피로 안전 규칙 발동) |
| LOW_MOOD 울적함 | MUSIC, RESET | 없음 | 플레이리스트, 정차 후 짧은 휴식 |
| HEAVY_FLOW, TOILET_WORRY | PLACE | 없음 | 장거리 시 화장실·편의점 경유 제안 |

**온열 선호(heatPreferred)** = Q6 “추위를 잘 타요” 이거나, 열선이 켜진 COMFORT/REPLAY 세션에 POSITIVE 평가가 1건 이상 있거나, 저장한 Comfort 프리셋의 열선 단계 > 0.

### 5.6 주기 예측 상수

```text
DEFAULT_MU = 29, DEFAULT_W = 4, DEFAULT_PERIOD_DAYS = 5
CYCLE_MIN_VALID = 15, CYCLE_MAX_VALID = 60, CYCLE_LOOKBACK = 6
W_MIN = 2, IRREGULAR_SPREAD_DAYS = 10, IRREGULAR_W_MIN = 5
LUTEAL_DAYS = 14 (배란 추정 O = E − 14)
P5_DAYS = 6 (E−6 ~ 다음 시작 전날), P4_END_OFFSET = 7 (E−7)
P3 = O−2 ~ O+1, P2 = 종료 다음 날 ~ O−3
READY_KIT_DAYS_BEFORE_RANGE = 2
```

**Q2 평소 주기 → μ₀:** 21–24일→23, 25–28일→27, 29–32일→30, 33–35일→34, 36일 이상→38(불규칙 모드), 모르겠어요→29
**Q3 규칙성 → w₀:** 거의 같아요 ±2, 며칠 차이 ±3, 많이 달라요 ±5(불규칙 모드), 모르겠어요 ±4
**Q4 생리 기간:** 3–4일→4, 5–6일→5, 7일 이상→7, 모르겠어요→5
**k (초기 응답 가중):** Q2를 평소 주기로 답함 → 2, Q2 모름(μ₀=29) → 1
**Q7 장거리 기준:** 기본 90분, “장거리 잦음” → 60분 (`LONG_DRIVE_MIN`)
**Q6:** 추위/더위 응답 → COMFORT의 P 기본값 60 (평가가 없을 때만, 13장 D7), 응답한 열선 단계를 Comfort Mode 기본값으로 저장

### 5.7 SOS 상수

```text
SOS_RADII_M = [1000, 3000, 5000, 10000]
SOS_MIN_ELIGIBLE = 3, SOS_TOP_N = 3, SOS_ROUTE_BUFFER = 10   # ETA 계산할 최대 후보 수
TOILET: W_T=0.50, W_O=0.30, W_A=0.15, W_R=0.05
STORE : W_T=0.50, W_P=0.30, W_O=0.15, W_R=0.05
T = max(0, 100 − 5 × t_total_min)
O: OPEN=100, UNKNOWN=30, CLOSED=제외, PERMANENTLY_CLOSED=제외
A: PUBLIC=100, CONDITIONAL=60, UNKNOWN=30, BLOCKED=제외
P: DEDICATED=100, NEARBY=70, UNKNOWN=40, UNSAFE=제외
R: 현장검증 ≤30일=100, 복수출처 일치 ≤90일=80, 공식 단일 출처=60, 오래됨/불명=30
LOCATION_MAX_AGE_SEC = 30, LOCATION_MAX_ACCURACY_M = 50   # 품질 목표 (보장 아님)
ETA_POLICY = STRICT | ESTIMATE   # 13장 D10
```

### 5.8 플레이리스트 태그

| 기분 | G01 차분해지기 | G02 기분 전환 | G03 기분 유지 |
|---|---|---|---|
| M01 | 잔잔한 어쿠스틱 | 밝은 팝·인디팝 | 사용자가 좋아하는 활기찬 음악 |
| M02 | 로파이·연주곡 | 산뜻한 인디팝 | 부드러운 어쿠스틱 |
| M03 | 편안한 인스트루멘털 | 경쾌한 팝 | 평소 선호 음악 |
| M04 | (MUSIC 표시 안 함, 휴식 우선) | 〃 | 〃 |
| M05 | 따뜻한 어쿠스틱 | 밝은 팝·기분 전환 음악 | 익숙하고 편안한 음악 |
| M06 | 자극 적은 연주곡 | 부드러운 리듬의 인디팝 | 익숙한 저자극 음악 |

장르는 예시 태그다. 사용자 음악 선호 데이터가 있으면 개인 취향을 우선한다.

---

## 6. 함수 명세 — 주기 (STEP 0, 3–4)

### 6.1 `cycleLengths(starts: List<LocalDate>): CycleLengths`
- 최근 최대 7개 시작일에서 연속 차이 L_i를 구한다(최대 6개).
- 15일 미만·60일 초과는 제외하고 `excluded` 목록으로 돌려준다 → UI가 “이 기록을 확인해 주세요” 요청.
- 시작일 소스: Q0 가져온 기록 + Q2 날짜 + 실제 기록(중복 제거, 정렬).

### 6.2 `predictCycle(starts, periodLengths, onboarding, today): CycleForecast`

```text
n = 유효 주기 수
μ₀ = Q2 버킷 값 ?: 29 ;  k = (Q2 버킷 응답) ? 2 : 1 ;  w₀ = Q3 값 ?: 4
n = 0     → μ = μ₀,                               w = w₀
n = 1–2   → μ = roundHalfUp((n·mean(L) + k·μ₀)/(n+k)),  w = w₀
n ≥ 3     → μ = median(L) (짝수 개면 가운데 두 값 평균을 roundHalfUp), w = max(2, ceil(stdev(L)))   # 표본 표준편차
n < 3 이고 Q1 "정확하지 않아요" → w += 1
irregular = (n ≥ 3 && max(L) − min(L) ≥ 10) || Q3 "많이 달라요" || Q2 "36일 이상" || Q8 폐경 전후
irregular → w = max(w, 5)
E = 마지막 시작일 + μ ;  범위 = [E − w, E + w]
periodDays = median(기록된 생리 기간) ?: Q4 ?: 5
O = E − 14  (배란 테스트 양성 입력 있으면 그 날짜)
accuracyStage = n==0 ? 1 : n<3 ? 2 : 3
```

**Q8 처리**
- HORMONAL_PILL: `ovulationEstimate = null`, 가임기 표시 끔. (복용 일정 기반 휴약기 출혈 예상은 MVP 밖, TODO)
- PREGNANT / POSTPARTUM: 예측 중지 → `Phase.PAUSED`, 캘린더는 기록만. 증상 기록과 차량 케어는 동작(C는 오늘 기록 90 규칙만).
- PERIMENOPAUSE: 불규칙 모드 + HOT_STUFFY 기록 입력 버튼을 앞에 배치.

**표준편차 주의:** 9.2 예시(30,28,31,29,32,30)는 표본 표준편차 1.41 → ceil 2. 모표준편차(1.29)도 ceil 2라 결과는 같지만 **표본 표준편차**로 고정한다.

### 6.3 `phaseOf(day, forecast, lastStart, lastEnd?): Pair<Phase, Int?>`
우선순위대로 판정한다.

```text
PAUSED 모드                         → (PAUSED, null)
day < lastStart + periodDays (또는 ≤ 실제 종료일) → (P1, day − lastStart + 1)     # D1, D2 …
day ≥ E                              → (OVERDUE, day − E)   # 예정일 지남, 시작 기록 없음 (13장 D8)
day ≥ E − 6                          → (P5, day − E)        # −6 … −1
irregular                            → (P2, null)            # 불규칙이면 P3·P4 표시 안 함
day ≤ O − 3                          → (P2, null)
day ≤ O + 1                          → (P3, null)
else                                 → (P4, null)
```
- 과거 주기의 P5 기록은 **실제 다음 시작일이 확정된 뒤** 그 날짜 기준 음수 오프셋으로 다시 계산해 저장한다.
- 캘린더 P3 표시·배란 추정 표시는 Family Planning 플래그가 켜진 경우만, 피임 불가 고지와 함께.

### 6.4 `cycleSymptomScore(user, care, today): Int`  (= C)

```text
if !consents.healthPersonalization → 50
if related(todaySymptoms, care) (5.5 표 + 90점 추가 조건) → 90
(phase, off) = phaseOf(today)
if phase ∉ {P1, P5} → 50
if phase == P5 && irregular → 50          # 불규칙이면 미래 P5에는 65·60 미적용 (P1은 유지)
if nCycles ≥ 3:
    hits = 최근 완료된 3주기 중, 같은 phase·정렬 오프셋 ±1일에 care 관련 증상 기록이 있는 주기 수
    return hits ≥ 2 ? 65 : 50
if onboarding.earlySignals에 (care 관련 증상, 해당 시기) 있음 → 60    # P1 ↔ DURING/BOTH, P5 ↔ BEFORE/BOTH
return 50
```
- 기록이 3주기 쌓이면 60 규칙은 사라진다.
- 3주기 동안 해당 구간에 관련 기록이 한 번도 없으면 그 초기 신호를 끄고 “이 시기 ○○ 추천을 줄일까요?” 확인을 정차 중에 1회 보낸다. (확인 문구에 증상명을 쓰는 것은 정차 중 앱 화면이라 허용, 트리거 카드에는 금지)

### 6.5 `afterFirstPrediction()` 확인 질문
첫 실제 시작일이 기록되면 1회: “예상했던 범위(MM/DD ~ MM/DD) 안에 시작했나요?” [맞았어요][더 빨랐어요][더 늦었어요]. “빨랐어요/늦었어요” 2회 연속이면 Q2·Q3를 다시 묻는다. 응답은 품질 지표로 저장.

---

## 7. 함수 명세 — Mood Care (STEP 5–6)

### 7.1 `defaultGoal(mood): GoalId`
M01·M02 → G03, **M03 → G02** (2026-11-06 변경), M05 → G02, M06 → G01, **M04 → G01** (13장 D3).

### 7.2 `isFeasible(care, ctx): Boolean`  (= D 게이트)
D는 100 또는 제외다. 제외된 후보는 순위에도, 화면에도 나오지 않는다.

| 케어 | 통과 조건 (모두 만족) |
|---|---|
| COMFORT | consents.vehicleControl && (seatHeat ‖ seatVent ‖ hvacTemp) 지원 |
| QUIET | consents.vehicleControl && (hvacFan ‖ ambientLight) 지원 (13장 D5) |
| RESET | drivingState == PARKED |
| REPLAY | consents.vehicleControl && POSITIVE 평가를 받은 적용 설정 스냅샷이 1개 이상 && 그 설정 중 현재 차량이 지원하는 항목이 1개 이상 |
| PLACE | consents.location && 위치 수신 성공 && (주행 중이면 navigation 지원) |
| REST | **항상 통과** (안내 기능. 차량 제어 없음) |
| MUSIC | consents.musicService && MusicProvider.isSupported() && mood ≠ M04 |

시트 위치 프로필(REPLAY/COMFORT 내 seatProfileId)은 PARKED일 때만 적용 항목에 포함한다. 주행 중이면 해당 항목만 빼고 나머지를 적용한다.

### 7.3 `preferenceScore(user, care): Int`  (= P)

```text
if !consents.learnFromFeedback → base(care)
ratings = 해당 care의 최근 평가 5건 (NEUTRAL 포함해서 5건을 센다)
if ratings.isEmpty() → base(care)
L = POSITIVE 수, N = NEGATIVE 수
P = clamp(50 + 10L − 15N, 0, 100)
base(care) = (care == COMFORT && Q6 ∈ {추위, 더위}) ? 60 : 50
```
예: 긍정 3, 부정 0 → 80. 긍정 1, 부정 2 → 30. 긍정 5 → 100.

### 7.4 `score(mood, goal, care, user, ctx): Double`

```text
S×10 = 3·M[mood][care] + 3·G[goal][care] + 2·P + C + D(=100)
S = (S×10) / 10.0
```

### 7.5 `fatigueRisk(mood, todaySymptoms): Boolean`
`mood == M04 || todaySymptoms ∋ SLEEPY || todaySymptoms ∋ FATIGUE(강도 ≥ 2)`

### 7.6 `recommend(input): Recommendation`

```text
1. feasible = CareId.values().filter(isFeasible)
2. ranked = feasible.map(score).sortedWith(점수 내림차순, 동점이면 CareId.order 오름차순)
3. if fatigueRisk:
       main = REST
       alternatives = ranked에서 REST·MUSIC·PLACE 제외 후 상위 2개 (13장 D4)
       notice = "피로가 느껴질 때는 안전한 곳에 정차해 쉬는 것이 먼저예요. 케어가 휴식을 대신할 수는 없어요."
       return SafetyFirst(main, alternatives, notice)
4. if ranked.isEmpty() || ranked[0].score < 70.0:
       return GeneralChoices(ranked.map { it.care })   # 강조 없이 가능한 케어 목록
       # 화면 (2026-11-06 변경): GeneralChoices도 Main과 같은 레이아웃. 1순위를 큰 카드로, 다음 2개를 행으로 보여준다.
5. main = ranked[0]
   alternatives = ranked[1..2].filter { main.score − it.score < 5.0 }   # 최대 2개 (13장 D2)
   return Main(main, alternatives)
```
- 결과와 모든 후보 점수는 `CareSession`에 저장한다(화면에는 숫자 노출 금지).
- 목표가 바뀌면 1부터 다시 계산한다.
- 주행 상태가 바뀌면(PARKED↔DRIVING) 다시 계산한다(RESET 진입/이탈).

### 7.7 케어별 실행 내용 `buildCareAction(care, user, ctx): CareAction`

| 케어 | 실행 내용 | 승인 UI |
|---|---|---|
| COMFORT | HOT_STUFFY 기록 또는 Q6 더위 → 통풍(등받이+좌면, 저장 단계) + 저장 온도. 그 외 → 저장 열선 단계 + 저장 온도. 저장값 없으면 열선 1단·온도 변경 없음 | [적용] [취소] |
| QUIET | 팬 단계 −1(최소 1), 앰비언트 조명 낮춤, 온미 트리거 카드·음성 안내 끄기(이번 주행 동안) | [적용] [취소] |
| RESET | 정차 휴식 가이드 3분(옵션 5분): 호흡 안내(날숨 길게), 선택 시 조명·온도 프리셋 | [시작] [닫기] |
| REPLAY | 같은 주기 구간에서 POSITIVE였던 설정 우선, 없으면 가장 최근 POSITIVE 설정 | [다시 적용] [취소] |
| PLACE | 목표에 맞는 주변 장소 1–3곳(13장 D9) → 선택 시 내비 경유지 전송 | [경유지 추가] [나중에] |
| REST | 주행 중: 짧은 안내 문구(+허용 시 음성) 1회, 조작 요구 없음. 정차 중: 10분 휴식·가벼운 걷기 제안 타이머 | [확인] |
| MUSIC | 5.8 태그 + 사용자 선호로 플레이리스트 1–3개 → 선택 시 연동 서비스 열기 | [열기] [닫기] |

`VehicleGateway.apply()`는 사용자가 [적용]을 누른 뒤에만 호출한다. 적용 전 현재 값을 저장해 [되돌리기]를 제공한다. 실패 시 “이 차량에서는 지원되지 않는 기능이에요”를 보여주고 해당 케어를 이번 세션에서 제외한다.

### 7.8 화면 문구 (차량 화면, 숫자·증상명·구간명 금지)

| 케어 | 메인 카드 문구 |
|---|---|
| COMFORT | 따뜻한 시트를 준비했어요 / 시원한 통풍을 준비했어요 |
| QUIET | 오늘은 조용한 차량 환경을 추천해요 |
| RESET | 잠시 멈춘 지금, 3분만 쉬어가요 |
| REPLAY | 지난번 편안했던 설정을 다시 준비했어요 |
| PLACE | 근처에 잠시 들를 만한 곳이 있어요 |
| REST | 안전한 곳에 정차해 쉬어가세요 |
| MUSIC | 지금 기분에 어울리는 플레이리스트가 있어요 |

### 7.9 `collectFeedback(session)`
- 사용자가 실제로 적용·실행한 케어에 대해서만, 다음 PARKED 진입 시(또는 주행 종료 시) 1회 묻는다. 응답 안 하면 평가 없음(점수 영향 없음).
- 같은 세션에 두 번 묻지 않는다.

---

## 8. 함수 명세 — SOS Location Finder V2.1

### 8.1 흐름

```text
SOS 버튼 → 카테고리 선택(TOILET / STORE)
→ 위치 확보(30초 이내·오차 50m 이내 목표, 아니면 1회 재측정, 그래도 안 되면 경고 표시하고 진행)
→ 반경 1→3→5→10km 순 검색, 제외 조건 통과 후보가 3곳 이상이면 확장 중단 (여유 후보 최대 10곳까지 수집 허용)
→ 중복 제거(이름 정규화 + 50m 이내 좌표)
→ 후보별 ETA 계산 (t_drive + t_park + t_walk)
→ 예상 도착 시각 기준 개방 상태 판정
→ 제외 조건 적용 → 점수 → 정렬 → 상위 3곳 표시
→ 사용자 선택 시 NavigationLauncher로 목적지(또는 경유지) 전송
```

### 8.2 함수

```kotlin
fun timeScore(etaMin: Double): Double = max(0.0, 100.0 - 5.0 * max(0.0, etaMin))
fun openScore(s: OpenStatus): Double?          // OPEN 100 / UNKNOWN 30 / CLOSED·PERMANENTLY_CLOSED null
fun accessScore(s: AccessStatus): Double?      // PUBLIC 100 / CONDITIONAL 60 / UNKNOWN 30 / BLOCKED null
fun parkingScore(s: ParkingStatus): Double?    // DEDICATED 100 / NEARBY 70 / UNKNOWN 40 / UNSAFE null
fun reliabilityScore(src: SourceInfo, now: Instant): Double   // 5.7 R 표
fun openStatusAt(hours: OpeningHours?, arrival: Instant): OpenStatus   // 도착 "예정 시각" 기준
fun totalEta(drive: Double?, park: Double?, walk: Double?, policy: EtaPolicy): EtaResult  // 13장 D10

fun scoreCandidate(c: SosCandidate): Double? {
    val eta = c.etaMinutes ?: return null                 // 미확인 ETA는 0분으로 대체 금지
    val t = timeScore(eta)
    val o = openScore(c.openStatus) ?: return null
    if (c.parkingStatus == UNSAFE) return null            // 두 카테고리 모두 적용 (13장 D11)
    val r = c.reliability.coerceIn(0.0, 100.0)
    return when (c.category) {
        TOILET -> { val a = accessScore(c.accessStatus) ?: return null
                    round2(0.50*t + 0.30*o + 0.15*a + 0.05*r) }
        STORE  -> { val p = parkingScore(c.parkingStatus)!!
                    round2(0.50*t + 0.30*p + 0.15*o + 0.05*r) }
    }
}

fun rankCandidates(cs: List<SosCandidate>): List<Pair<SosCandidate, Double>> =
    cs.mapNotNull { c -> scoreCandidate(c)?.let { c to it } }
      .sortedWith(compareByDescending<Pair<SosCandidate, Double>> { it.second }
          .thenBy { it.first.etaMinutes }
          .thenByDescending { it.first.lastVerifiedAt })   // 정보 최신성
      .take(3)
```

### 8.3 표시 규칙
- 각 행: 장소명, 예상 소요 시간(분, 올림)과 계산 범위(“차량 6분 + 도보 약 2분”), 개방 정보, 주차/접근 정보, 정보 확인 상태.
- 개방: OPEN → “개방시간상 이용 가능”, UNKNOWN → “운영시간 미확인”. “확실히 열려 있음”, “지금 열려 있어요” 금지.
- ETA 일부 미확인이면 “차량 이동 약 N분 · 도보 시간 미확인”처럼 구간을 나눠 표시.
- 편의점에는 항상 “생리용품 재고는 확인되지 않아요” 보조 문구.
- 주차 P는 공간 존재 여부일 뿐 빈자리 보장이 아니다(문구로 보장 표현 금지).
- 적격 후보가 3개 미만이면 있는 만큼만 보여주고, 0개면 “근처에서 찾지 못했어요” + [다시 검색] [반대 카테고리 검색].
- 네트워크/GPS 오류는 원인을 명시하고 [다시 시도]를 준다.
- 주행 중: 카테고리 2버튼 + 최대 3행 + 행 탭 = 내비 전송. 그 외 상세 정보 펼치기는 정차 중에만.
- 위험한 정차를 유도하는 문구 금지(“지금 바로 세우세요” 등).

### 8.4 데이터 소스 후보 (11장에서 확정)
공중화장실: 공공데이터포털 “전국공중화장실표준데이터”(개방시간·위치 포함, 갱신일 확인). 편의점·주차: 지도 사업자 장소 API. 경로 ETA: Pleos 내비 또는 지도 사업자 경로 API.

---

## 9. 상황 트리거 카드 (F8)

| ID | 조건 | 카드 | 시점 |
|---|---|---|---|
| TR1 출발 전 케어 | PARKED + 시동 직후 + consents.proactiveCards + (오늘 관련 증상 기록 또는 C≥60인 케어 있음) | Mood Care를 현재 기분 없이 M03·G03으로 미리 계산하지 않는다. **홈에서 기분 선택을 유도**하는 카드 “오늘의 케어를 준비할까요?” | 출발 전 |
| TR2 장거리 휴식 | DRIVING + 연속 주행 ≥ LONG_DRIVE_MIN(90/60분) | “쉬어갈 곳을 경로에 추가할까요?” → 화장실 SOS 결과 1곳 또는 휴게소. (P1이고 HEAVY_FLOW 패턴/초기 신호 있으면 화장실 우선) | 허용된 알림 방식 |
| TR3 Ready Kit | 예정 범위 시작 2일 전, 하루 1회 | “준비물을 확인해 보세요” 체크리스트 | PARKED 중에만 |
| TR4 피드백 | 적용한 케어가 있고 PARKED 진입 | 7.9 | PARKED |

**빈도 제한(13장 D12):** 카드 [나중에]를 누르면 같은 ID 카드는 24시간 동안 다시 띄우지 않는다. 한 주행에서 TR1·TR2 합쳐 최대 2장. 사용자가 끌 수 있다. 카드 문구에 증상명·구간명 금지.

---

## 10. 개인정보·안전 규칙

### 10.1 개인정보
1. 생리·증상·임신 관련 정보는 민감정보로 취급한다. 기본값 비공개, 로컬 암호화 저장, 서버 전송 없음(MVP).
2. 동의는 항목별(Consents)로 받고 언제든 철회 가능. 철회 즉시 관련 점수는 기본값(C=50, P=base)으로, 관련 케어는 D 게이트에서 제외.
3. 차량 프로필(운전자)이 여러 명이면 기록을 프로필별로 분리한다. 프로필 전환 시 이전 사용자 데이터 노출 금지.
4. 전체 데이터 내보내기(JSON)와 전체 삭제를 설정에 둔다.
5. 로그에 증상·날짜 등 민감 값을 남기지 않는다(디버그 빌드 포함).

### 10.2 운전 안전
- 3.1 표의 주행 중 제한을 지킨다. UNKNOWN 상태는 DRIVING으로 본다.
- 주행 중 상호작용은 1탭 선택 중심, 텍스트 입력 없음, 스크롤 목록 없음(최대 3행).
- REST 안내는 정차를 강요하지 않고 “안전한 곳에서”를 붙인다.
- Android for Cars 앱 품질 가이드와 Pleos 정책이 이 문서보다 우선한다.

### 10.3 의료 표현
- 주기·배란은 예상치이며 오차 가능성을 표시한다. 피임 도구로 안내하지 않는다.
- 열선·음악·공조가 통증을 줄인다는 표현 금지. “편안함”, “준비했어요” 수준으로만.
- 반복되거나 심한 통증 기록이 이어지면(예: 강도 3이 3주기 연속) 정차 중 화면에서 전문의 상담 권장 문구를 1회 보여준다(13장 D13).

---

## 11. Pleos API 가정과 착수 전 검증 목록

Pleos에는 공식 개발 문서(pleos.ai의 **Connect SDK** API 레퍼런스, Vehicle 모듈 포함)가 있다. 이 명세 작성 시 문서 본문은 확인하지 못했으므로, 아래 항목은 **가정**이다. 에이전트는 실제 SDK 문서를 받으면 이 표를 먼저 채우고 `PleosVehicleGateway`를 구현한다.

| # | 확인할 것 | 쓰는 곳 | 안 되면 |
|---|---|---|---|
| A1 | 시트 열선·통풍 읽기/쓰기 API와 권한, 차종별 지원 | COMFORT, REPLAY | 시연 모드 시뮬레이션, 실제 차량에선 COMFORT 제외 |
| A2 | 실내 온도·팬 단계 제어 | COMFORT, QUIET | 동일 |
| A3 | 앰비언트 조명 제어 | QUIET, RESET | QUIET은 팬만으로 판정 |
| A4 | 시트 위치·요추 프로필 저장/호출, 주행 중 제한 | REPLAY(Body Fit) | 항목 제외 |
| A5 | 기어·주차 브레이크·속도 등 주행 상태 신호, UX 제한 신호 | 전체 안전 게이트 | UNKNOWN=DRIVING |
| A6 | 다른 앱 위 카드/알림 표시 방식 | 트리거 카드 | 앱 내부 표시만 |
| A7 | 내비 목적지·경유지 전달(인텐트/SDK) | PLACE, SOS, TR2 | 좌표 표시 + “내비에서 검색” 안내 |
| A8 | GPS 위치 권한, 정확도·타임스탬프 | PLACE, SOS | PLACE·SOS 불가 표시 |
| A9 | 장소 검색·경로 ETA API(어느 지도 사업자) | SOS | Mock 데이터로 시연 |
| A10 | 음악 서비스 연동(플레이리스트 열기/재생) | MUSIC | 추천 태그만 표시 |
| A11 | TTS 사용 가능 범위 | REST 안내 | 텍스트만 |
| A12 | 백그라운드 실행·시동 이벤트 수신 | TR1, TR2 | 앱 실행 시에만 판정 |
| A13 | 공모전 제출 형식(에뮬레이터/실차, APK 요구사항) | 전체 | — |

참고 링크: Pleos 앱 개발 챌린지 <https://appdevchallenge.auto-hmg.io/>, Pleos 개발 문서 <https://pleos.ai/playground/develop>, Android for Cars 품질 가이드 <https://developer.android.com/docs/quality-guidelines/car-app-quality>

AAOS 표준 대체 경로(SDK에 없을 때 검토): `CarPropertyManager`의 `HVAC_SEAT_TEMPERATURE`, `HVAC_SEAT_VENTILATION`, `HVAC_TEMPERATURE_SET`, `HVAC_FAN_SPEED`, `GEAR_SELECTION`, `PARKING_BRAKE_ON`, `PERF_VEHICLE_SPEED`와 `CarUxRestrictionsManager`. 단 공조 제어 권한은 보통 시스템 앱 전용이라 서드파티 앱에 허용되지 않을 가능성이 높다.

---

## 12. 테스트 케이스 (골든 값, 반드시 재현)

### 12.1 Mood Care 기본 조건 18개 (P=50, C=50, D=100, 모든 후보 통과, 피로 규칙 제외한 점수 순위)

`S = 0.3M + 0.3G + 25`. 동점은 CareId 순서.

| # | 기분 | 목표 | 1위 | 2위 | 3위 | 기대 화면 |
|---|---|---|---|---|---|---|
| 1 | M01 | G01 | MUSIC 77.5 | COMFORT 76.0 | QUIET 74.5 | 메인 MUSIC + COMFORT, QUIET |
| 2 | M01 | G02 | MUSIC 80.5 | PLACE 79.0 | COMFORT 65.5 | 메인 MUSIC + PLACE |
| 3 | M01 | G03 | REPLAY 80.5 | MUSIC 79.0 | COMFORT 74.5 | 메인 REPLAY + MUSIC |
| 4 | M02 | G01 | QUIET 79.0 | MUSIC 76.0 | RESET 71.5 | 메인 QUIET + MUSIC |
| 5 | M02 | G02 | MUSIC 79.0 | PLACE 71.5 | REPLAY 68.5 | MUSIC 단독 |
| 6 | M02 | G03 | REPLAY 83.5 | MUSIC 77.5 | QUIET 71.5 | REPLAY 단독 |
| 7 | M03 | G01 | MUSIC 76.0 | QUIET 74.5 | COMFORT 73.0 | 메인 MUSIC + QUIET, COMFORT |
| 8 | M03 | G02 | MUSIC 79.0 | PLACE 77.5 | REPLAY 67.0 | 메인 MUSIC + PLACE |
| 9 | M03 | G03 | REPLAY 82.0 | MUSIC 77.5 | COMFORT 71.5 | 메인 REPLAY + MUSIC |
| 10 | M04 | G01 | REST 80.5 | RESET 74.5 | COMFORT 70.0 | **SafetyFirst REST** |
| 11 | M04 | G02 | RESET 70.0 | REST 67.0 | PLACE 65.5 | **SafetyFirst REST** |
| 12 | M04 | G03 | REST 70.0 | COMFORT 68.5 | REPLAY 65.5 | **SafetyFirst REST** |
| 13 | M05 | G01 | RESET 82.0 | QUIET 76.0 | MUSIC 76.0 | RESET 단독 (차이 6.0) |
| 14 | M05 | G02 | PLACE 79.0 | MUSIC 79.0 | RESET 77.5 | 메인 PLACE + MUSIC, RESET |
| 15 | M05 | G03 | MUSIC 77.5 | COMFORT 70.0 | QUIET 68.5 | MUSIC 단독 |
| 16 | M06 | G01 | QUIET 83.5 | RESET 80.5 | COMFORT 78.4 | 메인 QUIET + RESET |
| 17 | M06 | G02 | MUSIC 77.5 | RESET 76.0 | PLACE 71.5 | 메인 MUSIC + RESET |
| 18 | M06 | G03 | COMFORT 76.9 | QUIET 76.0 | MUSIC 76.0 | 메인 COMFORT + QUIET, MUSIC |

주: M04에서는 MUSIC이 표시 대상이 아니므로(7.2) M04 테스트는 MUSIC을 후보에서 제외한 상태에서 점수를 확인한다(1–3위에는 영향 없음).

### 12.2 Mood Care 개별 시나리오

| # | 조건 | 기대 |
|---|---|---|
| MC1 | M06·G01, QUIET: P=80(긍정 3), C=90(오늘 감각 불편 기록) | S(QUIET)=**93.5** |
| MC2 | M05·G01, DRIVING (RESET 제외) | QUIET 76.0 메인(동점 MUSIC보다 ID 앞), 선택지 MUSIC 76.0, COMFORT 71.5 |
| MC3 | M04·G02, DRIVING | RESET 제외, main=REST, 안내 문구 표시, MUSIC·PLACE 선택지 없음 |
| MC4 | M03·G03, 오늘 SLEEPY 기록 | 피로 규칙 발동 → main=REST |
| MC5 | M01·G01, DRIVING, 차량 제어 미지원, 음악 미지원, 위치·내비 지원 | 후보 PLACE 59.5, REST 53.5 → **GeneralChoices** |
| MC6 | M02·G02, 위치 동의 없음 | PLACE 제외 → MUSIC 79.0, REPLAY 68.5 → MUSIC 단독 |
| MC7 | M04·G03 경계 | REST 70.0 (≥70 경계 포함) |
| MC8 | 목표 변경 M03: G03 → G02 | 재계산되어 메인 REPLAY → MUSIC |
| MC8b | 목표 선택지 | M01: G03만 / M02: G02, G03 / M06: G01만 / 그 외: 3개 모두 |
| MC9 | P 계산: 긍정 1·부정 2 / 긍정 5 / 부정 4 | 30 / 100 / 0 |
| MC10 | REPLAY 적용 이력 0건 | REPLAY 제외 |
| MC11 | 건강 개인화 동의 철회 | 모든 C=50 |
| MC12 | 사용자가 [적용]을 누르기 전 | VehicleGateway.apply 호출 0회 |

### 12.3 주기 예측·C 점수 (사용자 A, 주기 알고리즘 9장)

입력: 주기 30,28,31,29,32,30 / 생리 기간 5 / 마지막 시작일 2026-11-02 / D1–D2 복부 불편 3/3 + 열선 긍정 3회(COMFORT P=80) / D1 생리량 많음 2/3 / D−3~D−1 예민함·두통 2/3.

| # | 기대 |
|---|---|
| CY1 | μ=30, w=2, E=2026-12-02, 범위 11/30~12/04, O=11/18, irregular=false, stage=3 |
| CY2 | 구간: P1 11/02–11/06, P2 11/07–11/15, P3 11/16–11/19, P4 11/20–11/25, P5 11/26–12/01 |
| CY3 | Ready Kit 알림 11/28 |
| CY4 | 11/03(D2), M03·G03, 오늘 복부 불편 기록: REPLAY 82.0, COMFORT 81.5(C=90), MUSIC 77.5 → 메인 REPLAY + COMFORT |
| CY5 | 위와 같되 기록 없음(패턴만): COMFORT C=65 → 79.0, 메인 REPLAY + COMFORT |
| CY6 | 11/29(D−3), M06·G01, QUIET C=65: QUIET 85.0, RESET 80.5, COMFORT 78.4. PARKED → 메인 QUIET + RESET. DRIVING → QUIET 단독(차이 6.6) |
| CY7 | 11/10(P2), M01·G02: 모든 C=50 → MUSIC 80.5 |
| CY8 | 사용자 B: 기록 0, Q5 생리 중 복부 불편, Q6 추위, D1, M03·G03, REPLAY 제외. 초기 질문 건너뜀 → COMFORT 71.5, MUSIC 77.5 → MUSIC 단독. 응답함 → COMFORT 74.5(P60/C60) → MUSIC 메인 + COMFORT. 오늘 복부 기록 → COMFORT 77.5(C90) → **COMFORT 메인**(동점, C01<C07) |
| CY9 | 기록 n=1(L=31), Q2 25–28일(μ₀=27, k=2): μ = round((31+54)/3)=28 |
| CY10 | L에 14일, 61일 포함 → 제외되고 확인 요청 |
| CY11 | 주기 24,36,28 (n=3, 차이 12) → irregular, w ≥ 5, P3·P4 표시 안 함, P5에 65·60 미적용 |
| CY12 | n=3 이상인데 Q5 응답 있음 → C=60 나오지 않음 |
| CY13 | Q8 HORMONAL_PILL → ovulationEstimate=null / PREGNANT → PAUSED |
| CY14 | 오늘이 E 이후, 시작 기록 없음 → OVERDUE, C=50, “기록을 확인해 주세요”만 |

### 12.4 SOS

| # | 입력 | 기대 |
|---|---|---|
| SOS1 | T 표: t=1,2,3,4,5,6,10,15,20,25 | 95,90,85,80,75,70,50,25,0,0 |
| SOS2 | 화장실 A: t=3, OPEN, CONDITIONAL, R=60 | **84.5** |
| SOS3 | 화장실 B: t=5, OPEN, PUBLIC, R=80 | **86.5** → 순위 B, A |
| SOS4 | 편의점 A: t=3, OPEN, 주차 UNKNOWN, R=80 | **73.5** |
| SOS5 | 편의점 B: t=5, OPEN, DEDICATED, R=80 | **86.5** → 순위 B, A |
| SOS6 | CLOSED / BLOCKED / 편의점 UNSAFE / 화장실 UNSAFE | 모두 제외 |
| SOS7 | etaMinutes=null (STRICT) | 제외 |
| SOS8 | 개방 UNKNOWN 화장실 t=3, PUBLIC, R=60 | 0.5·85+0.3·30+0.15·100+0.05·60 = **69.5**, 라벨 “운영시간 미확인” |
| SOS9 | 동점 두 곳, ETA 4분 vs 6분 | 4분이 먼저 |
| SOS10 | 1km 내 적격 2곳 | 3km로 확장 |
| SOS11 | 후보 5곳 | 상위 3곳만 반환 |
| SOS12 | 위치 40초 전 측정 | 재측정 1회 시도 |

### 12.5 안전·UI

| # | 기대 |
|---|---|
| UI1 | DRIVING에서 S0, S5–S8, S10 진입 차단 |
| UI2 | PARKED→DRIVING 전환 시 열린 S8은 닫히고 입력은 임시 저장 |
| UI3 | 차량 화면 전체 문자열에 “생리”, “배란”, 증상명, 점수 숫자가 없음 (문자열 리소스 검사 테스트) |
| UI4 | DrivingState.UNKNOWN에서 RESET 제외 |
| UI5 | 트리거 카드 [나중에] 후 24시간 내 같은 카드 미노출 |

---

## 13. 원본 문서 간 불일치와 결정 사항

에이전트는 “기본 채택안”대로 구현한다. 사람이 나중에 바꿀 수 있도록 상수/플래그로 둔다.

| ID | 불일치·빈칸 | 근거 | 기본 채택안 |
|---|---|---|---|
| D1 | 서비스명 HERA(UX 기획서) vs 온미 \| ONME(알고리즘·주장) | 문서별 상이 | **온미 (ONME)** — 2026-11-05 확정 |
| D2 | “추가 선택 최대 2개”와 “상위 2개 차이 5점 미만이면 2번째 제공”이 3번째 후보 처리에서 모호. 의사코드는 `ranked[:3]` | Mood 1장 7, 5.1-3, 8장 | 1위와 차이 5 미만인 2·3위만 선택지(최대 2개) |
| D3 | M04의 초기 목표가 “REST”(목표 ID가 아님) → G 값을 정할 수 없음 | Mood 2장 | 계산용 기본 목표 G01, 메인은 안전 규칙으로 REST 고정 |
| D4 | 피로 시 REST 외 선택지에 무엇을 보여줄지 미정. 플레이리스트 표는 M04 “휴식 우선” | Mood 5.1-1, 7장 | REST 메인 + MUSIC·PLACE 제외한 상위 2개 |
| D5 | QUIET의 “온미 안내 최소화”는 항상 가능하지만 차량 기능 없으면 의미가 약함 | Mood 3장 | 팬 또는 조명 제어 지원 시에만 후보 |
| D6 | 복부 불편→COMFORT 90점에 Mood 문서는 “온열 선호” 조건, 주기 문서 7.2는 조건 없음 | Mood 4.5 vs 주기 7.2 | 온열 선호 조건 유지(5.5의 정의 사용) |
| D7 | Q6 응답 시 COMFORT P=60이 피드백 공식(기본 50)과 충돌 | 주기 3.4 vs Mood 4.4 | 평가가 0건일 때만 60, 평가가 생기면 50 기준 공식 |
| D8 | 예정일이 지나도 시작 기록이 없을 때 의사코드가 P5(오프셋 0 이상)로 처리됨 | 주기 11장 | OVERDUE 구간 신설, C=50 |
| D9 | PLACE가 “목표에 맞는 주변 장소”인데 목표별 장소 유형 정의 없음 | Mood 3장 | 가정: G01 공원·조용한 카페, G02 카페·전망 좋은 곳, G03 자주 간 장소. HEAVY_FLOW 관련이면 화장실 SOS 결과. 확정 필요 |
| D10 | SOS: 주차·도보 시간 미확인 시 “0분 금지, 구간 분리 표시 또는 미확인 처리”인데 의사코드는 무조건 제외. 대부분 장소는 도보 시간이 없어 결과가 비게 됨 | SOS 3.1 vs 7장 | `ETA_POLICY=ESTIMATE` 기본: t_park 미확인=1분, t_walk=직선거리×1.3÷67m/분, 화면에 “추정” 표시. STRICT로 전환 가능. 확정 필요 |
| D11 | 화장실 점수식에 주차가 없어 차량 접근 불가 화장실이 걸러지지 않음 | SOS 2.2 “안전 접근 경로 없음 제외” | 화장실도 주차 UNSAFE면 제외 |
| D12 | “거절한 추천 반복 노출 금지”의 기간·횟수 없음 | UX 8.1 | 같은 카드 24시간, 주행당 최대 2장 |
| D13 | “반복·심한 통증 시 전문의 상담 권장”의 기준 없음 | UX 13.3 | 강도 3이 3주기 연속이면 정차 중 1회 |
| D14 | RESET 길이: Mood 문서 2–3분, 주장 PDF·주기 문서 Relax Mode 5분 | Mood 3장, PDF 7번 | RESET 기본 3분, 5분 옵션. 둘을 같은 기능으로 통합 |
| D15 | REST의 정차 후 내용: PDF는 10분 걷기 휴식 근거 | PDF 6번 | 정차 중 REST = 10분 휴식·걷기 제안 |
| D16 | Mood 4.2의 COMFORT·M06 값 93만 5 단위가 아님 → 76.9, 78.4 같은 소수점 결과 | Mood 4.2 | **값 93 그대로 사용**(76.9는 0.3×93+0.3×80+25로 계산이 맞음). 의도한 값(90 또는 95)인지 확인 필요 |
| D17 | Mood 4.5의 65점 규칙(“동일 주기 구간”)과 주기 문서의 정교화(정렬일 ±1일, P1·P5만, 3주기 이상) | 두 문서 | 주기 문서 규칙 채택 |
| D18 | 초기 질문은 “모바일에서”라고 했지만 MVP에 모바일 앱이 없음 | 주기 3.2 | 차량 앱의 정차 전용 화면으로 구현, 동반 앱은 이후 |
| D19 | 홈의 기분 선택이 주행 중 허용되는지 미확정 | Mood 4.6 | 1탭 선택은 허용, 플랫폼 정책 확인 필요(A5·A6) |
| D20 | SOS 정렬 3순위 “정보 최신성”이 의사코드에 없음 | SOS 6장-3 | lastVerifiedAt 내림차순 추가 |

---

## 14. 개발 순서 (권장 마일스톤)

1. **M1 core:** `OnmeConfig`, 모델, Mood Care(7장), 12.1–12.2 테스트 통과
2. **M2 cycle:** 주기 예측·phaseOf·C 점수(6장), 12.3 테스트 통과
3. **M3 sos-core:** SOS 점수·정렬(8.2), 12.4 테스트 통과
4. **M4 data:** Room + 암호화, 기록·피드백·동의 저장, P 점수 연결
5. **M5 app:** 홈 → 목표 → 추천 → 실행 확인 → 피드백, Mock 차량, 주행 제한(3장), 12.5 테스트
6. **M6 calendar & onboarding:** S0, S7, S8
7. **M7 SOS UI:** Mock 장소·경로 → 실제 API(11장 A7–A9)
8. **M8 Pleos 연동:** 11장 검증 결과로 `PleosVehicleGateway` 구현, 시연 모드 마감
9. **M9 시연 시나리오:** 출발 전 1회 추천 → 승인 → 차량 변화 → 방해 없는 주행 (UX 10장), 사용자 A 데이터로 리허설

---

**발표용 핵심 정의:** “온미는 사용자가 선택한 감정과 케어 목표를 수치화하고, 생리 주기·개인 선호·차량 상황을 종합하여 실행 가능한 맞춤 케어를 우선 추천합니다.”
