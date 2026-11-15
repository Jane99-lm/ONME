package com.onme.app

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import com.onme.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class PleosVehicleGateway(context: Context) : VehicleGateway {
    private val vehicle = Vehicle(context.applicationContext)
    private val seat by lazy { vehicle.getSeat() }
    private val hvac by lazy { vehicle.getHVAC() }
    private val light by lazy { vehicle.getLight() }
    private val driver = SeatArea(SeatValue.ROW_1_LEFT)

    private class Feature<T>(var area: SeatArea? = null, var min: T? = null, var max: T? = null)
    private val heat = Feature<Int>()
    private val vent = Feature<Int>()
    private val temp = Feature<Float>()
    private val fan = Feature<Int>()
    private val ambient = Feature<Int>()
    private var brightBefore: Int? = null
    @Volatile private var powerAreas: List<SeatArea> = emptyList()

    val caps = MutableStateFlow(VehicleCapabilities(false, false, false, false, false, false, false, false))
    val cabin = MutableStateFlow(CabinState())
    @Volatile var skipped: List<String> = emptyList(); private set
    val controls get() = caps.value.let { it.seatHeat || it.seatVent || it.hvacTemp || it.hvacFan || it.ambientLight }

    init {
        runCatching { vehicle.initialize(); probe() }.onFailure { Log.w(TAG, "Vehicle SDK unavailable: ${it.javaClass.simpleName}") }
    }

    private fun writable(c: CapabilityConfig) = c.accessType == AccessType.WRITE || c.accessType == AccessType.READ_WRITE

    private fun <T : Comparable<T>> pick(f: Feature<T>, c: CapabilityConfig, map: Map<SeatArea, AreaBounds<T>>, set: (VehicleCapabilities) -> VehicleCapabilities) {
        if (!writable(c) || map.isEmpty()) return
        val (area, b) = map.entries.firstOrNull { it.key == driver }?.toPair() ?: map.entries.first().toPair()
        f.area = area; f.min = b.minValue; f.max = b.maxValue
        caps.value = set(caps.value)
    }

    private fun probe() {
        val fail: (Exception) -> Unit = { Log.i(TAG, "capability: ${it.message}") }
        seat.checkSeatTemperatureCapabilities({ c, m -> pick(heat, c, m) { it.copy(seatHeat = true) } }, fail)
        seat.checkSeatVentilationCapabilities({ c, m -> pick(vent, c, m) { it.copy(seatVent = true) } }, fail)
        hvac.checkHvacTemperatureCapabilities({ c: CapabilityConfig, m: Map<SeatArea, AreaBounds<Float>> -> pick(temp, c, m) { it.copy(hvacTemp = true) } }, fail)
        hvac.checkHvacFanSpeedCapabilities({ c: CapabilityConfig, m: Map<SeatArea, AreaBounds<Int>> -> pick(fan, c, m) { it.copy(hvacFan = true) } }, fail)
        light.checkAmbientLightBrightnessCapabilities({ c, m -> pick(ambient, c, m) { it.copy(ambientLight = true) } }, fail)
        hvac.checkHvacPowerCapabilities({ c: CapabilityConfig, areas: List<SeatArea> -> if (writable(c) || c.accessType == AccessType.READ) powerAreas = areas }, fail)
    }

    override fun capabilities() = caps.value

    private suspend fun <T> ask(call: ((T?) -> Unit, (Exception) -> Unit) -> Unit): T? = withTimeoutOrNull(3_000) {
        suspendCancellableCoroutine { k ->
            runCatching { call({ if (k.isActive) k.resume(it) }, { e -> Log.i(TAG, "get: ${e.message}"); if (k.isActive) k.resume(null) }) }
                .onFailure { if (k.isActive) k.resume(null) }
        }
    }

    private suspend fun write(call: ((Any?, Any?) -> Unit, (Exception) -> Unit) -> Unit): Boolean = withTimeoutOrNull(3_000) {
        suspendCancellableCoroutine { k ->
            runCatching { call({ _, _ -> if (k.isActive) k.resume(true) }, { e -> Log.w(TAG, "set: ${e.message}"); if (k.isActive) k.resume(false) }) }
                .onFailure { if (k.isActive) k.resume(false) }
        }
    } ?: false

    override suspend fun readCabin(): CabinState {
        val c = cabin.value
        val h = heat.area?.let { a -> ask<Int> { ok, no -> seat.getHvacSeatTemperature(a, { ok(it) }, no) } }
        val v = vent.area?.let { a -> ask<Int> { ok, no -> seat.getHvacSeatVentilationState(a, { ok(it) }, no) } }
        val t = temp.area?.let { a -> ask<Float> { ok, no -> hvac.getHvacTemperature(a, TemperatureUnit.CELSIUS, { ok(it) }, no) } }
        val f = fan.area?.let { a -> ask<Int> { ok, no -> hvac.getHvacFanSpeed(a, { ok(it) }, no) } }
        val b = ambient.area?.let { a -> ask<Int> { ok, no -> light.getAmbientLightBrightness(a, { ok(it) }, no) } }
        if (b != null && !c.ambientDim) brightBefore = b
        return c.copy(
            seatHeatLevel = h ?: c.seatHeatLevel, seatVentLevel = v ?: c.seatVentLevel,
            cabinTempC = t?.toDouble() ?: c.cabinTempC, fanLevel = f ?: c.fanLevel,
        ).also { cabin.value = it }
    }

    override suspend fun apply(settings: VehicleSettings): ApplyResult {
        val s = supportedPart(settings, caps.value, DrivingState.PARKED) ?: return ApplyResult(false, null)
        fun Int.fit(f: Feature<Int>) = coerceIn(f.min ?: this, f.max ?: this)
        val areas = powerAreas
        var power = areas.firstNotNullOfOrNull { a -> ask<Boolean> { ok, no -> hvac.getHvacPowerState(a, { ok(it) }, no) } } ?: true
        if (!power) {
            areas.forEach { a -> write { done, no -> hvac.setHvacPower(a, true, { x, y -> done(x, y) }, no) } }
            kotlinx.coroutines.delay(400)
            power = areas.firstNotNullOfOrNull { a -> ask<Boolean> { ok, no -> hvac.getHvacPowerState(a, { ok(it) }, no) } } ?: false
            Log.i(TAG, "climate power on: $power")
        }
        val skip = mutableListOf<String>()
        var any = false
        var ok = true
        s.seatHeatLevel?.let { v -> heat.area?.let { a -> ok = write { done, no -> seat.setHvacSeatTemperature(a, v.fit(heat), { x, y -> done(x, y) }, no) } && ok } }
        s.seatVentLevel?.let { v -> vent.area?.let { a -> ok = write { done, no -> seat.setHvacSeatVentilation(a, v.fit(vent), { x, y -> done(x, y) }, no) } && ok } }
        if (!power && (s.cabinTempC != null || s.fanLevel != null)) skip += "공조가 꺼져 있어 온도·바람은 바꾸지 못했어요"
        s.cabinTempC?.takeIf { power }?.let { v -> temp.area?.let { a ->
            val t = v.toFloat().coerceIn(temp.min ?: v.toFloat(), temp.max ?: v.toFloat())
            ok = write { done, no -> hvac.setHvacTemperature(a, t, TemperatureUnit.CELSIUS, { x, y -> done(x, y) }, no) } && ok
        } }
        s.fanLevel?.takeIf { power }?.let { v -> fan.area?.let { a -> ok = write { done, no -> hvac.setHvacFanSpeed(a, v.fit(fan), { x, y -> done(x, y) }, no) } && ok } }
        s.ambientDim?.let { dim -> ambient.area?.let { a ->
            val lo = ambient.min ?: 0; val hi = ambient.max ?: 10
            val target = if (dim) lo + (hi - lo) / 4 else brightBefore ?: hi
            ok = write { done, no -> light.setAmbientLightBrightness(a, target, { x, y -> done(x, y) }, no) } && ok
        } }
        val done = s.copy(cabinTempC = s.cabinTempC.takeIf { power }, fanLevel = s.fanLevel.takeIf { power })
        any = listOf(done.seatHeatLevel, done.seatVentLevel, done.cabinTempC, done.fanLevel, done.ambientDim).any { it != null }
        skipped = skip
        if (!ok || !any) return ApplyResult(false, null)
        done.ambientDim?.let { d -> cabin.value = cabin.value.copy(ambientDim = d) }
        kotlinx.coroutines.delay(400)
        val now = readCabin()
        val took = done.copy(
            seatHeatLevel = done.seatHeatLevel?.takeIf { now.seatHeatLevel == it.fit(heat) },
            seatVentLevel = done.seatVentLevel?.takeIf { now.seatVentLevel == it.fit(vent) },
            fanLevel = done.fanLevel?.takeIf { now.fanLevel == it.fit(fan) },
        )
        if (listOf(took.seatHeatLevel, took.seatVentLevel, took.cabinTempC, took.fanLevel, took.ambientDim).all { it == null }) {
            skipped = listOf(NOT_TAKEN); return ApplyResult(false, null)
        }
        if (took != done) skipped = skip + "일부 설정은 차량에서 바뀌지 않았어요"
        return ApplyResult(true, took)
    }

    fun release() = runCatching { vehicle.release() }

    companion object {
        private const val TAG = "PleosVehicle"
        const val NOT_TAKEN = "차량이 설정을 받지 않았어요. 차량의 공조를 켠 뒤 다시 시도해 주세요"
    }
}

class PleosVoice(context: Context) {
    private val tts = ai.pleos.playground.tts.TextToSpeech(context.applicationContext, ai.pleos.playground.tts.constant.Mode.ON_DEVICE)
    @Volatile private var ready = false

    init {
        runCatching {
            tts.initialize(object : ai.pleos.playground.tts.listener.OnInitializeListener {
                override fun onSuccess() { ready = true; runCatching { tts.setLocale(java.util.Locale.KOREA) } }
                override fun onFailure(errorType: ai.pleos.playground.tts.constant.ErrorType) { Log.w("PleosVoice", "init failed: $errorType") }
            })
        }.onFailure { Log.w("PleosVoice", "TTS unavailable: ${it.javaClass.simpleName}") }
    }

    fun say(text: String) { if (ready) runCatching { tts.speak(text, true) } }
    fun stop() { runCatching { tts.stop() } }
    fun release() = runCatching { tts.release() }
}
