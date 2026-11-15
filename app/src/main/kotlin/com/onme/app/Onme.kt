package com.onme.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.onme.core.*
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** App state machine: screens, driving gate, care flow, cards, SOS, demo mode. All rules live in :core. */
@OptIn(ExperimentalCoroutinesApi::class)
class Onme(private val context: Context) {
    private val scope = MainScope()
    private val io = Dispatchers.IO.limitedParallelism(1)
    private var store: SecureStore? = null

    var user by mutableStateOf(UserData()); private set
    fun update(f: (UserData) -> UserData) {
        user = f(user)
        val snapshot = user; val s = store ?: return
        scope.launch(io) { s.save(snapshot) }
    }

    // ---- profiles (loading → profile pick) ----
    val profiles = DemoData.profiles
    var profile by mutableStateOf<Profile?>(null); private set

    /** 10.1-3: nothing from the previous driver stays on screen or in memory. First pick seeds the pre-made profile. */
    fun selectProfile(p: Profile) {
        clearSession()
        val s = SecureStore(context, p.id).also { store = it }
        profile = p
        user = s.load() ?: p.seed(today()).also { seed -> scope.launch(io) { s.save(seed) } }
        screen = if (user.cycleTracking && user.onboarding == null) screenAfter(Screen.ONBOARDING, driving) else Screen.HOME
    }

    fun switchProfile() { clearSession(); profile = null; store = null; user = UserData(); screen = Screen.PROFILE }

    private fun clearSession() {
        mood = null; goal = null; rec = null; action = null; applied = false; playlists = emptyList()
        sessionId = null; pendingFeedback = null; card = null; message = null
        predictionAsk = null; sosCat = null; sosResult = null; sosError = null; excluded = emptySet(); quietDrive = false
    }

    // ---- demo mode (spec 0.2) ----
    var demo by mutableStateOf(false)
    var demoDriving by mutableStateOf(DrivingState.PARKED)
    var demoDate by mutableStateOf<LocalDate?>(null)
    var demoCaps by mutableStateOf(VehicleCapabilities())
    var demoMusic by mutableStateOf(true)
    var demoSos by mutableStateOf("ok")
    val car = MockVehicleGateway()
    private val demoNav = MockNavigationLauncher()
    private val demoLocation = FixedLocation(DemoData.SEOUL_CITY_HALL)
    private val realLocation = AndroidLocation(context)
    private val geoNav = GeoIntentNavigation(context)
    private val pleosNav = PleosNavigation(context)
    private val realNav: NavigationLauncher get() = if (pleosNav.isSupported()) pleosNav else geoNav
    val pleosCar = PleosVehicleGateway(context)
    private val demoRunner = CareRunner(car)
    private val carRunner = CareRunner(pleosCar)
    private var carIgnores by mutableStateOf(false)
    private val emulator = android.os.Build.HARDWARE.let { it.contains("ranchu") || it.contains("goldfish") }
    private val simulated get() = (demo && !pleosCar.controls) || carIgnores
    private val runner get() = if (simulated) demoRunner else carRunner
    val cabin: kotlinx.coroutines.flow.StateFlow<CabinState> get() = if (simulated) car.cabin else pleosCar.cabin
    private val voice = PleosVoice(context)
    var voiceOn by mutableStateOf(true)
    fun say(text: String) = speak(text)
    private fun speak(text: String?) { if (text != null && voiceOn && !quietDrive) voice.say(text) }
    fun refreshCabin() { if (!simulated) scope.launch { pleosCar.readCabin() } }

    /** From CarUxRestrictions; UNKNOWN until the car service answers (treated as driving). */
    var systemDriving by mutableStateOf(DrivingState.UNKNOWN)
    val driving get() = if (demo) demoDriving else systemDriving
    val parked get() = driving == DrivingState.PARKED
    fun today(): LocalDate = demoDate ?: LocalDate.now()

    private val demoHere = object : LocationSource {
        override suspend fun current() = if (realLocation.permitted()) runCatching { realLocation.current() }.getOrElse { demoLocation.current() } else demoLocation.current()
    }
    private val location: LocationSource get() = if (demo) demoHere else realLocation
    private val nav: NavigationLauncher get() = if (pleosNav.isSupported()) pleosNav else if (demo) demoNav else geoNav
    private val music = MockMusicProvider()

    /** No verified Pleos vehicle API yet (A1–A4): outside demo mode the car exposes no controls. */
    fun caps(): VehicleCapabilities = if (simulated) demoCaps else
        pleosCar.capabilities().copy(seatPosition = false, navigation = pleosNav.isSupported() || geoNav.isSupported(), overlayCard = false)

    private fun musicSupported() = demo && demoMusic // A10 not verified: real cars show no MUSIC
    private val locationPermitted get() = demo || realLocation.permitted()
    private var excluded = setOf<CareId>()

    fun ctx() = CareContext(driving, caps(), user.consents.location && locationPermitted, musicSupported(), today(), excluded)

    // ---- navigation ----
    var screen by mutableStateOf(Screen.LOADING)
    var blocked by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    fun go(s: Screen) {
        if (s in CYCLE_SCREENS && !user.cycleTracking) return
        if (canOpen(s, driving)) { screen = s; message = null; if (s == Screen.SOS) sosCat?.let { sos(it) } } else blocked = true
    }

    // ---- care flow ----
    var mood by mutableStateOf<MoodId?>(null); private set
    var goal by mutableStateOf<GoalId?>(null); private set
    var rec by mutableStateOf<Recommendation?>(null); private set
    var action by mutableStateOf<CareAction?>(null); private set
    var applied by mutableStateOf(false); private set
    var playlists by mutableStateOf<List<Playlist>>(emptyList()); private set
    private var sessionId: String? = null
    private var pendingFeedback: String? = null
    var quietDrive = false; private set

    fun pickMood(m: MoodId) { mood = m; goal = defaultGoal(m); recompute(); go(Screen.RESULT) }
    fun pickGoal(g: GoalId) { val m = mood ?: return; if (g !in goalsFor(m)) return; goal = g; recompute() }

    private fun recompute() {
        val m = mood ?: return; val g = goal ?: return
        car.caps = caps()
        val r = recommend(m, g, user, ctx())
        rec = r
        val main = when (r) { is Recommendation.Main -> r.main.care; is Recommendation.SafetyFirst -> r.main.care; else -> null }
        val ph = phaseOn(user, today())
        val s = CareSession(UUID.randomUUID().toString(), Instant.now(), m, g, r.ranked.map { it.care to it.score }, main, null, null, ph?.first, ph?.second)
        sessionId = s.id
        update { it.copy(sessions = it.sessions + s) }
    }

    fun start(care: CareId) {
        choose(care)
        if (care == CareId.COMFORT || care == CareId.QUIET || care == CareId.REPLAY) confirm()
    }

    fun preview(care: CareId) = buildCareAction(care, mood ?: MoodId.M03, goal ?: GoalId.G03, user, ctx(), cabin.value)

    fun choose(care: CareId) {
        action = preview(care); applied = false; playlists = emptyList()
        if (care == CareId.MUSIC) scope.launch { playlists = music.playlistsFor(action?.playlistTag ?: "").take(3) }
        go(Screen.CONFIRM)
    }

    private fun markChosen(care: CareId, settings: VehicleSettings?) {
        val id = sessionId ?: return
        update { u -> u.copy(sessions = u.sessions.map { if (it.id == id) it.copy(chosen = care, applied = settings) else it }) }
        pendingFeedback = id
    }

    /** User tapped the primary button — the only path to VehicleGateway.apply (0.1-4). */
    fun confirm() {
        val a = action ?: return
        when (a.care) {
            CareId.COMFORT, CareId.QUIET, CareId.REPLAY -> scope.launch {
                val s = a.settings
                val r = if (s != null) { car.caps = caps(); runner.approve(s) } else null
                if (r?.ok == true) {
                    applied = true; message = if (!simulated && pleosCar.skipped.isNotEmpty()) pleosCar.skipped.first() else Copy.APPLIED
                    if (a.quietDrive) quietDrive = true
                    markChosen(a.care, r.applied)
                } else if ((demo || emulator) && !simulated && s != null && pleosCar.skipped.firstOrNull() == PleosVehicleGateway.NOT_TAKEN) {
                    carIgnores = true; car.caps = caps()
                    val d = runner.approve(s)
                    applied = d.ok; message = if (d.ok) Copy.DEMO_CAR else Copy.UNSUPPORTED
                    if (d.ok) { if (a.quietDrive) quietDrive = true; markChosen(a.care, d.applied) } else screen = Screen.RESULT
                } else {
                    val why = if (!simulated) pleosCar.skipped.firstOrNull() else null
                    if (why != null) message = why
                    else { message = Copy.UNSUPPORTED; excluded = excluded + a.care; recompute() }
                    screen = Screen.RESULT
                }
            }
            CareId.RESET -> { markChosen(a.care, null); timerMinutes = a.minutes ?: OnmeConfig.RESET_MINUTES; go(Screen.RESET) }
            CareId.REST -> {
                markChosen(a.care, null)
                if (parked) { timerMinutes = a.minutes ?: OnmeConfig.REST_PARKED_MINUTES; go(Screen.RESET) }
                else { message = Copy.REST_DRIVING; screen = Screen.HOME }
            }
            CareId.PLACE -> if (a.sosToilet) { markChosen(a.care, null); sos(SosCategory.TOILET); go(Screen.SOS) }
            CareId.MUSIC -> Unit // a playlist row is picked instead
        }
    }

    fun revert() = scope.launch { runner.revert(); applied = false; message = null }

    /** D9 assumption: no POI provider for place kinds yet, so we hand the kind to navigation as a nearby search. */
    fun sendPlace(kind: String) = scope.launch {
        markChosen(CareId.PLACE, null)
        val here = runCatching { location.current().point }.getOrNull()
        val search = if (demo) demoNav else geoNav
        message = if (here != null && search.isSupported() && search.sendDestination(kind, here.offsetMeters(400.0, 300.0), asWaypoint = true)) { mapSet++; SosCopy.SENT }
        else SosCopy.NAV_FALLBACK
        screen = Screen.HOME
    }

    fun openPlaylist(p: Playlist) = scope.launch {
        markChosen(CareId.MUSIC, null)
        message = if (music.open(p)) "${p.title} · 열었어요" else Copy.UNSUPPORTED
        screen = Screen.HOME
    }

    // ---- RESET / parked REST timer ----
    var timerMinutes by mutableStateOf(OnmeConfig.RESET_MINUTES)
    fun timerDone() = if (pendingFeedback != null) go(Screen.FEEDBACK) else go(Screen.HOME)

    // ---- feedback (7.9) ----
    val feedbackPending get() = pendingFeedback != null
    fun rate(r: Rating?) {
        val id = pendingFeedback ?: return
        pendingFeedback = null // asked once per session
        if (r != null) update { u -> u.copy(sessions = u.sessions.map { if (it.id == id) it.copy(rating = r) else it }) }
        go(Screen.HOME)
    }

    // ---- trigger cards (9) ----
    var card by mutableStateOf<String?>(null); private set
    private var cardsThisDrive = 0
    private var driveStart: Instant? = null

    private fun offer(id: String, ok: Boolean) {
        if (card != null || !ok || quietDrive || !Cards.canShow(user, id, Instant.now(), cardsThisDrive)) return
        card = id
        if (id != Cards.TR3) cardsThisDrive++
    }

    fun evaluateCards() {
        val c = ctx()
        if (c.parked) {
            offer(Cards.TR3, Cards.readyKit(user, c))
            offer(Cards.TR1, driveStart == null && Cards.departure(user, c))
        } else driveStart?.let { offer(Cards.TR2, Cards.longDrive(user, Duration.between(it, Instant.now()).toMinutes())) }
    }

    fun cardLater() { card?.let { id -> update { Cards.snooze(it, id, Instant.now()) } }; card = null }
    fun cardOk() {
        when (card) {
            Cards.TR2 -> { sos(SosCategory.TOILET); go(Screen.SOS) }
            Cards.TR3 -> message = Copy.READY_KIT_ITEMS
            else -> go(Screen.HOME)
        }
        card = null
    }

    fun demoAddDriveMinutes(min: Long) { driveStart = (driveStart ?: Instant.now()).minusSeconds(min * 60); evaluateCards() }

    // ---- SOS (8) ----
    var sosCat by mutableStateOf<SosCategory?>(null); private set
    var sosResult by mutableStateOf<SosResult?>(null); private set
    var sosError by mutableStateOf<String?>(null); private set
    var sosLoading by mutableStateOf(false); private set
    var sosRadius by mutableStateOf<Int?>(null); private set
    private var sosJob: kotlinx.coroutines.Job? = null
    private val places = object : PlaceProvider {
        // ponytail: A9 (map/POI API) not chosen yet — mock places generated around the query point.
        override suspend fun search(cat: SosCategory, center: GeoPoint, radiusM: Int): List<RawPlace> {
            sosRadius = radiusM; kotlinx.coroutines.delay(350)
            return DemoData.places(center).filter { it.category == cat && haversineM(center, it.point) <= radiusM }
        }
    }

    fun sos(cat: SosCategory) {
        sosCat = cat; sosResult = null; sosError = null
        if (!user.consents.location || !locationPermitted || (demo && demoSos == "gps")) { sosError = SosCopy.GPS_ERROR; return }
        if (demo && demoSos == "net") { sosError = SosCopy.NET_ERROR; return }
        sosLoading = true; sosRadius = null; sosJob?.cancel()
        sosJob = scope.launch {
            val loc = try { location.current() } catch (e: Exception) { null }
            if (loc == null) { sosError = SosCopy.GPS_ERROR; sosLoading = false; return@launch }
            val fixed = object : LocationSource { var first = true
                override suspend fun current() = if (first) { first = false; loc } else location.current() }
            sosResult = try { findSos(cat, fixed, places, StraightLineRoutes(), Instant.now()) } catch (e: Exception) { sosError = SosCopy.NET_ERROR; null }
                ?.let { r -> if (!demo) r else when (demoSos) { "none" -> r.copy(results = emptyList()); "weak" -> r.copy(locationWarning = true); else -> r } }
            sosLoading = false
        }
    }

    var mapSet by mutableStateOf(0); private set

    fun cancelSos() { sosJob?.cancel(); sosLoading = false; sosCat = null; sosResult = null }

    fun sendSos(c: SosCandidate) = scope.launch {
        val p = c.place ?: return@launch
        val n = nav
        message = if (n.isSupported() && n.sendDestination(p.name, p.point, asWaypoint = !parked)) { mapSet++; SosCopy.SENT }
        else (n as? PleosNavigation)?.lastError ?: "${SosCopy.NAV_FALLBACK}: ${p.name}"
    }

    // ---- records (S8) — every tap is saved right away, so nothing is lost when driving closes the screen (UI2) ----
    var predictionAsk by mutableStateOf<Pair<LocalDate, LocalDate>?>(null); private set

    fun setSymptom(s: Symptom, level: Int) {
        val day = today()
        update { u -> u.copy(symptoms = u.symptoms.filter { it.date != day || it.symptom != s } + listOfNotNull(SymptomLog(day, s, level).takeIf { level > 0 })) }
    }

    fun startPeriod() {
        val day = today()
        val before = predictCycle(user)
        if (before != null && day > (allStarts(user).lastOrNull() ?: day)) predictionAsk = before.rangeStart to before.rangeEnd // 6.5
        update { u -> u.copy(periods = (u.periods.filter { it.start != day } + PeriodRecord(day)).sortedBy { it.start }) }
    }

    fun endPeriod() {
        val day = today()
        update { u ->
            val last = u.periods.lastOrNull { it.start <= day } ?: return@update u
            u.copy(periods = u.periods.map { if (it == last) it.copy(end = day) else it })
        }
    }

    /** 6.5: "early"/"late" twice in a row → ask Q2/Q3 again. */
    fun answerPrediction(answer: String) {
        predictionAsk = null
        val miss = answer != "맞았어요"
        update { it.copy(firstPredictionAsked = true, predictionFeedback = it.predictionFeedback + answer, predictionMisses = if (miss) it.predictionMisses + 1 else 0) }
        if (user.predictionMisses >= 2) { update { it.copy(predictionMisses = 0) }; go(Screen.ONBOARDING) }
    }

    fun dismissPrediction() { predictionAsk = null; update { it.copy(firstPredictionAsked = true) } }

    fun specialistNoticeShown() = update { it.copy(specialistNoticeShown = true) }

    // ---- onboarding (S0) ----
    fun finishOnboarding(a: OnboardingAnswers, consents: Consents) {
        update { u ->
            u.copy(
                onboarding = a, consents = consents,
                periods = (u.periods.filter { it.start != a.lastStart } + PeriodRecord(a.lastStart)).sortedBy { it.start },
                presets = u.presets.filter { it.createdFrom != "ONBOARDING" } + listOfNotNull(
                    a.preferredSeatHeatLevel?.takeIf { it > 0 }?.let { ComfortPreset("onboarding", "기본 편안함", VehicleSettings(seatHeatLevel = it), "ONBOARDING") },
                ),
            )
        }
        go(Screen.HOME)
    }

    // ---- settings (S10) ----
    fun setConsents(c: Consents) { update { it.copy(consents = c) }; excluded = emptySet() }
    fun setFamilyPlanning(on: Boolean) = update { it.copy(familyPlanning = on) }

    fun exportIntent(): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_TEXT, UserStore.encode(user)), "ONME 데이터 내보내기",
    )

    fun deleteAll() {
        store?.deleteAll(); clearSession()
        user = UserData(cycleTracking = user.cycleTracking)
        screen = if (user.cycleTracking) Screen.ONBOARDING else Screen.HOME
    }

    fun loadUserA() {
        demo = true; if (profile == null) selectProfile(profiles.first()); clearSession(); update { DemoData.userA() }
        demoDate = LocalDate.of(2026, 11, 3); excluded = emptySet(); go(Screen.HOME)
    }

    fun resetDemo() {
        demoDriving = DrivingState.PARKED; demoDate = null; demoCaps = VehicleCapabilities(); demoMusic = true
        car.cabin.value = CabinState(); demoSos = "ok"; excluded = emptySet(); quietDrive = false; driveStart = null; cardsThisDrive = 0
        mood = null; goal = null; rec = null; action = null; card = null; pendingFeedback = null; message = null
        go(Screen.HOME)
    }

    // ---- driving transitions ----
    private fun onDriving(old: DrivingState?, new: DrivingState) {
        if (profile == null) return // still on loading / profile pick
        screen = screenAfter(screen, new)
        if (new == DrivingState.DRIVING && driveStart == null) { driveStart = Instant.now(); cardsThisDrive = 0; card = null }
        if (new == DrivingState.PARKED && old != null && old != DrivingState.PARKED) {
            driveStart = null; quietDrive = false
            if (pendingFeedback != null) go(Screen.FEEDBACK)
        }
        if (rec != null) recompute() // RESET appears/disappears (7.6)
        evaluateCards()
    }

    init {
        scope.launch { delay(LOADING_MS); if (screen == Screen.LOADING) screen = Screen.PROFILE }
        scope.launch { snapshotFlow { message }.collect { speak(it) } }
        scope.launch { snapshotFlow { card }.collect { id -> speak(when (id) { Cards.TR1 -> Copy.TR1; Cards.TR2 -> Copy.TR2; Cards.TR3 -> Copy.TR3; else -> null }) } }
        var last: DrivingState? = null
        scope.launch { snapshotFlow { driving }.collect { onDriving(last, it); last = it } }

    }

    private var cardLoop: Job? = null
    fun onForeground() {
        refreshCabin()
        if (profile != null) evaluateCards()
        cardLoop?.cancel()
        cardLoop = scope.launch { while (isActive) { delay(30_000); if (profile != null) evaluateCards() } }
    }
    fun onBackground() { cardLoop?.cancel(); cardLoop = null }

    fun restore(profileId: String?, screenName: String?) {
        if (profile != null) return
        val p = profiles.firstOrNull { it.id == profileId } ?: return
        selectProfile(p)
        val s = screenName?.let { n -> Screen.entries.firstOrNull { it.name == n } }
        if (s in RESTORABLE) go(s!!)
    }

    private companion object {
        val RESTORABLE = setOf(Screen.HOME, Screen.SOS, Screen.CALENDAR, Screen.RECORD, Screen.SETTINGS)
        const val LOADING_MS = 500L
        val CYCLE_SCREENS = setOf(Screen.ONBOARDING, Screen.CALENDAR, Screen.RECORD)
    }
}
