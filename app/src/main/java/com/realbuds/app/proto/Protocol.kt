package com.realbuds.app.proto

/**
 * Wire protocol constants for realme Buds.
 *
 * Framing and command layouts are described where they are used: see
 * [LinkFrame] for the outer frame, [Packet] for the header, and [Cmd] in
 * Commands.kt for the command reference.
 */
object Protocol {

    /** Standard SPP UUID — the buds' control channel is Bluetooth Classic, not BLE. */
    const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

    /** Link-frame start-of-frame byte. */
    const val SOF: Byte = 0xAA.toByte()

    // ---- command ids (Protocol.java) ----
    const val CMD_SET_NOISE_REDUCTION = 0x404
    const val CMD_HEARING_DETECTION   = 0x40E
    const val CMD_BASS_ENGINE         = 0x41B

    /** Response id = request id or 0x8000. */
    const val RESPONSE_FLAG = 0x8000

    fun responseOf(cmd: Int) = cmd or RESPONSE_FLAG

    // ---- ANC ----
    /** action byte: 1 = set mode, 2 = set cycle mode */
    const val ACTION_SET_MODE = 1
    const val ACTION_SET_CYCLE = 2

    /**
     * Raw ANC wire values. See [AncMode] for the confirmed semantics.
     *
     * Kept for the developer/probe screen. Note these are wire values, not the
     * Vendor UI values -- the device remaps these to wire values before
     * sending (UI 1 -> 8, UI 2 -> 1, UI 4 -> 2, UI 8 -> 4).
     */
    val WIRE_VALUES = intArrayOf(0, 1, 2, 4, 8, 16, 32)
}
