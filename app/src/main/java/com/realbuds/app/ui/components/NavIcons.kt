package com.realbuds.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Purpose-drawn nav icons that animate on selection.
 *
 * Drawn rather than using Material icons for two reasons: the stock set has no
 * good "sound wave" or "earbud settings" glyph, and a Canvas icon can animate
 * its own geometry — bars growing, a slider knob sliding — which an
 * AnimatedVectorDrawable would need a separate XML per state to achieve.
 *
 * Every icon takes a 0..1 [progress] so the caller can drive it from the same
 * spring that moves the nav pill, keeping icon and indicator in sync.
 */

private const val GRID = 24f

@Composable
fun NavIconSound(active: Boolean, tint: Color, size: Dp = 20.dp) {
    val p by animateFloatAsState(
        if (active) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 320f),
        label = "soundbars",
    )
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / GRID
        val heights = listOf(0.34f, 0.70f, 1.0f, 0.62f, 0.30f)
        val w = 2.4f * u
        heights.forEachIndexed { i, hFrac ->
            val idle = 0.36f
            val h = (idle + (hFrac - idle) * p) * 15f * u
            val x = (4.2f + i * 3.9f) * u
            drawLine(
                color = tint,
                start = Offset(x, 12 * u + h / 2),
                end = Offset(x, 12 * u - h / 2),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun NavIconDevice(active: Boolean, tint: Color, size: Dp = 20.dp) {
    val p by animateFloatAsState(
        if (active) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "sliders",
    )
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / GRID
        val rows = listOf(6f, 12f, 18f)
        val from = listOf(9f, 15f, 11f)
        val to = listOf(15f, 8f, 16f)
        rows.forEachIndexed { i, y ->
            drawLine(
                color = tint.copy(alpha = 0.45f),
                start = Offset(4 * u, y * u),
                end = Offset(20 * u, y * u),
                strokeWidth = 1.9f * u,
                cap = StrokeCap.Round,
            )
            val kx = (from[i] + (to[i] - from[i]) * p) * u
            drawCircle(tint, 2.7f * u, Offset(kx, y * u))
        }
    }
}

@Composable
fun NavIconConsole(active: Boolean, tint: Color, size: Dp = 20.dp) {
    val p by animateFloatAsState(
        if (active) 1f else 0f,
        spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "console",
    )
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / GRID
        val stroke = 1.9f * u

        drawRoundRect(
            color = tint.copy(alpha = 0.5f),
            topLeft = Offset(3 * u, 4.5f * u),
            size = Size(18 * u, 15 * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4 * u, 4 * u),
            style = Stroke(stroke),
        )

        val cx = (7f + 1.2f * p) * u
        drawPath(
            Path().apply {
                moveTo(cx, 9.5f * u)
                lineTo(cx + 3f * u, 12 * u)
                lineTo(cx, 14.5f * u)
            },
            color = tint,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )

        drawLine(
            color = tint,
            start = Offset(13.5f * u, 14.5f * u),
            end = Offset((13.5f + 3.5f * p) * u, 14.5f * u),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun NavIconSettings(active: Boolean, tint: Color, size: Dp = 20.dp) {
    val p by animateFloatAsState(
        if (active) 1f else 0f,
        spring(dampingRatio = 0.62f, stiffness = 260f),
        label = "gear",
    )
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / GRID
        val c = Offset(12 * u, 12 * u)
        val stroke = 1.9f * u
        val teeth = 8
        val rIn = 6.4f * u
        val rOut = 9.2f * u
        val spin = p * (360f / teeth / 2f)

        repeat(teeth) { i ->
            val a = Math.toRadians((i * 360f / teeth + spin).toDouble())
            val ca = kotlin.math.cos(a).toFloat()
            val sa = sin(a).toFloat()
            drawLine(
                color = tint,
                start = Offset(c.x + ca * rIn, c.y + sa * rIn),
                end = Offset(c.x + ca * rOut, c.y + sa * rOut),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(tint, rIn, c, style = Stroke(stroke))
        drawCircle(tint.copy(alpha = 0.35f + 0.65f * p), 2.3f * u, c)
    }
}

@Composable
fun NavIconTouch(active: Boolean, tint: Color, size: Dp = 20.dp) {
    val p by animateFloatAsState(
        if (active) 1f else 0f,
        spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "touch",
    )
    Canvas(Modifier.size(size)) {
        val u = this.size.minDimension / GRID
        val stroke = 1.9f * u
        val cx = 11f * u
        val cy = 13.5f * u

        repeat(2) { i ->
            val r = (4.5f + i * 2.6f + p * 1.6f) * u
            drawArc(
                color = tint.copy(alpha = (0.5f - i * 0.18f) * (0.35f + 0.65f * p)),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(stroke * 0.8f, cap = StrokeCap.Round),
            )
        }

        val press = p * 0.9f * u
        drawRoundRect(
            color = tint,
            topLeft = Offset(8.2f * u, 10.2f * u + press),
            size = Size(5.6f * u, 7.4f * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.8f * u, 2.8f * u),
            style = Stroke(stroke),
        )
        drawLine(
            tint,
            Offset(11f * u, 17.6f * u + press),
            Offset(11f * u, 20.5f * u),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
