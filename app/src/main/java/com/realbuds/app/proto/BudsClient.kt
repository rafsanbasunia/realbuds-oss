package com.realbuds.app.proto

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class ConnState { DISCONNECTED, CONNECTING, CONNECTED }

data class LogLine(
    val outgoing: Boolean,
    val text: String,
    val hex: String,
    val at: Long = System.currentTimeMillis(),
)

/** Everything we know about the connected buds. */
data class BudsState(
    val batteries: List<BatteryInfo> = emptyList(),
    val anc: AncMode? = null,
    val features: Map<Int, Boolean> = emptyMap(),
    /**
     * Ids the earbuds actually reported in their feature list, in order.
     * Anything outside this is unsupported on this model: 0x0403 still
     * answers with status 0, but nothing changes.
     */
    val supportedFeatures: List<Int> = emptyList(),
    val eqPreset: EqPreset? = null,
    val customEqs: List<CustomEq> = emptyList(),
    /** Raw active EQ id from 0x010F — may be a preset or a custom slot. */
    val activeEqId: Int? = null,
    /** Per-bud wear state from 0x0109. */
    val wear: List<WearState> = emptyList(),
    val useTime: List<UseTime> = emptyList(),
    val keys: List<KeyBinding> = emptyList(),
    /** High-volume mode (regional volume-cap override), from 0x0110. */
    val highVolume: Boolean? = null,
    /** Push events this device offers, from 0x0200. */
    val pushEvents: List<Int> = emptyList(),
    /** Three-band Dynamic audio, keyed by band id from 0x012C. */
    val bassBands: List<BassBand> = emptyList(),
    val firmware: String? = null,
) {
    fun battery(slot: BatteryInfo.Slot) = batteries.firstOrNull { it.slot == slot }
    fun bass(band: Int) = bassBands.firstOrNull { it.band == band }

    /**
     * Wear state for drawing. Kept here so the bit interpretation lives with
     * the rest of the protocol rather than in a composable.
     */
    fun budWear(slot: BatteryInfo.Slot): com.realbuds.app.ui.components.BudWear {
        val w = wearOf(slot) ?: return com.realbuds.app.ui.components.BudWear.UNKNOWN
        return when {
            w.inCase -> com.realbuds.app.ui.components.BudWear.IN_CASE
            w.inEar -> com.realbuds.app.ui.components.BudWear.IN_EAR
            else -> com.realbuds.app.ui.components.BudWear.OUT
        }
    }
    fun wearOf(slot: BatteryInfo.Slot) = wear.firstOrNull { it.slot == slot }
}

/**
 * Talks to realme Buds over SPP.
 *
 * Sole owner of the socket: the earbuds accept one connection at a time, so
 * every read, write and notification is funnelled through here and published
 * as [StateFlow] for the UI.
 */
@SuppressLint("MissingPermission")
class BudsClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Single funnel for every failure; the UI shows these as alerts. */
    val errors = ErrorBus()

    private val _state = MutableStateFlow(ConnState.DISCONNECTED)
    val state: StateFlow<ConnState> = _state.asStateFlow()

    private val _buds = MutableStateFlow(BudsState())
    val buds: StateFlow<BudsState> = _buds.asStateFlow()

    private val _log = MutableSharedFlow<LogLine>(replay = 200, extraBufferCapacity = 200)
    val log: SharedFlow<LogLine> = _log.asSharedFlow()

    private var socket: BluetoothSocket? = null
    private var out: OutputStream? = null
    private var input: InputStream? = null
    private val seq = TransferIdCounter()
    private val writeLock = Mutex()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    fun candidates(adapter: BluetoothAdapter): List<BluetoothDevice> =
        adapter.bondedDevices.orEmpty().filter { d ->
            val n = (d.name ?: "").lowercase()
            listOf("realme", "buds", "oppo", "enco", "oneplus", "nord").any { n.contains(it) }
        }

    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        disconnect()
        _state.value = ConnState.CONNECTING
        _deviceName.value = device.name
        try {
            val s = device.createRfcommSocketToServiceRecord(UUID.fromString(Protocol.SPP_UUID))
            s.connect()
            socket = s
            out = s.outputStream
            input = s.inputStream
            _state.value = ConnState.CONNECTED
            emitLog(false, "connected to ${device.name}", "")
            startReader()
            scope.launch { handshake() }
            Result.success(Unit)
        } catch (e: IOException) {
            emitLog(false, "connect failed: ${e.message}", "")
            errors.connection("Could not connect to the earbuds.", e.message)
            _state.value = ConnState.DISCONNECTED
            cleanup()
            Result.failure(e)
        }
    }

    fun disconnect() {
        if (_state.value != ConnState.DISCONNECTED) emitLog(false, "disconnected", "")
        cleanup()
        _state.value = ConnState.DISCONNECTED
        _buds.value = BudsState()
    }

    private fun cleanup() {
        try { socket?.close() } catch (_: IOException) {}
        socket = null; out = null; input = null
    }

    /**
     * Startup sequence.
     *
     * The buds gate almost everything behind a capability bitmask, so the
     * order matters: ask for capabilities, then device info, and only then
     * the per-feature queries. `i()` in the real app runs after the
     * capability reply lands and starts with D() = 0x0101 device info.
     */
    suspend fun handshake() {
        // Order matters. The device expects MTU negotiation first, then
        // i() once capabilities land) opens with MTU negotiation; the buds
        // reject feature commands with status 1 until the session is set up.
        deviceInfo();                delay(250)
        query(Cmd.FIRMWARE_VERSION); delay(250)
        refreshAll()
    }

    /**
     * Re-read mutable state.
     *
     * Battery and earbud status arrive unprompted on 0x0402, so we do not
     * poll for them; these are the ones that need asking.
     */
    suspend fun refreshAll() {
        send(Cmd.ANC_STATUS, byteArrayOf(1, 1)); delay(250)
        query(Cmd.EARBUD_STATUS); delay(250)
        query(Cmd.USE_TIME);      delay(250)
        query(Cmd.KEY_FUNCTIONS); delay(250)
        query(Cmd.EQ_STATUS); delay(250)
        queryCustomEqs(); delay(250)
        queryBassBands(); delay(250)
        queryHighVolume(); delay(250)
        // Register for pushes so battery and wear state stop depending on
        // this poll running again.
        subscribePushEvents(); delay(250)
        featureList(Feature.ids)
    }

    /** 0x0101 with a u16 field mask. */
    suspend fun deviceInfo(mask: Int = 512): Boolean =
        send(Cmd.DEVICE_INFO, byteArrayOf(((mask shr 8) and 0xFF).toByte(), (mask and 0xFF).toByte()))

    /** 0x010D — the authoritative supported-feature query. */
    suspend fun featureList(ids: IntArray): Boolean {
        val payload = ByteArray(ids.size + 1)
        payload[0] = ids.size.toByte()
        ids.forEachIndexed { i, v -> payload[i + 1] = v.toByte() }
        return send(Cmd.FEATURE_LIST, payload)
    }

    // ---- outgoing ----

    /** Send a command with an empty payload (a plain query). */
    suspend fun query(cmd: Int) = send(cmd, ByteArray(0))

    suspend fun setAnc(mode: AncMode) = setAnc(mode.wire)

    suspend fun setAnc(wireValue: Int, action: Int = 1, type: Int = 1) =
        send(
            Cmd.SET_ANC,
            byteArrayOf(
                (if (action == 1) 1 else 2).toByte(),
                (if (type == 1) 1 else 2).toByte(),
                wireValue.toByte(),
            ),
        )

    /**
     * Sets which modes the noise-control gesture cycles through.
     *
     * The `type` byte appears to be model-dependent, so
     * try type 1 and fall back to 2 if the write is refused.
     */
    suspend fun setAncCycle(cycle: AncMode.Companion.Cycle, type: Int = 1) =
        send(
            Cmd.SET_ANC,
            byteArrayOf(2, type.toByte(), cycle.wire.toByte()),
        )

    suspend fun setFeature(feature: Feature, enabled: Boolean) =
        setFeature(feature.id, enabled)

    /**
     * No optimistic update: the buds reply with a status byte and may refuse.
     * The UI only moves once 0x8403 confirms, so a switch that springs back
     * means the earbuds said no.
     */
    suspend fun setFeature(id: Int, enabled: Boolean): Boolean =
        send(Cmd.SET_SWITCH_FEATURE, byteArrayOf(id.toByte(), if (enabled) 1 else 0))

    /** Query current on/off state for a set of feature ids. */
    suspend fun statusFeature(ids: IntArray): Boolean {
        val payload = ByteArray(ids.size + 1)
        payload[0] = ids.size.toByte()
        ids.forEachIndexed { i, v -> payload[i + 1] = v.toByte() }
        return send(Cmd.STATUS_FEATURE, payload)
    }

    /** Write a custom EQ slot. Also selects it. */
    suspend fun setCustomEq(eq: CustomEq, action: Int = EqAction.MODIFY) =
        send(Cmd.SET_EQ_INFO, eq.toPayload(action))

    suspend fun queryCustomEqs() = query(Cmd.CUSTOM_EQ_LIST)

    /**
     * Start or stop the earbuds' locate beep.
     *
     * Fire-and-forget: there is no status query and no push notification for
     * find mode, so the caller owns the "is it ringing" state. It is a latch,
     * not a one-shot — the buds keep beeping until told to stop.
     */
    suspend fun setFindMode(on: Boolean) =
        send(Cmd.FIND_BUDS, byteArrayOf(if (on) 1 else 0))

    /** Read high-volume mode. */
    suspend fun queryHighVolume() = query(Cmd.HIGH_VOLUME)

    /** Lift or restore the regional volume cap. */
    suspend fun setHighVolume(on: Boolean) =
        send(Cmd.SET_HIGH_VOLUME, byteArrayOf(if (on) 1 else 0))

    /**
     * Subscribe to the events this device offers, so battery and wear state
     * arrive as they change instead of only when polled.
     *
     * Asks 0x0200 first rather than assuming: these buds offer only events 1
     * and 2, and subscribing to an unsupported event is wasted traffic. The
     * bulk form 0x0205 is gated on a capability bit they do not report, so
     * each event is registered individually.
     */
    suspend fun subscribePushEvents() {
        query(Cmd.PUSH_EVENTS)
    }

    private suspend fun subscribe(eventId: Int) =
        send(Cmd.PUSH_SUBSCRIBE, byteArrayOf(eventId.toByte()))

    /** Reads all three Dynamic audio bands in one round-trip. */
    suspend fun queryBassBands() = query(Cmd.BASS_ENGINE_LIST)

    /**
     * Writes one band. min and max are echoed from what the device reported;
     * sending our own guesses risks the firmware clamping against a range it
     * never advertised.
     */
    suspend fun setBassBand(band: BassBand, value: Int) {
        send(Cmd.SET_BASS_ENGINE, band.toPayload(value))
    }

    /** Clear a custom slot. The buds drop it from the 0x0122 list entirely. */
    suspend fun deleteCustomEq(eqId: Int) = send(
        Cmd.SET_EQ_INFO,
        byteArrayOf(EqAction.DELETE.toByte(), (-6).toByte(), 6, eqId.toByte(), 0, 0),
    )

    suspend fun setEqPreset(preset: EqPreset) = selectEq(preset.id)

    /** Select any EQ by id — a built-in preset or a custom slot (4/5/6). */
    suspend fun selectEq(id: Int) = send(Cmd.SET_EQ_PRESET, byteArrayOf(id.toByte()))

    /**
     * Write touch bindings. Writes both earbuds together so left and right
     * stay in step — the buds accept per-side writes, but a split mapping is
     * confusing in practice.
     */
    /**
     * Writes one gesture binding.
     *
     * [deviceType] 1 = left, 2 = right, or null for both. The earbuds store
     * the two sides independently — verified by writing left long-press to
     * Volume up and right to Volume down and reading both back — so a
     * per-side write is a real capability, not a convenience.
     *
     * Payload: [count]{[deviceType][button][action][function]}*, button
     * always 1 (these buds expose a single touch surface per side).
     */
    suspend fun setKeyBinding(action: Int, function: Int, deviceType: Int? = null): Boolean {
        val sides = deviceType?.let { listOf(it) } ?: listOf(1, 2)
        val payload = ByteArray(1 + sides.size * 4)
        payload[0] = sides.size.toByte()
        sides.forEachIndexed { i, side ->
            val at = 1 + i * 4
            payload[at] = side.toByte()
            payload[at + 1] = 1
            payload[at + 2] = action.toByte()
            payload[at + 3] = function.toByte()
        }
        return send(Cmd.SET_KEY_FUNCTIONS, payload)
    }

    suspend fun queryKeys() = query(Cmd.KEY_FUNCTIONS)

    suspend fun sendRaw(commandId: Int, payload: ByteArray) = send(commandId, payload)

    private suspend fun send(commandId: Int, payload: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                val o = out ?: run {
                    emitLog(true, "DROPPED cmd=0x%04X — not connected".format(commandId), "")
                    errors.connection("Not connected to the earbuds.")
                    return@withLock false
                }
                val pkt = Packet(commandId, seq.take(), payload)
                val frame = LinkFrame.wrapSingle(pkt.toBytes())
                try {
                    o.write(frame)
                    o.flush()
                    emitLog(
                        true,
                        "cmd=0x%04X tid=%d".format(pkt.commandId, pkt.transferId),
                        hex(frame),
                    )
                    true
                } catch (e: IOException) {
                    emitLog(true, "write failed: ${e.message}", "")
                    errors.connection("Lost the connection while sending.", e.message)
                    false
                }
            }
        }

    // ---- incoming ----

    private fun startReader() {
        scope.launch {
            val buf = ByteArray(4096)
            var pending = ByteArray(0)
            val stream = input ?: return@launch
            try {
                while (true) {
                    val n = stream.read(buf)
                    if (n <= 0) break
                    pending += buf.copyOfRange(0, n)
                    emitLog(false, "raw ${n}B", hex(buf.copyOfRange(0, n)))
                    val (frames, consumed) = LinkFrame.parseAll(pending)
                    if (consumed > 0) pending = pending.copyOfRange(consumed, pending.size)
                    for (f in frames) {
                        Packet.parse(f.payload)?.let { handle(it) }
                    }
                }
            } catch (e: IOException) {
                emitLog(false, "reader stopped: ${e.message}", "")
                errors.connection("The earbuds stopped responding.", e.message)
            }
            _state.value = ConnState.DISCONNECTED
            cleanup()
        }
    }

    private fun handle(pkt: Packet) {
        emitLog(
            false,
            "cmd=0x%04X tid=%d len=%d".format(pkt.commandId, pkt.transferId, pkt.payload.size),
            hex(pkt.payload),
        )
        val body = pkt.payload
        when (pkt.baseCommandId) {
            // The buds push state here unprompted — this is where battery and
            // earbud status actually arrive, not from our 0x0106 polls.
            Cmd.NOTIFY -> handleNotify(body)

            // Pushed whenever a feature changes, from any source.
            Cmd.CUSTOM_EQ_LIST -> {
                if (body.isNotEmpty() && body[0].toInt() == 0) {
                    val list = CustomEq.parseList(body, 1)
                    if (list.isNotEmpty()) _buds.value = _buds.value.copy(customEqs = list)
                }
            }

            NOTIFY_CUSTOM_EQ -> {
                // [action][count][entry]* — same table, different leading byte.
                val list = CustomEq.parseList(body, 1)
                if (list.isNotEmpty()) {
                    _buds.value = _buds.value.copy(customEqs = list)
                    emitLog(false, "custom EQ updated", "")
                }
            }

            Cmd.BASS_ENGINE_LIST -> {
                val list = BassBand.parseList(body)
                if (list != null) {
                    _buds.value = _buds.value.copy(bassBands = list)
                    emitLog(false, "dynamic audio: " +
                        list.joinToString { "${it.label}=${it.value}" }, "")
                }
            }

            NOTIFY_BASS_ENGINE -> {
                // Single record, no status and no count byte.
                BassBand.parseOne(body)?.let { b ->
                    val merged = _buds.value.bassBands
                        .filter { it.band != b.band } + b
                    _buds.value = _buds.value.copy(
                        bassBands = merged.sortedBy { BassBand.ORDER.indexOf(it.band) },
                    )
                    emitLog(false, "${b.label} now ${b.value}", "")
                }
            }

            Cmd.HIGH_VOLUME -> {
                if (body.size >= 2 && body[0].toInt() == 0) {
                    _buds.value = _buds.value.copy(highVolume = body[1].toInt() != 0)
                }
            }

            Cmd.PUSH_EVENTS -> {
                // [status][count][eventId]* — then register each one.
                if (body.size >= 2 && body[0].toInt() == 0) {
                    val n = body[1].toInt() and 0xFF
                    val ids = (0 until n).mapNotNull { i ->
                        body.getOrNull(2 + i)?.toInt()?.and(0xFF)
                    }
                    _buds.value = _buds.value.copy(pushEvents = ids)
                    emitLog(false, "device pushes events $ids", "")
                    scope.launch { ids.forEach { subscribe(it); delay(120) } }
                }
            }

            Cmd.PUSH_SUBSCRIBE -> {
                if (body.size >= 2) {
                    val st = body[0].toInt() and 0xFF
                    val ev = body[1].toInt() and 0xFF
                    emitLog(false,
                        if (st == 0) "subscribed to event $ev"
                        else "subscribe event $ev REJECTED status=$st", "")
                    if (st != 0) errors.refused("Live updates", st, Cmd.PUSH_SUBSCRIBE)
                }
            }

            Cmd.FIND_BUDS -> {
                if (body.isNotEmpty()) {
                    val st = body[0].toInt() and 0xFF
                    emitLog(false, if (st == 0) "find mode ack" else "find REJECTED status=$st", "")
                    if (st != 0) errors.refused("Find my earbuds", st, Cmd.FIND_BUDS)
                }
            }

            Cmd.SET_BASS_ENGINE -> {
                // A status-0 ack proves only that the frame parsed; the value
                // is confirmed by the 0x0508 push or a re-read.
                if (body.isNotEmpty()) {
                    val st = body[0].toInt() and 0xFF
                    if (st != 0) {
                        emitLog(false, "dynamic audio write REJECTED status=$st", "")
                        errors.refused("Dynamic audio", st, Cmd.SET_BASS_ENGINE)
                    }
                }
            }

            Cmd.SET_EQ_INFO -> {
                if (body.isNotEmpty()) {
                    val st = body[0].toInt() and 0xFF
                    if (st != 0) {
                        emitLog(false, "custom EQ write REJECTED status=$st", "")
                        errors.refused("Custom EQ", st, Cmd.SET_EQ_INFO)
                    }
                }
            }

            NOTIFY_EQ -> {
                if (body.isNotEmpty()) {
                    val id = body[0].toInt() and 0xFF
                    _buds.value = _buds.value.copy(
                        activeEqId = id,
                        eqPreset = EqPreset.byId(id),
                    )
                    emitLog(false, "EQ now id $id", "")
                }
            }

            NOTIFY_FEATURE -> {
                if (body.size >= 3) {
                    mergeFeatures(listOf(FeatureState(body[1].toInt() and 0xFF, body[2].toInt() != 0)))
                }
            }

            Cmd.BATTERY -> {
                if (body.isNotEmpty() && body[0].toInt() == 0) {
                    BatteryInfo.parseList(body, 1)
                        .takeIf { it.isNotEmpty() }
                        ?.let { _buds.value = _buds.value.copy(batteries = it) }
                }
            }

            Cmd.STATUS_FEATURE, Cmd.FEATURE_LIST -> {
                if (body.isNotEmpty() && body[0].toInt() == 0) {
                    // Trailing id 0 entries are padding, not real features.
                    val states = FeatureState.parseList(body, 1).filter { it.id != 0 }
                    if (states.isNotEmpty()) {
                        mergeFeatures(states)
                        _buds.value = _buds.value.copy(
                            supportedFeatures = states.map { it.id }
                        )
                    }
                }
            }

            Cmd.EARBUD_STATUS -> {
                if (body.isNotEmpty() && body[0].toInt() == 0) {
                    val w = WearState.parseList(body, 1)
                    if (w.isNotEmpty()) {
                        _buds.value = _buds.value.copy(wear = w)
                        emitLog(false, "wear " + w.joinToString { "${it.slot}=${it.label}" }, "")
                    }
                }
            }

            Cmd.USE_TIME -> {
                if (body.isNotEmpty() && body[0].toInt() == 0) {
                    val u = UseTime.parseList(body, 1)
                    if (u.isNotEmpty()) _buds.value = _buds.value.copy(useTime = u)
                }
            }

            Cmd.KEY_FUNCTIONS -> {
                if (body.isNotEmpty() && body[0].toInt() == 0) {
                    val k = KeyBinding.parseList(body, 1)
                    if (k.isNotEmpty()) _buds.value = _buds.value.copy(keys = k)
                }
            }

            Cmd.ANC_STATUS -> {
                // [status][action][type][wire]; action 1 is the live mode.
                if (body.size >= 4 && body[0].toInt() == 0 && body[1].toInt() == 1) {
                    AncMode.fromWire(body[3].toInt() and 0xFF)?.let {
                        _buds.value = _buds.value.copy(anc = it)
                        emitLog(false, "ANC is ${it.label}", "")
                    }
                }
            }

            Cmd.EQ_STATUS -> {
                if (body.size >= 2 && body[0].toInt() == 0) {
                    val id = body[1].toInt() and 0xFF
                    _buds.value = _buds.value.copy(
                        activeEqId = id,
                        eqPreset = EqPreset.byId(id),   // null when a custom slot is active
                    )
                }
            }

            // Writes whose reply is only [status]. Grouped so a new write
            // command cannot silently fail for want of its own handler.
            Cmd.SET_SWITCH_FEATURE,
            Cmd.SET_EQ_PRESET,
            Cmd.SET_KEY_FUNCTIONS,
            Cmd.SET_HIGH_VOLUME -> {
                val st = if (body.isNotEmpty()) body[0].toInt() and 0xFF else 0
                if (st != 0) {
                    val what = when (pkt.baseCommandId) {
                        Cmd.SET_SWITCH_FEATURE -> "That feature"
                        Cmd.SET_EQ_PRESET -> "Sound effect"
                        Cmd.SET_KEY_FUNCTIONS -> "Touch control"
                        else -> "High volume"
                    }
                    emitLog(false, "write REJECTED cmd=0x%04X status=%d".format(
                        pkt.baseCommandId, st), "")
                    errors.refused(what, st, pkt.baseCommandId)
                } else if (pkt.baseCommandId == Cmd.SET_HIGH_VOLUME) {
                    scope.launch { queryHighVolume() }
                }
            }

            Cmd.SET_ANC -> {
                val st = if (body.isNotEmpty()) body[0].toInt() and 0xFF else 0
                if (st != 0) {
                    emitLog(false, "ANC write REJECTED status=$st", "")
                    errors.refused("Noise control", st, Cmd.SET_ANC)
                } else if (body.size >= 3) {
                    AncMode.fromWire(body[2].toInt() and 0xFF)?.let {
                        _buds.value = _buds.value.copy(anc = it)
                    }
                }
            }
        }
    }

    /**
     * Notifications on 0x0402: `[subtype][count][id, value] * count`.
     * Subtype 0xF2/0xF4 carry JSON diagnostics, which we ignore.
     */
    private fun handleNotify(body: ByteArray) {
        if (body.size < 2) return
        val subtype = body[0].toInt() and 0xFF
        if (subtype == Notify.JSON_TELEMETRY_A || subtype == Notify.JSON_TELEMETRY_B) return

        // ANC state is [subtype][action][type][wire] — not a count/pair list.
        if (subtype == Notify.ANC_STATE) {
            // Confirmed shape, sent after every mode change (including ones
            // made with the touch on the buds themselves):
            //     03 <action> <type> <wire>      exactly 4 bytes
            // Longer payloads on this subtype are something else; ignore them
            // rather than mis-parsing a byte as a mode.
            if (body.size == 4) {
                val wire = body[3].toInt() and 0xFF
                AncMode.fromWire(wire)?.let {
                    _buds.value = _buds.value.copy(anc = it)
                    emitLog(false, "ANC now ${it.label} (wire $wire)", "")
                } ?: emitLog(false, "ANC unknown wire value $wire", "")
            }
            return
        }

        val count = body[1].toInt() and 0xFF
        if (count == 0 || body.size < 2 + count * 2) return
        val pairs = (0 until count).map {
            val id = body[2 + it * 2].toInt() and 0xFF
            val v = body[3 + it * 2].toInt() and 0xFF
            id to v
        }

        when (subtype) {
            Notify.BATTERY -> {
                val list = pairs.map { (id, v) ->
                    BatteryInfo(id, v and 0x7F, (v and 0x80) != 0)
                }
                _buds.value = _buds.value.copy(batteries = list)
                emitLog(false, "battery " + list.joinToString { "${it.slot}=${it.level}%" }, "")
            }
            Notify.EARBUD_STATUS -> {
                // Same shape as 0x8109, pushed when a bud is taken out or
                // put back, so the UI updates without polling.
                val w = pairs.map { WearState(it.first, it.second) }
                _buds.value = _buds.value.copy(wear = w)
                emitLog(false, "wear " + w.joinToString { "${it.slot}=${it.label}" }, "")
            }
            else -> emitLog(false, "notify subtype 0x%02X: %s".format(subtype, pairs), "")
        }
    }

    private fun mergeFeatures(states: List<FeatureState>) {
        _buds.value = _buds.value.copy(
            features = _buds.value.features + states.associate { it.id to it.enabled }
        )
    }

    fun close() {
        disconnect()
        scope.cancel()
    }

    private fun emitLog(outgoing: Boolean, text: String, hex: String) {
        _log.tryEmit(LogLine(outgoing, text, hex))
        Log.d(TAG, (if (outgoing) "TX " else "RX ") + text + "  " + hex)
    }

    companion object {
        const val TAG = "BudsProto"
        fun hex(b: ByteArray): String = b.joinToString(" ") { "%02X".format(it) }
    }
}


/**
 * Process-wide client so the UI and the adb probe share one RFCOMM socket.
 * The buds only accept a single SPP connection, so a second one fails.
 */
object BudsClientHolder {
    val instance: BudsClient by lazy { BudsClient() }
}
