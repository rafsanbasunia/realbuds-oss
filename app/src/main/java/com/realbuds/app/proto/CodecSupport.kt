package com.realbuds.app.proto

import android.bluetooth.BluetoothCodecConfig
import android.content.Context
import android.os.Build

/**
 * Whether this phone can actually negotiate a codec, as opposed to whether
 * the earbuds claim to support it.
 *
 * Buds Air 8 offers AAC, SBC and LHDC. LHDC is a proprietary Savitech codec
 * that needs a vendor library, and that library is missing from many ROMs
 * even when the Android framework's LHDC API surface is present. On such a
 * phone the LHDC toggle is worse than useless: the earbuds accept it and
 * restart their audio stage — which drops their output gain until the link is
 * rebuilt — in exchange for a codec the phone can never select.
 *
 * Verified on one such device: the A2DP stack reported local capabilities of
 * LDAC, aptX-HD, aptX, AAC and SBC with no LHDC entry, no LHDC library under
 * /system or /vendor, and `lhdc_codec_support` off while `a2dp_lhdc_api` was
 * on. The negotiated codec was AAC in every state.
 */
object CodecSupport {

    /**
     * True when the platform exposes an LHDC codec type at all.
     *
     * Checked reflectively because `SOURCE_CODEC_TYPE_LHDC*` constants are
     * not in the public SDK — they exist only where a vendor has added them.
     * Absence of the constant is the clearest signal the codec is unavailable;
     * a phone that can negotiate LHDC necessarily defines it.
     */
    fun hasLhdc(@Suppress("UNUSED_PARAMETER") context: Context? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Below API 33 the codec type constants are not queryable this
            // way. Assume supported rather than hiding a control that may
            // work, since the cost of a wrong guess is only a useless toggle.
            return true
        }
        return runCatching {
            BluetoothCodecConfig::class.java.declaredFields.any {
                it.name.startsWith("SOURCE_CODEC_TYPE_LHDC")
            }
        }.getOrDefault(true)
    }
}
