package com.realbuds.app.proto

/**
 * SDK-level packet: 5-byte header + payload.
 *
 *   off 0  2  commandId   little-endian (bit15 = response flag)
 *   off 2  1  transferId  sequence, echoed by the device
 *   off 3  2  dataLength  little-endian
 *   off 5  N  payload
 *
 * The command id and length are LITTLE-endian on the wire. Confirmed on
 * hardware: battery replies arrive as bytes `06 81`, i.e. 0x8106 = the
 * response to 0x0106, not 0x0681.
 */
data class Packet(
    val commandId: Int,
    val transferId: Int,
    val payload: ByteArray,
) {
    val isResponse: Boolean get() = (commandId and Protocol.RESPONSE_FLAG) != 0
    val baseCommandId: Int get() = commandId and 0x7FFF

    fun toBytes(): ByteArray {
        val out = ByteArray(5 + payload.size)
        out[0] = (commandId and 0xFF).toByte()
        out[1] = ((commandId shr 8) and 0xFF).toByte()
        out[2] = (transferId and 0xFF).toByte()
        out[3] = (payload.size and 0xFF).toByte()
        out[4] = ((payload.size shr 8) and 0xFF).toByte()
        payload.copyInto(out, 5)
        return out
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Packet) return false
        return commandId == other.commandId &&
            transferId == other.transferId &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int =
        (commandId * 31 + transferId) * 31 + payload.contentHashCode()

    companion object {
        /** Parse a 5-byte-header packet. Returns null if too short or truncated. */
        fun parse(data: ByteArray): Packet? {
            if (data.size < 5) return null
            val cmd = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
            val tid = data[2].toInt() and 0xFF
            val len = (data[3].toInt() and 0xFF) or ((data[4].toInt() and 0xFF) shl 8)
            val avail = data.size - 5
            val take = if (len <= avail) len else avail
            return Packet(cmd, tid, data.copyOfRange(5, 5 + take))
        }
    }
}

/** Per-device transfer-id counter, wraps at 255 (PacketFactory.f). */
class TransferIdCounter {
    private var next = 0
    @Synchronized fun take(): Int {
        val cur = next
        next = if (cur + 1 <= 255) cur + 1 else 0
        return cur
    }
}
