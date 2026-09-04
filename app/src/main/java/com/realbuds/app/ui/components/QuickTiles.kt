package com.realbuds.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.realbuds.app.ui.theme.Accent500
import com.realbuds.app.ui.theme.LocalGlass
import com.realbuds.app.ui.theme.OnAccent
import kotlin.math.sin

/**
 * A feature the quick strip can toggle, with a drawn glyph.
 *
 * Glyphs are drawn rather than taken from the Material set because most of
 * these have no good stock icon (there is no "de-wind" or "party mode"
 * glyph), and a Canvas icon can carry its own active state — waves that
 * grow, a shield that fills — which communicates on/off without relying on
 * the tile's fill colour alone.
 */
enum class QuickGlyph { GAME, WIND, SPATIAL, VOICE, MULTI, HD, BASS, PLAY_PAUSE, POWER, CALL, GENERIC }

/**
 * One row of four fixed tiles, sized to split the width evenly.
 *
 * Deliberately not scrollable and not sorted: a quick-settings row is only
 * useful if a given control is always in the same place. Showing every
 * supported feature turned it into a list you had to read; four in fixed
 * positions can be hit from muscle memory.
 */
@Composable
fun QuickTileStrip(
    items: List<QuickTile>,
    onToggle: (QuickTile, Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { t ->
            QuickTileCard(t, Modifier.weight(1f)) { onToggle(t, !t.enabled) }
        }
    }
}

data class QuickTile(
    val id: Int,
    val label: String,
    val glyph: QuickGlyph,
    val enabled: Boolean,
)

@Composable
private fun QuickTileCard(
    tile: QuickTile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(18.dp)

    val bg by animateColorAsState(
        if (tile.enabled) Accent500 else g.fillStrong,
        tween(260), label = "qtbg",
    )
    val fg by animateColorAsState(
        if (tile.enabled) OnAccent else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(260), label = "qtfg",
    )
    val lift by animateFloatAsState(
        if (tile.enabled) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "qtlift",
    )

    Column(
        modifier
            .clip(shape)
            .background(bg)
            .noRipple(onClick)
            .padding(horizontal = 6.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(26.dp)) {
                drawGlyph(tile.glyph, fg, lift)
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            tile.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (tile.enabled) FontWeight.SemiBold else FontWeight.Normal,
            color = fg,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
        )
    }
}

/** Grid is 24x24, matching the Material icon box so sizes agree. */
private fun DrawScope.drawGlyph(glyph: QuickGlyph, tint: Color, on: Float) {
    val u = size.minDimension / 24f
    val w = 1.9f * u
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, sw: Float = w, a: Float = 1f) =
        drawLine(
            tint.copy(alpha = a), Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u),
            strokeWidth = sw, cap = StrokeCap.Round,
        )

    when (glyph) {
        QuickGlyph.GAME -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(2.5f * u, 7.5f * u),
                size = Size(19 * u, 9 * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.5f * u, 4.5f * u),
                style = Stroke(w),
            )
            line(6f, 12f, 9f, 12f)
            line(7.5f, 10.5f, 7.5f, 13.5f)
            drawCircle(tint, 1.4f * u, Offset(16f * u, 10.8f * u))
            drawCircle(tint, 1.4f * u, Offset(18.5f * u, 13.2f * u))
        }
        QuickGlyph.WIND -> {
            val e = 1f + 0.35f * on
            line(3f, 8f, (3f + 10f * e).coerceAtMost(21f), 8f)
            line(3f, 12f, (3f + 13f * e).coerceAtMost(21f), 12f)
            line(3f, 16f, (3f + 7f * e).coerceAtMost(21f), 16f)
        }
        QuickGlyph.SPATIAL -> {
            drawCircle(tint, 3.4f * u, Offset(12 * u, 12 * u), style = Stroke(w))
            for (i in 0..1) {
                val r = (7f + i * 3f) * u
                drawArc(
                    color = tint.copy(alpha = 0.55f + 0.45f * on),
                    startAngle = -50f + i * 18f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(12 * u - r, 12 * u - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(w * 0.85f, cap = StrokeCap.Round),
                )
            }
        }
        QuickGlyph.VOICE -> {
            val amp = 2.2f + 4.2f * on
            for (i in 0..4) {
                val x = 4f + i * 4f
                val h = amp * (if (i % 2 == 0) 1f else 0.62f) + 1.6f
                line(x, 12f - h, x, 12f + h, w * 1.15f)
            }
        }
        QuickGlyph.MULTI -> {
            drawRoundRect(
                color = tint.copy(alpha = 0.5f),
                topLeft = Offset(3 * u, 5 * u),
                size = Size(11 * u, 13 * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * u, 2.5f * u),
                style = Stroke(w),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(10 * u, 8 * u),
                size = Size(11 * u, 13 * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * u, 2.5f * u),
                style = Stroke(w),
            )
        }
        QuickGlyph.HD -> {
            line(4.5f, 17f, 4.5f, 13f, w * 1.3f)
            line(9.5f, 17f, 9.5f, 10f, w * 1.3f)
            line(14.5f, 17f, 14.5f, 7f, w * 1.3f)
            line(19.5f, 17f, 19.5f, 4f, w * 1.3f, a = 0.3f + 0.7f * on)
        }
        QuickGlyph.BASS -> {
            val amp = 2.5f + 3.5f * on
            val path = androidx.compose.ui.graphics.Path()
            var first = true
            var x = 3f
            while (x <= 21f) {
                val y = 12f + sin((x - 3f) / 18f * 2f * Math.PI).toFloat() * amp
                if (first) { path.moveTo(x * u, y * u); first = false }
                else path.lineTo(x * u, y * u)
                x += 0.75f
            }
            drawPath(path, tint, style = Stroke(w, cap = StrokeCap.Round))
        }
        QuickGlyph.PLAY_PAUSE -> {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(4 * u, 6.5f * u); lineTo(4 * u, 17.5f * u); lineTo(12 * u, 12 * u); close()
            }
            drawPath(p, tint)
            line(15.5f, 7.5f, 15.5f, 16.5f, w * 1.4f)
            line(19.5f, 7.5f, 19.5f, 16.5f, w * 1.4f)
        }
        QuickGlyph.POWER -> {
            drawArc(
                color = tint, startAngle = 130f, sweepAngle = 260f, useCenter = false,
                topLeft = Offset(6 * u, 8 * u), size = Size(12 * u, 12 * u),
                style = Stroke(w, cap = StrokeCap.Round),
            )
            line(12f, 3.5f, 12f, 9.5f)
        }
        QuickGlyph.CALL -> {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(5 * u, 6 * u)
                quadraticBezierTo(4 * u, 14 * u, 12 * u, 19 * u)
                quadraticBezierTo(15 * u, 20f * u, 16 * u, 16.5f * u)
                lineTo(12.5f * u, 14.5f * u)
                lineTo(10.5f * u, 16f * u)
                quadraticBezierTo(7.5f * u, 13f * u, 8.5f * u, 10.5f * u)
                lineTo(9.5f * u, 7f * u)
                close()
            }
            drawPath(p, tint)
            drawArc(
                color = tint.copy(alpha = 0.35f + 0.65f * on),
                startAngle = -70f, sweepAngle = 60f, useCenter = false,
                topLeft = Offset(11 * u, 3 * u), size = Size(10 * u, 10 * u),
                style = Stroke(w * 0.9f, cap = StrokeCap.Round),
            )
        }
        QuickGlyph.GENERIC -> {
            drawCircle(tint, 6.5f * u, Offset(12 * u, 12 * u), style = Stroke(w))
            drawCircle(tint, 2f * u, Offset(12 * u, 12 * u))
        }
    }
}
