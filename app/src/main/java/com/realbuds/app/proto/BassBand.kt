package com.realbuds.app.proto

/**
 * One band of the three-band "Dynamic audio" equaliser.
 *
 * The device owns the range: it reports min and max per band rather than the
 * app assuming a scale, so the UI reads them from the reply. On these buds
 * that comes back as -5..+5, but nothing here hardcodes that.
 *
 * Wire record is 4 bytes: [min i8][max i8][current i8][band u8]. All three
 * values are plain signed bytes, so -5 travels as 0xFB.
 */
data class BassBand(
    val band: Int,
    val min: Int,
    val max: Int,
    val value: Int,
) {
    /** Label for the band id. 1/2/3 are confirmed against the device's own
     *  string-resource binding (low / medium / high frequency), not guessed
     *  from list order. */
    val label: String get() = when (band) {
        LOW -> "Low freq"
        MID -> "Med freq"
        HIGH -> "High freq"
        else -> "Band $band"
    }

    /** Payload for [Cmd.SET_BASS_ENGINE]: min, max, current, band. */
    fun toPayload(newValue: Int = value): ByteArray = byteArrayOf(
        min.toByte(),
        max.toByte(),
        newValue.coerceIn(min, max).toByte(),
        band.toByte(),
    )

    companion object {
        const val LOW = 1
        const val MID = 2
        const val HIGH = 3

        /** Wire size of one record. */
        const val RECORD = 4

        /** Bands in display order, low through high. */
        val ORDER = listOf(LOW, MID, HIGH)

        /**
         * Parses a 0x812C list reply: [status][count] then count 4-byte
         * records. Returns null when the payload is short or the status is
         * non-zero, so a malformed reply cannot masquerade as "all bands at 0".
         */
        fun parseList(payload: ByteArray): List<BassBand>? {
            if (payload.size < 2) return null
            if (payload[0].toInt() != 0) return null
            val count = payload[1].toInt() and 0xFF
            if (count <= 0) return null
            val out = ArrayList<BassBand>(count)
            var i = 2
            repeat(count) {
                // Need min, max and current; the band byte is optional in the
                // official parser, so tolerate a 3-byte final record.
                if (i + 2 > payload.lastIndex) return@repeat
                val band = if (i + 3 <= payload.lastIndex) payload[i + 3].toInt() else -1
                out.add(
                    BassBand(
                        band = band,
                        min = payload[i].toInt(),
                        max = payload[i + 1].toInt(),
                        value = payload[i + 2].toInt(),
                    )
                )
                i += RECORD
            }
            return out.takeIf { it.isNotEmpty() }
        }

        /**
         * Parses a single record with no status and no count, as pushed on
         * [NOTIFY_BASS_ENGINE].
         */
        fun parseOne(payload: ByteArray): BassBand? {
            if (payload.size < 3) return null
            val band = if (payload.size >= 4) payload[3].toInt() else -1
            return BassBand(band, payload[0].toInt(), payload[1].toInt(), payload[2].toInt())
        }
    }
}
