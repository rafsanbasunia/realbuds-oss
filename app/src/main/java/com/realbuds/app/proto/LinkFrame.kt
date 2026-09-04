package com.realbuds.app.proto

/**
 * Link-layer framing.
 *
 *   [0]     SOF 0xAA
 *   [1..]   linkDataLen  varint, 7 bits/byte, low byte first, 0x80 = continue
 *   [i]     control byte, bits 0-1 = fragment state (FSN)
 *   [i+1]   one further header byte (the app's parser skips it)
 *   [i+2]   payload
 *
 * where i = 1 + (varint byte count).
 *
 * NOTE: the encoder below covers single-frame writes, which is the only case
 * needed here — an ANC command is ~12 bytes against a 20-byte MTU. The exact
 * meaning of the byte at [i+1] is unconfirmed; if the earbuds start rejecting
 * frames, that byte is the first suspect.
 */
object LinkFrame {

    const val DEFAULT_MTU = 20

    const val FSN_SINGLE = 0
    const val FSN_FIRST = 1
    const val FSN_MIDDLE = 2
    const val FSN_LAST = 3

    /** Encode an int as the frame varint. */
    fun encodeVarint(value: Int): ByteArray {
        if (value == 0) return byteArrayOf(0)
        var n = 0
        var v = value
        while (v != 0) { n++; v = v ushr 7 }
        val out = ByteArray(n)
        v = value
        for (i in 0 until n) {
            out[i] = if (i == n - 1) (v and 0x7F).toByte()
                     else ((v and 0x7F) or 0x80).toByte()
            v = v ushr 7
        }
        return out
    }

    /** Decode the varint at [offset]; returns value to byteCount. */
    fun decodeVarint(data: ByteArray, offset: Int): Pair<Int, Int> {
        var value = 0
        var i = 0
        while (true) {
            val b = data[offset + i].toInt()
            value = value or ((b and 0x7F) shl (7 * i))
            i++
            if ((b and 0x80) == 0) return value to i
        }
    }

    /**
     * Wrap a payload in a single link frame.
     *
     * Confirmed working against real hardware: 0x0103 round-trips with the
     * tid echoed, so this encoding is correct.
     *
     *     AA <varint bodyLen> <control 0x00> <reserved 0x00> <payload>
     */
    fun wrapSingle(payload: ByteArray): ByteArray {
        val body = ByteArray(2 + payload.size)
        body[0] = FSN_SINGLE.toByte()
        body[1] = 0
        payload.copyInto(body, 2)

        val lenBytes = encodeVarint(body.size)
        val frame = ByteArray(1 + lenBytes.size + body.size)
        frame[0] = Protocol.SOF
        lenBytes.copyInto(frame, 1)
        body.copyInto(frame, 1 + lenBytes.size)
        return frame
    }

    data class Parsed(val payload: ByteArray, val fsn: Int, val consumed: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Parsed) return false
            return payload.contentEquals(other.payload) &&
                fsn == other.fsn && consumed == other.consumed
        }
        override fun hashCode(): Int =
            (payload.contentHashCode() * 31 + fsn) * 31 + consumed
    }

    /**
     * Parse one link frame from the head of [data].
     * Returns null if it is not a frame or is incomplete.
     */
    fun parseOne(data: ByteArray): Parsed? {
        if (data.size < 4) return null
        if (data[0] != Protocol.SOF) return null

        val (linkLen, varLen) = decodeVarint(data, 1)
        val ctrlIdx = 1 + varLen
        if (ctrlIdx >= data.size) return null

        val fsn = data[ctrlIdx].toInt() and 0x03
        // frame length as the parser expects it
        val frameLength = linkLen + 1 + varLen
        if (frameLength > data.size) return null

        // single package: payload at ctrlIdx+2; multi: one byte later
        val start = if (fsn == FSN_SINGLE) ctrlIdx + 2 else ctrlIdx + 3
        if (start > frameLength) return null

        return Parsed(data.copyOfRange(start, frameLength), fsn, frameLength)
    }

    /** Split a stream buffer into whole frames; returns frames and bytes consumed. */
    fun parseAll(buffer: ByteArray): Pair<List<Parsed>, Int> {
        val out = mutableListOf<Parsed>()
        var off = 0
        while (off < buffer.size) {
            if (buffer[off] != Protocol.SOF) { off++; continue }
            val p = parseOne(buffer.copyOfRange(off, buffer.size)) ?: break
            out += p
            off += p.consumed
        }
        return out to off
    }
}
