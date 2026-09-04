package com.realbuds.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.realbuds.app.ui.theme.Accent500
import com.realbuds.app.ui.theme.LocalGlass
import kotlin.math.roundToInt

/**
 * Discrete stepped slider: a track of dots with the filled run up to the
 * current step.
 *
 * Dots rather than a continuous bar because the value is a small integer
 * range (-5..+5 on these buds) and showing every stop makes the granularity
 * obvious — you can see there are eleven positions, not guess at them.
 *
 * [onCommit] fires on release, not on every drag pixel:
 * writing per pixel would flood the SPP link. [value] is echoed locally while
 * dragging so the thumb tracks the finger without waiting for the device.
 */
@Composable
fun StepSlider(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    enabled: Boolean = true,
    onCommit: (Int) -> Unit,
) {
    val steps = (max - min).coerceAtLeast(1)
    val g = LocalGlass.current

    var drag by remember(value, min, max) { mutableStateOf<Int?>(null) }
    val shown = (drag ?: value).coerceIn(min, max)

    var widthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    fun stepFor(x: Float): Int {
        if (widthPx <= 0) return shown
        val frac = (x / widthPx).coerceIn(0f, 1f)
        return min + (frac * steps).roundToInt()
    }

    val trackColor = if (g.isDark) Color(0x24FFFFFF) else Color(0x1F000000)
    val accent = Accent500
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) onSurface else muted,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (shown > 0) "+$shown" else "$shown",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (!enabled) muted else if (shown == 0) muted else accent,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .then(
                    if (!enabled) Modifier else Modifier.pointerInput(steps, min, max) {
                        detectDragGestures(
                            onDragStart = { pos -> drag = stepFor(pos.x) },
                            onDragEnd = {
                                drag?.let(onCommit)
                                drag = null
                            },
                            onDragCancel = { drag = null },
                        ) { change, _ -> drag = stepFor(change.position.x) }
                    }
                )
                .then(
                    if (!enabled) Modifier else Modifier.pointerInput(steps, min, max) {
                        detectTapGestures { pos -> onCommit(stepFor(pos.x)) }
                    }
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Canvas(Modifier.fillMaxWidth().height(36.dp)) {
                widthPx = size.width.toInt()
                val cy = size.height / 2f
                val r = with(density) { 4.dp.toPx() }
                val activeR = with(density) { 7.dp.toPx() }
                val stroke = with(density) { 2.5f.dp.toPx() }
                val inset = activeR
                val usable = (size.width - inset * 2).coerceAtLeast(1f)
                val idx = shown - min

                drawLine(
                    trackColor,
                    Offset(inset, cy),
                    Offset(size.width - inset, cy),
                    strokeWidth = stroke,
                )
                if (idx > 0) {
                    drawLine(
                        if (enabled) onSurface else muted,
                        Offset(inset, cy),
                        Offset(inset + usable * idx / steps, cy),
                        strokeWidth = stroke,
                    )
                }
                for (i in 0..steps) {
                    val x = inset + usable * i / steps
                    val filled = i <= idx
                    drawCircle(
                        color = when {
                            !enabled -> muted
                            filled -> onSurface
                            else -> trackColor
                        },
                        radius = if (i == idx) activeR else r,
                        center = Offset(x, cy),
                    )
                    if (i == idx && enabled) {
                        drawCircle(accent, activeR * 0.55f, Offset(x, cy))
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth()) {
            Text("$min", style = MaterialTheme.typography.labelSmall, color = muted)
            Spacer(Modifier.weight(1f))
            Text("0", style = MaterialTheme.typography.labelSmall, color = muted)
            Spacer(Modifier.weight(1f))
            Text("+$max", style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}
