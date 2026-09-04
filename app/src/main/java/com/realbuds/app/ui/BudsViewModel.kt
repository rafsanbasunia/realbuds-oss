package com.realbuds.app.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.realbuds.app.adaptive.ActivityMonitor
import com.realbuds.app.adaptive.Motion
import com.realbuds.app.adaptive.AdaptiveController
import com.realbuds.app.adaptive.AdaptiveRules
import com.realbuds.app.proto.BudsError
import com.realbuds.app.proto.AncMode
import com.realbuds.app.proto.BassBand
import com.realbuds.app.proto.BudsClient
import com.realbuds.app.proto.BudsState
import com.realbuds.app.proto.ConnState
import com.realbuds.app.proto.EqPreset
import com.realbuds.app.proto.Feature
import com.realbuds.app.proto.LogLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BudsViewModel : ViewModel() {

    private val client = com.realbuds.app.proto.BudsClientHolder.instance

    private val _devices = MutableStateFlow<List<DeviceEntry>>(emptyList())
    val devices: StateFlow<List<DeviceEntry>> = _devices.asStateFlow()

    private val _logs = MutableStateFlow<List<LogLine>>(emptyList())
    val logs: StateFlow<List<LogLine>> = _logs.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val state: StateFlow<ConnState> get() = client.state
    val buds: StateFlow<BudsState> get() = client.buds

    val connectedName: StateFlow<String?> get() = client.deviceName

    /** Remembered so "Noise cancellation" returns to the level you last used. */
    private var lastLevel: AncMode = AncMode.ANC_SMART

    data class DeviceEntry(val name: String, val mac: String, val device: BluetoothDevice)

    init {
        viewModelScope.launch {
            client.log.collect { line -> _logs.value = (_logs.value + line).takeLast(400) }
        }
    }

    fun lastAncLevel(): AncMode = lastLevel

    fun refreshDevices(adapter: BluetoothAdapter?) {
        if (adapter == null || !adapter.isEnabled) {
            _devices.value = emptyList()
            return
        }
        _devices.value = try {
            client.candidates(adapter).map { DeviceEntry(it.name ?: "(unnamed)", it.address, it) }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun connect(entry: DeviceEntry) {
        viewModelScope.launch { client.connect(entry.device) }
    }

    fun disconnect() {
        _finding.value = false
        client.disconnect()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            client.refreshAll()
            delay(500)
            _refreshing.value = false
        }
    }

    fun setAnc(mode: AncMode) {
        if (mode.group == AncMode.Group.ANC) lastLevel = mode
        adaptive?.onManualOverride()
        viewModelScope.launch { client.setAnc(mode) }
    }

    fun setFeature(feature: Feature, enabled: Boolean) = setFeatureId(feature.id, enabled)

    fun setFeatureId(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            client.setFeature(id, enabled)
            kotlinx.coroutines.delay(400)
            client.featureList(com.realbuds.app.proto.Feature.ids)
        }
    }

    /**
     * Whether the locate beep is running. Held here because the protocol has
     * no status query and sends no push for find mode, so this is the only
     * record of it. Reset on disconnect so a stale "ringing" cannot persist.
     */
    private val _finding = MutableStateFlow(false)
    val finding: StateFlow<Boolean> = _finding.asStateFlow()

    private var adaptive: AdaptiveController? = null

    /** Detected activity, for the UI to show what the rules are reacting to. */
    private val _activity = MutableStateFlow(Motion.UNKNOWN)
    val activity: StateFlow<Motion> = _activity.asStateFlow()

    private val _adaptiveOn = MutableStateFlow(false)
    val adaptiveOn: StateFlow<Boolean> = _adaptiveOn.asStateFlow()

    private val _adaptiveAvailable = MutableStateFlow(true)
    val adaptiveAvailable: StateFlow<Boolean> = _adaptiveAvailable.asStateFlow()

    /**
     * Wires up adaptive ANC. Called once the Activity exists, since the step
     * sensor needs a Context and the ViewModel is constructed without one.
     */
    fun initAdaptive(ctx: Context) {
        if (adaptive != null) return
        val monitor = ActivityMonitor(ctx)
        _adaptiveAvailable.value = monitor.available
        val ctrl = AdaptiveController(
            context = ctx.applicationContext,
            scope = viewModelScope,
            monitor = monitor,
            applyMode = { mode -> client.setAnc(mode) },
        )
        adaptive = ctrl
        viewModelScope.launch {
            ctrl.current.collect { _activity.value = it }
        }
        if (AdaptiveRules.isEnabled(ctx) && monitor.available) {
            ctrl.start()
            _adaptiveOn.value = true
        }
    }

    fun setAdaptive(ctx: Context, on: Boolean) {
        AdaptiveRules.setEnabled(ctx, on)
        _adaptiveOn.value = on
        if (on) adaptive?.start() else adaptive?.stop()
    }

    /**
     * Persisted locally: the cycle config is write-only on this model
     * (0x010C action 2 echoes action 1 back), so the device cannot be asked
     * what its cycle currently is.
     */
    private val _ancCycle = MutableStateFlow(AncMode.Companion.Cycle.DEFAULT)
    val ancCycle: StateFlow<AncMode.Companion.Cycle> = _ancCycle.asStateFlow()

    fun setAncCycle(cycle: AncMode.Companion.Cycle) {
        _ancCycle.value = cycle
        viewModelScope.launch { client.setAncCycle(cycle) }
    }

    fun setHighVolume(on: Boolean) {
        viewModelScope.launch { client.setHighVolume(on) }
    }

    fun setFindMode(on: Boolean) {
        _finding.value = on
        viewModelScope.launch { client.setFindMode(on) }
    }

    /**
     * Writes one Dynamic audio band.
     *
     * Writes are sent on release rather than
     * flooding the SPP link with a write per drag pixel. The device persists
     * the value itself, so there is no save step.
     */
    fun setBassBand(band: BassBand, value: Int) {
        viewModelScope.launch { client.setBassBand(band, value) }
    }

    /**
     * Dynamic audio master switch. Turning it on re-reads the bands, since
     * the sliders are meaningless until the engine is running.
     */
    fun setDynamicAudio(enabled: Boolean) {
        viewModelScope.launch {
            client.setFeature(Feature.DYNAMIC_BASS.id, enabled)
            delay(400)
            client.featureList(com.realbuds.app.proto.Feature.ids)
            if (enabled) client.queryBassBands()
        }
    }

    fun setEqPreset(preset: EqPreset) {
        viewModelScope.launch {
            client.setEqPreset(preset)
            delay(300)
            client.query(com.realbuds.app.proto.Cmd.EQ_STATUS)
        }
    }

    /** Which custom slot's editor is expanded; -1 = none. */
    private val _editingEqId = MutableStateFlow(-1)
    val editingEqId: StateFlow<Int> = _editingEqId.asStateFlow()

    fun toggleEqEditor(id: Int) {
        _editingEqId.value = if (_editingEqId.value == id) -1 else id
    }

    /** Apply a custom slot without rewriting its curve. */
    fun selectCustomEq(eq: com.realbuds.app.proto.CustomEq) {
        if (eq.isEmpty) return   // nothing stored yet; let the user edit first
        viewModelScope.launch {
            client.selectEq(eq.eqId)
            delay(300)
            client.query(com.realbuds.app.proto.Cmd.EQ_STATUS)
            client.queryCustomEqs()
        }
    }

    fun saveCustomEq(eq: com.realbuds.app.proto.CustomEq) {
        viewModelScope.launch {
            client.setCustomEq(eq, com.realbuds.app.proto.EqAction.MODIFY)
            delay(400)
            client.selectEq(eq.eqId)
            delay(300)
            client.queryCustomEqs()
            client.query(com.realbuds.app.proto.Cmd.EQ_STATUS)
        }
    }

    fun deleteCustomEq(eqId: Int) {
        viewModelScope.launch {
            client.deleteCustomEq(eqId)
            delay(400)
            client.queryCustomEqs()
            client.query(com.realbuds.app.proto.Cmd.EQ_STATUS)
        }
    }

    fun refreshCustomEqs() {
        viewModelScope.launch { client.queryCustomEqs() }
    }

    /** [deviceType] 1 = left, 2 = right, null = both. */
    fun setKeyBinding(action: Int, function: Int, deviceType: Int? = null) {
        viewModelScope.launch {
            client.setKeyBinding(action, function, deviceType)
            delay(350)
            client.queryKeys()   // read back, never assume it took
        }
    }

    /** The one error currently worth showing, or null. */
    val error: StateFlow<BudsError?> get() = client.errors.current

    /** How many further errors arrived while the first was unacknowledged. */
    val suppressedErrors: StateFlow<Int> get() = client.errors.suppressed

    fun dismissError() = client.errors.dismiss()

    /**
     * The log as one block of text, for pasting into a bug report.
     *
     * Oldest first — a reader wants the sequence that led to a failure, and
     * the on-screen list was newest-first, which reads backwards when copied.
     * Includes a header so a pasted log identifies its own device and build.
     */
    fun logForClipboard(): String {
        val lines = _logs.value.asReversed()
        val header = buildString {
            appendLine("RealBuds debug log")
            appendLine("device: ${connectedName.value ?: "not connected"}")
            appendLine("state: ${state.value}")
            appendLine("firmware: ${buds.value.firmware ?: "unknown"}")
            appendLine("features: ${buds.value.supportedFeatures}")
            appendLine("lines: ${lines.size}")
            appendLine("--")
        }
        return header + lines.joinToString("\n") { l ->
            val dir = if (l.outgoing) "TX" else "RX"
            "$dir ${l.text}" + if (l.hex.isNotBlank()) "  ${l.hex}" else ""
        }
    }

    fun sendRaw(cmd: Int, payload: ByteArray) {
        viewModelScope.launch { client.sendRaw(cmd, payload) }
    }

    fun clearLog() { _logs.value = emptyList() }

    fun close() = Unit
}
