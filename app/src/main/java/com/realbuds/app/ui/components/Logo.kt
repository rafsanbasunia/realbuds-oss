package com.realbuds.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.realbuds.app.ui.theme.Accent500

/**
 * RealBuds mark.
 *
 * Constructed on a 100-unit grid rather than eyeballed, so proportions hold at
 * any size:
 *
 *   - two driver circles at (30,34) and (70,34), r = 15.5, on a shared axis
 *   - each with a punched centre, so it reads as a bud rather than a dot
 *   - stems sweeping down and inward, ending at (43,76) and (57,76)
 *   - a 14-unit gap between the tips, forming the negative-space apex of an M
 *
 * Symmetric about x = 50, which keeps it centred in a square launcher icon and
 * legible down to 24dp in the app bar.
 */
private const val GRID = 100f

/**
 * The punched centre is the page ground, not a fixed colour, so the hole
 * reads as a hole in both themes. Hardcoding a dark violet here (from the
 * old aurora palette) made it a dark ring on the light background.
 */

@Composable
fun RealBudsLogo(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    animated: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "logo")
    val shimmer by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer",
    )
    val phase = if (animated) shimmer else 0.5f

    // One accent, so a "gradient" of three identical stops is pointless.
    // The shimmer now sweeps a brighter highlight across the mark instead,
    // which still reads as motion but with a single colour.
    val accent = Accent500
    val ground = MaterialTheme.colorScheme.background
    val highlight = accent.copy(alpha = 0.55f)

    Canvas(modifier.size(size)) {
        drawLogo(
            Brush.linearGradient(
                colors = listOf(accent, highlight, accent),
                start = Offset(-this.size.width * phase, 0f),
                end = Offset(this.size.width * (1f + phase), this.size.height),
            ),
            punch = ground,
        )
    }
}

/** Flat single-colour variant for monochrome contexts. */
@Composable
fun RealBudsGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    color: Color = Accent500,
    punch: Color = MaterialTheme.colorScheme.background,
) {
    Canvas(modifier.size(size)) {
        drawLogo(Brush.linearGradient(listOf(color, color)), punch)
    }
}

private fun DrawScope.drawLogo(brush: Brush, punch: Color) {
    val u = size.minDimension / GRID
    fun at(x: Float, y: Float) = Offset(x * u, y * u)

    val r = 15.5f * u
    val stem = 9.6f * u

    drawPath(
        Path().apply {
            moveTo(30f * u, 34f * u)
            cubicTo(31f * u, 60f * u, 36f * u, 72f * u, 43f * u, 76f * u)
        },
        brush,
        style = Stroke(width = stem, cap = StrokeCap.Round),
    )
    drawPath(
        Path().apply {
            moveTo(70f * u, 34f * u)
            cubicTo(69f * u, 60f * u, 64f * u, 72f * u, 57f * u, 76f * u)
        },
        brush,
        style = Stroke(width = stem, cap = StrokeCap.Round),
    )

    drawCircle(brush, r, at(30f, 34f))
    drawCircle(brush, r, at(70f, 34f))

    drawCircle(punch, r * 0.40f, at(30f, 34f))
    drawCircle(punch, r * 0.40f, at(70f, 34f))
}
