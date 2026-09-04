package com.realbuds.app.adaptive

import android.content.Context
import com.realbuds.app.proto.AncMode

/**
 * One rule: when the wearer is doing X, put the earbuds in mode Y.
 *
 * Kept to activity only. A location dimension (Sony's "at home / at work")
 * would need background location, which is a far heavier permission ask than
 * a step counter and one Android increasingly gates behind a system dialog —
 * not worth it until the activity rules prove useful in daily use.
 */
data class AdaptiveRule(
    val activity: Motion,
    val mode: AncMode,
    val enabled: Boolean,
)

/**
 * Adaptive ANC settings, persisted across launches.
 *
 * This is the phone-side layer *under* the earbuds' own Smart mode. Smart
 * (wire 32) already adapts cancellation strength in firmware based on
 * ambient noise; what it cannot do is know you started walking, because it
 * has no motion sensor. So the two compose: Smart handles "how noisy is it
 * here", these rules handle "what are you doing".
 *
 * Defaults encode the obvious intent rather than leaving everything off:
 * still means you probably want quiet and can let the firmware judge how
 * much, walking and running mean you want to hear traffic. Switching the
 * feature on therefore does something sensible immediately.
 */
object AdaptiveRules {

    private const val FILE = "realbuds_adaptive"
    private const val KEY_ENABLED = "adaptive_enabled"
    private const val KEY_PREFIX_MODE = "mode_"
    private const val KEY_PREFIX_ON = "on_"

    private val defaults = mapOf(
        Motion.STILL to (AncMode.ANC_SMART to true),
        Motion.WALKING to (AncMode.TRANSPARENCY to true),
        Motion.RUNNING to (AncMode.TRANSPARENCY to true),
    )

    /** Activities we can actually detect, in the order the UI shows them. */
    val order = listOf(Motion.STILL, Motion.WALKING, Motion.RUNNING)

    fun isEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ENABLED, false)

    fun setEnabled(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, on).apply()
    }

    fun rules(ctx: Context): List<AdaptiveRule> = order.map { act ->
        val (defMode, defOn) = defaults[act] ?: (AncMode.OFF to false)
        val wire = prefs(ctx).getInt(KEY_PREFIX_MODE + act.name, defMode.wire)
        AdaptiveRule(
            activity = act,
            mode = AncMode.byWire(wire) ?: defMode,
            enabled = prefs(ctx).getBoolean(KEY_PREFIX_ON + act.name, defOn),
        )
    }

    fun ruleFor(ctx: Context, act: Motion): AdaptiveRule? =
        rules(ctx).firstOrNull { it.activity == act && it.enabled }

    fun setMode(ctx: Context, act: Motion, mode: AncMode) {
        prefs(ctx).edit().putInt(KEY_PREFIX_MODE + act.name, mode.wire).apply()
    }

    fun setRuleEnabled(ctx: Context, act: Motion, on: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_PREFIX_ON + act.name, on).apply()
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
