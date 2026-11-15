# 온미 | ONME

A care app for women drivers on Hyundai **Pleos Connect** (Android Automotive OS). You pick how you feel, and ONME recommends one care the car can actually do right now, such as warming the seat, quieting the cabin, a short rest, or music. It also finds a nearby toilet or convenience store fast when you need one.

Health data never leaves the car. It is stored encrypted, separately for each driver profile.

---

## How it works

### 1. Mood → goal → recommendation (Mood Care)

1. On the home screen the driver taps one of six moods: 좋아요, 평온해요, 보통이에요, 지쳤어요, 울적해요, 예민해요.
2. ONME picks a default goal for that mood (`defaultGoal`): 차분해지기, 기분 전환 or 기분 유지. The driver can switch to another allowed goal (`goalsFor`). Changing the goal recalculates right away.
3. `recommend()` scores the seven cares: COMFORT, QUIET, RESET, REPLAY, PLACE, REST, MUSIC.

**Step A – safety gate first (`isFeasible`).** A care that can't run right now is removed before scoring, so a high score never outweighs safety:

| Care | Shown only when |
|---|---|
| COMFORT | vehicle control is allowed and the car has seat heat/vent or climate control |
| QUIET | vehicle control is allowed and the car has fan or ambient-light control |
| RESET | the car is parked |
| REPLAY | a setting the driver rated 좋았어요 exists and the car supports part of it |
| PLACE | location is allowed and found (and navigation is available while driving) |
| REST | always |
| MUSIC | music is allowed and supported, and the mood isn't 지쳤어요 |

**Step B – score (`score`).** Each remaining care gets 0–100:

```
S = 0.30·M + 0.30·G + 0.20·P + 0.10·C + 0.10·D
```

- **M** – how well the care fits the mood (lookup table)
- **G** – how well it fits the goal (lookup table)
- **P** – personal preference from past ratings (`preferenceScore`): starts at 50, each recent 좋았어요 adds 10, each 별로였어요 subtracts 15, over the last 5 ratings
- **C** – cycle and symptom context (`cycleSymptomScore`), see section 2
- **D** – 100 for every care that passed the gate

The sum is computed in integers (`S×10`) to avoid floating-point rounding errors.

**Step C – pick what to show.**
- **Tired** (`fatigueRisk`: mood 지쳤어요, or sleepiness/fatigue logged today) → the main card is always **REST**, with a safety notice. No other care can override this.
- **Top score ≥ 70** → that care is the main card. Up to two more cares are offered if they are within 5 points of it.
- **Otherwise** → the available cares are shown as equal choices.

The screen never shows scores, cycle phases or symptom names, only action phrases such as "따뜻한 시트를 준비했어요".

### 2. Cycle prediction and context (Cycle)

- `cycleLengths` takes the recorded period start dates and ignores gaps under 15 days or over 60 days.
- `predictCycle` estimates the next start and a ± range:
  - with no history, from the onboarding answers;
  - with 1–2 cycles, a blend of history and answers;
  - with 3 or more cycles, the median length, with the range from the spread.
  - Irregular cycles get a wider range.
- `phaseOf` places a day in one of these phases: P1 (period), P2–P4, P5 (the 6 days before the next start), OVERDUE, or PAUSED (pregnancy or postpartum).
- `cycleSymptomScore` (C) is above 50 only when there is evidence the driver gave:
  - **90** – a related symptom logged today (e.g. a stiff lower back → COMFORT);
  - **65** – the same symptom showed up at the same point in 2 of the last 3 cycles;
  - **60** – fewer than 3 cycles recorded, but onboarding said this phase is uncomfortable;
  - **50** – everything else. ONME never guesses symptoms from the cycle alone.

Predictions are always shown as an expected range, never as fact, and are not for contraception.

### 3. Running a care

- `buildCareAction` turns the chosen care into concrete settings (seat heat level, temperature, fan, lighting, playlist, places).
- Nothing changes in the car until the driver taps **적용**. `CareRunner` saves the current values first so **되돌리기** can restore them.
- Parts the car doesn't support are skipped (`supportedPart`). Seat position only changes while parked.
- After 적용, the confirm screen shows the ONME character holding a glowing jade ball (`BearOrb`). The ball animates only while parked.
- The next time the car is parked, ONME asks once for a rating: 좋았어요 / 보통 / 별로였어요. The answer feeds the P score next time.

### 4. SOS – toilet or convenience store (급할 때)

1. The driver picks 화장실 or 편의점.
2. `findSos` searches 1 → 3 → 5 → 10 km and stops widening once 3 usable places are found. Duplicates are removed (`dedup`).
3. Each place gets an ETA: drive + park + walk (`totalEta`). Unknown parts are estimated and labelled "추정", never counted as 0 minutes.
4. Places that are closed, blocked, or have unsafe parking are removed. The rest are scored (`scoreCandidate`):
   - Toilet: `0.50·time + 0.30·open + 0.15·access + 0.05·reliability`
   - Store: `0.50·time + 0.30·parking + 0.15·open + 0.05·reliability`
5. The top 3 are shown (`rankCandidates`). Tapping one sends it to navigation (`PleosNavigation` → Pleos NaviHelper).

Opening hours are checked against the expected **arrival** time. The wording never promises a place is open or that a parking spot is free.

### 5. Situation cards

| Card | When |
|---|---|
| TR1 – 오늘의 케어를 준비할까요? | before departure, if something relevant was logged today |
| TR2 – 쉬어갈 곳을 경로에 추가할까요? | after a long continuous drive |
| TR3 – 준비물을 확인해 보세요 | 2 days before the expected period range, parked only |

Tapping 나중에 hides that card for 24 hours, and at most 2 cards appear per drive.

### 6. Driving safety

- The driving state comes from the car's UX-restriction signal. **Unknown is treated as driving.**
- While driving, only one-tap screens are available: home, goal, recommendation, confirm and SOS. Onboarding, the record, calendar, rest timer, feedback and settings screens close and say "안전하게 정차한 뒤 이용할 수 있어요". Anything typed so far is saved first.
- ONME never sets a destination or changes the car on its own. The driver always taps first.

### 7. Privacy

- All data stays on the device, AES-GCM encrypted with an Android Keystore key, one file per driver profile (`SecureStore`).
- Each data use has its own consent toggle in 설정. Turning one off immediately resets the related score (C = 50, P = default) and hides the cares that depend on it.
- Logs never contain symptoms or dates. 설정 has full export and full delete.

### 8. Demo mode

Turn it on in 설정. It lets you switch between parked and driving, toggle which car features exist, use a fake location, and load a sample user with 6 cycles of history. Vehicle changes go to a simulated car when the emulator doesn't store them.

---

## Project layout

```
core/   Pure Kotlin, no Android: models, OnmeConfig (all weights and thresholds),
        Mood Care, cycle prediction, SOS scoring, trigger rules, UI copy. Unit-tested on the JVM.
app/    Jetpack Compose UI, Pleos vehicle / navigation / TTS adapters, Keystore storage, demo mode
design/ Web design preview and asset tools
```

All weights and thresholds live in `core/.../OnmeConfig.kt`. They are starting values and will change after testing.

## Build and run

```bash
./gradlew :core:test          # core logic tests
./gradlew :app:installDebug   # install on the Pleos Connect emulator
```

The 3D cycle model (`design/cycle/index.html`) isn't in the repo. Generate it with `design/tools/cycle.sh`.
