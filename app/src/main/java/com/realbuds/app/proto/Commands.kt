package com.realbuds.app.proto

/**
 * Command ids for the TL protocol, from `tl.protocol.packet.Protocol`.
 *
 * Response id is always `request or 0x8000`.
 *
 * Ranges: 0x01xx device info & status, 0x02xx pairing, 0x03xx multi-connect,
 * 0x04xx audio features, 0x05xx keys, 0x0Fxx firmware upgrade.
 */
object Cmd {
    // --- 0x01xx : info / status ---
    /**
     * MTU negotiation — the FIRST command of a session.
     * This is sent before anything else, and
     * the buds appear to withhold capabilities until it has been done.
     * Reply 0x8100 = [status][u16 mtu].
     */
    const val MTU               = 0x0100
    const val DEVICE_INFO       = 0x0101   // payload: u16 field mask
    const val FIRMWARE_VERSION  = 0x0102   // payload: u16 version
    const val CAPABILITIES      = 0x0103   // empty; reply = capability bitmask
    const val PROTOCOL_VERSION  = 0x0104   // payload: ascii version string
    const val FW_VERSIONS       = 0x0105   // empty; per-bud firmware strings
    /**
     * Per-bud wear state. Reply [status][count][id, flags]*, where flags is a
     * bitfield:
     *     bit0 clear -> in the charging case
     *     bit1 set   -> in the ear
     * Verified on hardware: 3 = worn, 1 = out but not worn, 0 = in case.
     */
    const val EARBUD_STATUS     = 0x0109
    const val BATTERY           = 0x0106   // empty; reply = list of BatteryInfo
    const val KEY_FUNCTIONS     = 0x0108   // read:  [deviceType,button,action,function]*
    /** Write touch bindings: [count][deviceType,button,action,function]*. */
    const val SET_KEY_FUNCTIONS = 0x0401
    /** Accumulated listening time per bud, u32 little-endian, in MINUTES. */
    /**
     * Dynamic audio: read all bands at once.
     *
     * Reply is [status u8][count u8] then count * 4 bytes of
     * [min i8][max i8][current i8][band u8]. A capture from these buds gave
     * "00 03 <4 bytes>*3", i.e. three bands, which is what the UI shows.
     *
     * One read returns all three bands, rather than three round-trips via
     * the single-band 0x0124 form.
     */
    const val BASS_ENGINE_LIST   = 0x012C

    const val USE_TIME          = 0x0128
    const val EQ_STATUS         = 0x010F   // empty; reply = [status][eqId]
    /** Query the custom EQ slots; reply 0x8122 = [status][count][entry]*. */
    const val CUSTOM_EQ_LIST    = 0x0122
    /**
     * Query ANC state. Payload [action, type]; reply
     * [status, action, type, wireValue]. Action 1 = current mode,
     * action 2 = cycle-mode config.
     */
    const val ANC_STATUS        = 0x010C
    const val FEATURE_LIST      = 0x010D   // payload: count + feature ids
    const val PERSONAL_ANC_RESULT = 0x011A
    const val STATUS_FEATURE    = 0x0129   // payload: count + feature ids

    /**
     * Ask which events this device is willing to push. Empty payload; reply
     * 0x8200 = [status][count][eventId]*.
     *
     * Buds Air 8 answers "00 02 02 01" — only battery (1) and wear state (2).
     * The protocol defines many more (ANC change, tap gesture, fit test), but
     * this device does not offer them, so the poll for those stays.
     */
    const val PUSH_EVENTS        = 0x0200

    /**
     * Subscribe to one push event: payload [eventId], reply [status][eventId].
     * Verified: 0x0201 [01] -> 0x8201 [00][01].
     *
     * The bulk form 0x0205 exists but is gated on a capability bit these buds
     * do not report, so subscribe one at a time.
     */
    const val PUSH_SUBSCRIBE     = 0x0201

    /** Cancel a subscription: payload [eventId]. */
    const val PUSH_UNSUBSCRIBE   = 0x0202

    /**
     * Unsolicited notification channel. The buds push state here on connect
     * and whenever something changes — you do not have to ask.
     *
     * Payload: [subtype][count][id, value] * count
     * (JSON telemetry also arrives here, with subtype 0xF4 / 0xF2.)
     */
    const val NOTIFY             = 0x0204

    /**
     * Find my earbuds: payload is one byte, 1 = start, 0 = stop. The buds beep
     * themselves (firmware-side), so there is no volume or duration to set and
     * no left/right selector — the firmware decides. Reply 0x8400 = [status].
     *
     * Verified on Buds Air 8. The earbuds answer this readily even though
     * the stock app does not offer the feature.
     */
    const val FIND_BUDS          = 0x0400

    // --- 0x04xx : audio features ---
    const val SET_SWITCH_FEATURE = 0x0403  // payload: [featureId, 0|1]
    const val SET_ANC            = 0x0404  // payload: [action, type, mode]
    /**
     * Select a sound-effect preset: payload is a single id byte.
     * Reply 0x8406 = [status],
     * and the new value is pushed on 0x0504.
     */
    const val SET_EQ_PRESET      = 0x0406
    /** Full profile write, including custom EQ bands. */
    const val SET_EQ_INFO        = 0x0418
    /**
     * Dynamic audio: set one frequency band.
     *
     * Payload is [min i8][max i8][current i8][band u8]. A 3-byte form
     * without the band byte also exists; we always send 4 so the write is
     * unambiguous.
     *
     * Values are plain signed bytes, so -5 goes out as 0xFB. min and max are
     * echoed back from what the device reported rather than assumed.
     */
    const val SET_BASS_ENGINE    = 0x041B
    /**
     * High-volume mode: [0|1]. Lifts the regional volume cap.
     *
     * Read the current value with 0x0110 ([status][0|1]). Verified on Buds
     * Air 8: writing flips the 0x0110 read between 00 01 and 00 00.
     */
    const val SET_HIGH_VOLUME    = 0x0407

    /** Read high-volume mode: reply [status][0|1]. */
    const val HIGH_VOLUME        = 0x0110

    const val HEARING_DETECTION  = 0x040E
    const val SET_MIC_STATE      = 0x042C

    fun response(cmd: Int) = cmd or 0x8000
}

/** Unsolicited feature-change notification: [id, value]. */
const val NOTIFY_FEATURE = 0x0503

/** Pushed when the EQ preset changes: [presetId]. */
const val NOTIFY_EQ = 0x0504

/** Pushed when a custom EQ slot changes: [action][count][entry]*. */
const val NOTIFY_CUSTOM_EQ = 0x0506

/**
 * Unsolicited single-band bass-engine push: [min i8][max i8][current i8][band u8].
 * No status byte and no count, unlike the 0x812C list reply.
 */
const val NOTIFY_BASS_ENGINE = 0x0508

/** Subtypes seen on [Cmd.NOTIFY]. */
object Notify {
    const val BATTERY = 0x01      // [id, level|charging<<7] — id 1=L 2=R 3=case
    const val EARBUD_STATUS = 0x02
    /** [action, type, wireValue] — the buds' current ANC mode, pushed on change. */
    const val ANC_STATE = 0x03
    const val JSON_TELEMETRY_A = 0xF2
    const val JSON_TELEMETRY_B = 0xF4
}

/**
 * Feature ids for [Cmd.SET_SWITCH_FEATURE] / [Cmd.FEATURE_LIST].
 *
 * The protocol defines these ids; a device reports the subset it supports
 * via 0x010D. Meanings were established by toggling each on hardware and
 * observing what changed:
 *
 *     5   Power saving          17  Multi-device
 *     7   Party mode            24  HD audio (LHDC)
 *     9   Enhance voice         26  Smart de-wind
 *     11  Personalised hearing  27  Spatial audio
 *     12  Enhance voice (call)  29  Dynamic bass
 *
 * Ids 4, 6, 8 and 38 behave as auto play/pause, game mode, auto answer and
 * find-my-phone respectively. Id 19 is defined by the protocol but has no
 * observable effect on this model, so it is marked [certain] = false.
 */
enum class Feature(
    val id: Int,
    val label: String,
    val description: String,
    /** false = the label is inferred and may be wrong. */
    val certain: Boolean = true,
) {
    POWER_SAVING(5, "Power saving", "Reduce power draw", true),
    PARTY_MODE(7, "Party mode", "Share audio with another set of buds", true),
    ENHANCE_VOICE(9, "Enhance voice", "Emphasise speech", true),
    ENHANCE_VOICE_2(12, "Voice enhancement (call)", "Second voice-enhance channel", true),
    MULTI_DEVICE(17, "Multi-device", "Stay connected to two devices", true),
    HD_AUDIO(24, "High-definition sound", "LHDC 5.0 high resolution", true),
    WIND_NOISE(26, "Smart de-wind", "Reduce wind noise (affects ANC)", true),
    SPATIAL_AUDIO(27, "Spatial audio", "3D sound effect", true),
    DYNAMIC_BASS(29, "Dynamic bass", "Boost low frequencies at low volume", true),

    GAME_MODE(6, "Game mode", "Lower latency for gaming", true),
    AUTO_PLAY_PAUSE(4, "Auto play/pause", "Pause when you remove an earbud", true),
    AUTO_ANSWER(8, "Auto answer", "Answer calls by wearing an earbud", true),

    /**
     * Applies a stored personalised hearing profile.
     *
     * Buds Air 8 does not answer the hearing-test commands (0x0111, 0x0115),
     * so there is no way to create a profile here — this only applies one
     * that already exists on the earbuds. With no profile stored, toggling
     * it has no audible effect.
     */
    HEAR_ID(11, "Personalised hearing", "Apply your HearID hearing profile", true),

    /** The earbuds ask the phone to ring. */
    FIND_PHONE(38, "Find my phone", "Let the earbuds make this phone ring", true),

    /** Virtual bass: synthesises low frequencies. Not reported by Buds Air 8. */
    VIRTUAL_BASS(54, "Virtual bass", "Synthesise low frequencies", true),

    /**
     * Present in the protocol but
     * that case does not exist in this build and no setter writes id 19 — so
     * it has no consumer to identify it from. Not reported by Buds Air 8.
     */
    UNKNOWN_19(19, "Setting 19", "Unidentified toggle", false),
    ;

    /** Two words at most, for a 96dp tile. The long label is for list rows. */
    val shortLabel: String get() = when (this) {
        POWER_SAVING -> "Power saving"
        PARTY_MODE -> "Party mode"
        ENHANCE_VOICE -> "Voice"
        ENHANCE_VOICE_2 -> "Call voice"
        MULTI_DEVICE -> "Multi device"
        HD_AUDIO -> "LHDC"
        WIND_NOISE -> "De-wind"
        SPATIAL_AUDIO -> "Spatial audio"
        DYNAMIC_BASS -> "Dynamic bass"
        GAME_MODE -> "Game"
        AUTO_PLAY_PAUSE -> "Auto play"
        AUTO_ANSWER -> "Auto answer"
        HEAR_ID -> "HearID"
        FIND_PHONE -> "Find phone"
        VIRTUAL_BASS -> "Virtual bass"
        else -> label
    }

    val glyph: com.realbuds.app.ui.components.QuickGlyph get() = when (this) {
        GAME_MODE -> com.realbuds.app.ui.components.QuickGlyph.GAME
        WIND_NOISE -> com.realbuds.app.ui.components.QuickGlyph.WIND
        SPATIAL_AUDIO -> com.realbuds.app.ui.components.QuickGlyph.SPATIAL
        ENHANCE_VOICE, ENHANCE_VOICE_2 -> com.realbuds.app.ui.components.QuickGlyph.VOICE
        MULTI_DEVICE -> com.realbuds.app.ui.components.QuickGlyph.MULTI
        HD_AUDIO -> com.realbuds.app.ui.components.QuickGlyph.HD
        DYNAMIC_BASS -> com.realbuds.app.ui.components.QuickGlyph.BASS
        AUTO_PLAY_PAUSE -> com.realbuds.app.ui.components.QuickGlyph.PLAY_PAUSE
        POWER_SAVING -> com.realbuds.app.ui.components.QuickGlyph.POWER
        AUTO_ANSWER -> com.realbuds.app.ui.components.QuickGlyph.CALL
        HEAR_ID -> com.realbuds.app.ui.components.QuickGlyph.VOICE
        FIND_PHONE -> com.realbuds.app.ui.components.QuickGlyph.CALL
        VIRTUAL_BASS -> com.realbuds.app.ui.components.QuickGlyph.BASS
        else -> com.realbuds.app.ui.components.QuickGlyph.GENERIC
    }

    companion object {
        fun byId(id: Int) = entries.firstOrNull { it.id == id }
        /** The full set worth querying, in protocol order. */
        val ids: IntArray =
            intArrayOf(7, 6, 4, 5, 11, 8, 17, 19, 24, 9, 26, 12, 29, 27, 54, 38)
        val known: List<Feature> get() = entries.filter { it.certain }
        val unknown: List<Feature> get() = entries.filterNot { it.certain }

        /**
         * Buds Air 8 reports these 13 in its 0x010D feature list. Ids 7, 19
         * and 54 are defined by the protocol but absent here — writing
         * them returns status 0 and changes nothing, so the reply status is
         * not a reliable "supported" signal; the feature list is.
         */
        val airEight = intArrayOf(6, 4, 5, 11, 8, 17, 24, 9, 26, 12, 29, 27, 38)
    }
}

/** One battery reading. `BatteryInfo` in the app: 2 bytes per entry. */
data class BatteryInfo(
    val deviceType: Int,
    val level: Int,
    val charging: Boolean,
) {
    /** 1 = left, 2 = right, 3 = case — inferred from the official UI (L / R / case). */
    val slot: Slot get() = when (deviceType) {
        1 -> Slot.LEFT
        2 -> Slot.RIGHT
        3 -> Slot.CASE
        else -> Slot.UNKNOWN
    }

    enum class Slot { LEFT, RIGHT, CASE, UNKNOWN }

    companion object {
        const val ENTRY_SIZE = 2

        /** Parse `count` then `count` 2-byte entries, starting at [offset]. */
        fun parseList(data: ByteArray, offset: Int): List<BatteryInfo> {
            if (data.size <= offset) return emptyList()
            val count = data[offset].toInt() and 0xFF
            var p = offset + 1
            if (count <= 0 || data.size < p + count * ENTRY_SIZE) return emptyList()
            return (0 until count).map {
                val type = data[p].toInt() and 0xFF
                val raw = data[p + 1].toInt() and 0xFF
                p += ENTRY_SIZE
                BatteryInfo(type, raw and 0x7F, (raw and 0x80) != 0)
            }
        }
    }
}

/**
 * Where one earbud is right now.
 *
 * The flags byte is a bitfield, not an enum — reading it as an enum would make
 * "out but not worn" (1) look like an unknown value.
 */
data class WearState(val id: Int, val flags: Int) {
    val inCase: Boolean get() = (flags and 1) == 0
    val inEar: Boolean get() = (flags and 2) != 0

    val slot: BatteryInfo.Slot get() = when (id) {
        1 -> BatteryInfo.Slot.LEFT
        2 -> BatteryInfo.Slot.RIGHT
        3 -> BatteryInfo.Slot.CASE
        else -> BatteryInfo.Slot.UNKNOWN
    }

    /** Short label for the UI. */
    val label: String get() = when {
        inCase -> "In case"
        inEar -> "In ear"
        else -> "Out"
    }

    companion object {
        fun parseList(data: ByteArray, offset: Int): List<WearState> {
            if (data.size <= offset) return emptyList()
            val count = data[offset].toInt() and 0xFF
            var p = offset + 1
            if (count == 0 || data.size < p + count * 2) return emptyList()
            return (0 until count).map {
                val id = data[p].toInt() and 0xFF
                val f = data[p + 1].toInt() and 0xFF
                p += 2
                WearState(id, f)
            }
        }
    }
}

/** Accumulated listening time for one earbud. */
data class UseTime(val id: Int, val minutes: Long) {
    val slot: BatteryInfo.Slot get() = when (id) {
        1 -> BatteryInfo.Slot.LEFT
        2 -> BatteryInfo.Slot.RIGHT
        else -> BatteryInfo.Slot.UNKNOWN
    }
    val hours: Long get() = minutes / 60
    val remainder: Long get() = minutes % 60
    val label: String get() = "${hours}h ${remainder}m"

    companion object {
        /** [count] then [deviceType][u32 LE] per bud. */
        fun parseList(data: ByteArray, offset: Int): List<UseTime> {
            if (data.size <= offset) return emptyList()
            val count = data[offset].toInt() and 0xFF
            var p = offset + 1
            if (count == 0 || data.size < p + count * 5) return emptyList()
            return (0 until count).map {
                val id = data[p].toInt() and 0xFF
                val v = (data[p + 1].toLong() and 0xFF) or
                        ((data[p + 2].toLong() and 0xFF) shl 8) or
                        ((data[p + 3].toLong() and 0xFF) shl 16) or
                        ((data[p + 4].toLong() and 0xFF) shl 24)
                p += 5
                UseTime(id, v)
            }
        }
    }
}

/**
 * One touch-gesture binding.
 */
data class KeyBinding(val deviceType: Int, val button: Int, val action: Int, val function: Int) {
    val side: String get() = when (deviceType) { 1 -> "Left"; 2 -> "Right"; else -> "?" }

    /**
     * Gesture name for the wire action id.
     *
     * Action ids:
     *
     *     1  ITEM_ACTION_SINGLE_CLICK
     *     2  ITEM_ACTION_DOUBLE_CLICK
     *     3  ITEM_ACTION_TRI_CLICK
     *     4  ITEM_ACTION_LONG_CLICK_1S
     *     5  ITEM_ACTION_LONG_CLICK_2S
     *     6  ITEM_ACTION_LONG_CLICK_3S
     *
     * These labels were previously shifted by one — single tap was omitted,
     * so id 1 was shown as "Double tap" and every row named the wrong
     * gesture. Confirmed on hardware: writing Next track to id 2 fires on a
     * double tap, not a triple.
     */
    val actionLabel: String get() = when (action) {
        1 -> "Single tap"
        2 -> "Double tap"
        3 -> "Triple tap"
        4 -> "Long press (1s)"
        5 -> "Long press (2s)"
        6 -> "Long press (3s)"
        else -> "Action $action"
    }

    val functionLabel: String get() = FUNCTIONS[function] ?: "Unknown ($function)"

    companion object {
        /**
         * function id -> label, from CommandProtocol.BUTTON_ACTION (1..10).
         *
         * Ids 1, 6 and 8 are confirmed: they are what this device shipped with
         * (triple tap = play/pause, long press = noise control, long press 2s =
         * game mode). The rest follow the enum's declaration order, which is
         * the usual media-control set; ids past 8 are offered but unverified.
         */
        val FUNCTIONS = linkedMapOf(
            0 to "None",
            1 to "Play / pause",
            3 to "Voice assistant",
            5 to "Previous track",
            6 to "Next track",
            8 to "Noise control",
            11 to "Volume up",
            12 to "Volume down",
            13 to "Switch device",
            17 to "Game mode",
            26 to "Live translation",
            27 to "Conversation translation",
            31 to "AI assistant",
        )

        /**
         * Action ids these earbuds actually act on.
         *
         * The device reports a slot for action 1 (single tap) and stores a
         * function there, but never fires it — confirmed on hardware. That
         * Consistent with the protocol, which only accepts a single-tap
         * binding for a different hardware class. Reported storage is not
         * reported capability.
         *
         * A reported slot is not proof the gesture is bound: the reply lists
         * storage, not capability.
         */
        val USABLE_ACTIONS = listOf(2, 3, 4, 5, 6)

        /**
         * Offered in the picker, in display order.
         *
         * Translation and AI ids (26/27/31) are omitted: they exist in the
         * firmware table but depend on services this app does not implement.
         */
        val ASSIGNABLE = listOf(0, 1, 6, 5, 3, 8, 11, 12, 13, 17)

        fun parseList(data: ByteArray, offset: Int): List<KeyBinding> {
            if (data.size <= offset) return emptyList()
            val count = data[offset].toInt() and 0xFF
            var p = offset + 1
            if (count == 0 || data.size < p + count * 4) return emptyList()
            return (0 until count).map {
                val k = KeyBinding(
                    data[p].toInt() and 0xFF,
                    data[p + 1].toInt() and 0xFF,
                    data[p + 2].toInt() and 0xFF,
                    data[p + 3].toInt() and 0xFF,
                )
                p += 4
                k
            }
        }
    }
}

/** A device feature toggle and its current value. `FeatureSwitchInfo`: 2 bytes. */
data class FeatureState(val id: Int, val enabled: Boolean) {
    companion object {
        fun parseList(data: ByteArray, offset: Int): List<FeatureState> {
            if (data.size <= offset) return emptyList()
            val count = data[offset].toInt() and 0xFF
            var p = offset + 1
            if (count == 0 || data.size < p + count * 2) return emptyList()
            return (0 until count).map {
                val id = data[p].toInt() and 0xFF
                val v = data[p + 1].toInt() and 0xFF
                p += 2
                FeatureState(id, v != 0)
            }
        }
    }
}

/**
 * Built-in sound-effect presets.
 *
 * The wire ids are not in the order the presets are usually listed, so do
 * not infer one from the other — read the id and label together:
 *
 *     0  Nature Balance      2  Clear Vocals
 *     1  Bass Boost          3  Clear Bass
 *
 * Custom slots share this id space: 0x0406 with eqId 4/5/6 selects a custom
 * EQ, and 0x010F then reports that id. Verified on hardware.
 */
enum class EqPreset(val id: Int, val label: String) {
    CLEAR_VOCALS(2, "Clear Vocals"),
    NATURE_BALANCE(0, "Nature Balance"),
    CLEAR_BASS(3, "Clear Bass"),
    BASS_BOOST(1, "Bass Boost"),
    ;

    companion object {
        fun byId(id: Int) = entries.firstOrNull { it.id == id }
        /** Custom slots start here; anything >= this is a custom EQ. */
        const val FIRST_CUSTOM_ID = 4
        fun isCustom(id: Int) = id >= FIRST_CUSTOM_ID
    }
}
