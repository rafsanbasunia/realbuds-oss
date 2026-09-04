package com.realbuds.app.proto

/**
 * A custom equaliser preset.
 *
 * Buds Air 8 exposes three slots (eqId 4, 5, 6) with six bands each. Verified
 * against hardware: writing 0x0418 with action [EqAction.MODIFY] updates the
 * slot, selects it, and the buds echo the whole table back on 0x0506.
 */
data class CustomEq(
    val eqId: Int,
    val name: String,
    val bands: List<Band>,
    val selected: Boolean = false,
    val minDb: Int = -6,
    val maxDb: Int = 6,
) {
    data class Band(val freqHz: Int, val db: Int)

    val isEmpty: Boolean get() = bands.isEmpty()

    fun withDb(index: Int, db: Int): CustomEq =
        copy(bands = bands.mapIndexed { i, b ->
            if (i == index) b.copy(db = db.coerceIn(minDb, maxDb)) else b
        })

    /**
     * Encode for 0x0418:
     *
     *     [action][min][max][eqId][nameLen][name][nBands][freqLo,freqHi,db]*
     *
     * Frequencies are little-endian u16; dB is a signed byte.
     */
    fun toPayload(action: Int): ByteArray {
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        val out = ArrayList<Byte>(5 + nameBytes.size + 1 + bands.size * 3)
        out += action.toByte()
        out += minDb.toByte()
        out += maxDb.toByte()
        out += eqId.toByte()
        out += nameBytes.size.toByte()
        nameBytes.forEach { out += it }
        out += bands.size.toByte()
        bands.forEach { b ->
            out += (b.freqHz and 0xFF).toByte()
            out += ((b.freqHz shr 8) and 0xFF).toByte()
            out += b.db.toByte()
        }
        return out.toByteArray()
    }

    companion object {
        /** Band centre frequencies reported by Buds Air 8. */
        val DEFAULT_FREQS = listOf(62, 250, 1000, 4000, 8000, 16000)

        fun flat(eqId: Int, name: String) =
            CustomEq(eqId, name, DEFAULT_FREQS.map { Band(it, 0) })

        /**
         * Parse the 0x8122 / 0x0506 table.
         *
         *   [status][count] then per entry:
         *   [selected][min][max][eqId][nameLen][name][nBands][freqLo,freqHi,db]*
         *
         * On 0x0506 the leading byte is an action code rather than a status,
         * so pass the right [offset].
         */
        fun parseList(data: ByteArray, offset: Int = 1): List<CustomEq> {
            if (data.size <= offset) return emptyList()
            var p = offset
            val count = data[p].toInt() and 0xFF; p++
            val out = ArrayList<CustomEq>(count)
            repeat(count) {
                if (p + 5 > data.size) return out
                val sel = data[p].toInt() and 0xFF; p++
                val min = data[p].toInt(); p++
                val max = data[p].toInt(); p++
                val id = data[p].toInt() and 0xFF; p++
                val nameLen = data[p].toInt() and 0xFF; p++
                if (p + nameLen > data.size) return out
                val nm = String(data, p, nameLen, Charsets.UTF_8); p += nameLen
                if (p >= data.size) return out
                val nBands = data[p].toInt() and 0xFF; p++
                if (p + nBands * 3 > data.size) return out
                val bands = (0 until nBands).map {
                    val f = (data[p].toInt() and 0xFF) or ((data[p + 1].toInt() and 0xFF) shl 8)
                    val db = data[p + 2].toInt()
                    p += 3
                    Band(f, db)
                }
                out += CustomEq(id, nm, bands, sel != 0, if (min != 0) min else -6, if (max != 0) max else 6)
            }
            return out
        }
    }
}

/** Action byte for 0x0418, from AllEqViewModel's calls to HeadsetManager.t0. */
object EqAction {
    const val ADD = 1
    const val MODIFY = 2
    /** Payload only needs [action][min][max][eqId][nameLen=0][nBands=0]. */
    const val DELETE = 3
    /** p() short-circuits on 5 and sends an empty query instead. */
    const val QUERY = 5
}
