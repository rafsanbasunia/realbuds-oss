package com.realbuds.app.adaptive

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the wearer is doing, inferred from step cadence.
 *
 * Deliberately uses the platform step detector rather than Google's Activity
 * Recognition API. The Play Services dependency would pull a proprietary blob
 * into an otherwise dependency-free app — the exact thing that gets a project
 * rejected from F-Droid — and TYPE_STEP_DETECTOR is present on this hardware
 * (and virtually every phone since 2014) with a wakeup variant, so it keeps
 * working with the screen off.
 *
 * The trade-off is honest: cadence cannot distinguish "in a vehicle" from
 * "sitting still", because neither produces steps. Sony's version knows the
 * difference because Google's API fuses accelerometer and location. So this
 * offers STILL / WALKING / RUNNING and nothing more, rather than pretending
 * to a precision it does not have.
 */
enum class Motion { STILL, WALKING, RUNNING, UNKNOWN;

    val label: String get() = when (this) {
        STILL -> "Still"
        WALKING -> "Walking"
        RUNNING -> "Running"
        UNKNOWN -> "Unknown"
    }
}

class ActivityMonitor(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    // Prefer the wakeup detector so activity is still tracked with the screen
    // off; fall back to the non-wakeup one where that is all there is.
    private val stepSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR, true)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _activity = MutableStateFlow(Motion.UNKNOWN)
    val activity: StateFlow<Motion> = _activity.asStateFlow()

    /** True when the device has no step detector at all. */
    val available: Boolean get() = stepSensor != null

    /** Timestamps (ms) of recent steps, newest last. */
    private val steps = ArrayDeque<Long>()

    private var running = false

    fun start() {
        val s = stepSensor ?: return
        if (running) return
        running = true
        // SENSOR_DELAY_NORMAL is plenty: we are measuring cadence over seconds,
        // not reacting to individual steps.
        sensorManager?.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(this)
        steps.clear()
        _activity.value = Motion.UNKNOWN
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_DETECTOR) return
        val now = System.currentTimeMillis()
        steps.addLast(now)
        prune(now)
        _activity.value = classify(now)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Drop steps older than the window so cadence reflects the present. */
    private fun prune(now: Long) {
        while (steps.isNotEmpty() && now - steps.first() > WINDOW_MS) {
            steps.removeFirst()
        }
    }

    /**
     * Steps per minute over the window.
     *
     * Thresholds come from gait research rather than taste: normal walking
     * cadence sits near 100 spm and running starts around 140. Two steps is
     * the minimum for any interval at all.
     */
    private fun classify(now: Long): Motion {
        prune(now)
        if (steps.size < 2) return Motion.STILL
        val spanMs = now - steps.first()
        if (spanMs <= 0L) return Motion.STILL
        val spm = steps.size * 60_000.0 / spanMs
        return when {
            spm >= RUN_SPM -> Motion.RUNNING
            spm >= WALK_SPM -> Motion.WALKING
            else -> Motion.STILL
        }
    }

    /**
     * Call periodically so that *absence* of steps decays to STILL. The step
     * detector only fires on a step, so without this a walk that stops would
     * leave the state stuck on WALKING forever.
     */
    fun tick() {
        val now = System.currentTimeMillis()
        prune(now)
        if (steps.isEmpty()) _activity.value = Motion.STILL
        else _activity.value = classify(now)
    }

    private companion object {
        const val WINDOW_MS = 8_000L
        const val WALK_SPM = 45.0
        const val RUN_SPM = 130.0
    }
}
