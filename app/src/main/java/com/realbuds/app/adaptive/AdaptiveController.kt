package com.realbuds.app.adaptive

import android.content.Context
import com.realbuds.app.proto.AncMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives ANC from detected activity.
 *
 * Three behaviours matter more than the mapping itself:
 *
 *  - **Debounce.** Raw cadence flickers between STILL and WALKING at the
 *    start and end of a walk. Switching ANC on every flicker would be worse
 *    than not switching at all, so a new activity must hold for
 *    [HOLD_MS] before it is acted on.
 *  - **Write once per transition.** Re-sending the same mode every tick
 *    would spam the SPP link and fight any manual change the user makes.
 *  - **Yield to the user.** A manual mode change pauses adaptation until the
 *    activity actually changes again, so the app never immediately overrides
 *    a deliberate tap.
 */
class AdaptiveController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val monitor: ActivityMonitor,
    private val applyMode: suspend (AncMode) -> Unit,
) {
    private val _lastApplied = MutableStateFlow<AncMode?>(null)
    val lastApplied: StateFlow<AncMode?> = _lastApplied.asStateFlow()

    private val _current = MutableStateFlow(Motion.UNKNOWN)
    val current: StateFlow<Motion> = _current.asStateFlow()

    private var job: Job? = null

    /** Set when the user picks a mode by hand; cleared on the next change. */
    private var suppressedFor: Motion? = null

    private var candidate: Motion = Motion.UNKNOWN
    private var candidateSince = 0L

    val available: Boolean get() = monitor.available

    fun start() {
        if (job != null) return
        monitor.start()
        job = scope.launch {
            while (isActive) {
                // The step detector only fires on a step, so poll to let an
                // absence of steps decay to STILL.
                monitor.tick()
                evaluate(monitor.activity.value)
                delay(TICK_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        monitor.stop()
        candidate = Motion.UNKNOWN
        suppressedFor = null
        _current.value = Motion.UNKNOWN
        _lastApplied.value = null
    }

    /**
     * Called when the user changes mode by hand. Adaptation then holds off
     * until the activity changes, so a deliberate choice is not immediately
     * undone by the next tick.
     */
    fun onManualOverride() {
        suppressedFor = _current.value
        _lastApplied.value = null
    }

    private suspend fun evaluate(raw: Motion) {
        if (raw == Motion.UNKNOWN) return
        val now = System.currentTimeMillis()

        if (raw != candidate) {
            candidate = raw
            candidateSince = now
            return
        }
        // Not held long enough to be believed yet.
        if (now - candidateSince < HOLD_MS) return

        val settled = candidate
        if (settled != _current.value) {
            _current.value = settled
            // A genuine activity change lifts a manual override.
            if (suppressedFor != null && suppressedFor != settled) {
                suppressedFor = null
            }
        }
        if (suppressedFor == settled) return

        val rule = AdaptiveRules.ruleFor(context, settled) ?: return
        if (_lastApplied.value == rule.mode) return

        applyMode(rule.mode)
        _lastApplied.value = rule.mode
    }

    private companion object {
        const val TICK_MS = 3_000L
        /** How long an activity must persist before it is acted on. */
        const val HOLD_MS = 9_000L
    }
}
