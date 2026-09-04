package com.realbuds.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.realbuds.app.proto.CustomEq
import com.realbuds.app.ui.theme.LocalGlass
import com.realbuds.app.ui.theme.SignalLime
import kotlin.math.roundToInt

/** 62 -> "62", 1000 -> "1K", 16000 -> "16K". */
private fun freqLabel(hz: Int): String = when {
    hz >= 1000 -> {
        val k = hz / 1000.0
        if (k % 1.0 == 0.0) "${k.toInt()}K" else "${k}K"
    }
    else -> "$hz"
}

/**
 * Vertical curve equaliser.
 *
 * Each band is a node on a vertical dB axis, joined by a smoothed curve with a
 * soft fill underneath. Drag anywhere in the plot to grab the nearest band, so
 * you never have to hit a small target precisely; tap to set a band directly.
 */
@Composable
fun EqEditor(
    eq: CustomEq,
    onChange: (CustomEq) -> Unit,
    onSave: (CustomEq) -> Unit,
    onReset: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var draft by remember(eq.eqId, eq.bands) { mutableStateOf(eq) }
    var dragging by remember { mutableStateOf(-1) }
    val dirty = draft.bands != eq.bands || draft.name != eq.name
    val n = draft.bands.size

    val g = LocalGlass.current
    val guide = if (g.isDark) Color(0x1FFFFFFF) else Color(0x1F000000)
    // Hoisted: the draw scope below is not a composable context.
    val accent = SignalLime
    val knobCore = if (g.isDark) Color(0xFF0B0812) else Color(0xFFFFFFFF)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(g.fillStrong)
                .pointerInput(n, draft.minDb, draft.maxDb) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val step = size.width / (n - 1).coerceAtLeast(1)
                            dragging = (pos.x / step).roundToInt().coerceIn(0, n - 1)
                        },
                        onDragEnd = { dragging = -1 },
                        onDragCancel = { dragging = -1 },
                    ) { change, _ ->
                        change.consume()
                        val step = size.width / (n - 1).coerceAtLeast(1)
                        val i = if (dragging >= 0) dragging
                                else (change.position.x / step).roundToInt().coerceIn(0, n - 1)
                        dragging = i
                        val f = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        val range = (draft.maxDb - draft.minDb).toFloat()
                        val db = (draft.minDb + f * range).roundToInt()
                            .coerceIn(draft.minDb, draft.maxDb)
                        draft = draft.withDb(i, db)
                        onChange(draft)
                    }
                }
                .pointerInput(n, draft.minDb, draft.maxDb) {
                    detectTapGestures { pos ->
                        val step = size.width / (n - 1).coerceAtLeast(1)
                        val i = (pos.x / step).roundToInt().coerceIn(0, n - 1)
                        val f = 1f - (pos.y / size.height).coerceIn(0f, 1f)
                        val range = (draft.maxDb - draft.minDb).toFloat()
                        draft = draft.withDb(
                            i,
                            (draft.minDb + f * range).roundToInt()
                                .coerceIn(draft.minDb, draft.maxDb),
                        )
                        onChange(draft)
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize().padding(vertical = 14.dp)) {
                if (n < 2) return@Canvas
                val stepX = size.width / (n - 1)
                val range = (draft.maxDb - draft.minDb).toFloat()

                fun yFor(db: Int): Float = size.height * (1f - (db - draft.minDb) / range)

                val pts = draft.bands.mapIndexed { i, b -> Offset(stepX * i, yFor(b.db)) }

                pts.forEach { p ->
                    drawLine(
                        color = guide,
                        start = Offset(p.x, 0f),
                        end = Offset(p.x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 6.dp.toPx())
                        ),
                    )
                }

                val zeroY = yFor(0)
                drawLine(
                    color = guide,
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = 1.dp.toPx(),
                )

                val curve = smoothPath(pts)

                drawPath(
                    Path().apply {
                        addPath(curve)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    },
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.20f), Color.Transparent)
                    ),
                )

                drawPath(
                    curve,
                    color = accent,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )

                pts.forEachIndexed { i, p ->
                    val active = i == dragging
                    if (active) drawCircle(accent.copy(alpha = 0.20f), 14.dp.toPx(), p)
                    drawCircle(knobCore, (if (active) 7.5f else 6f).dp.toPx(), p)
                    drawCircle(accent, (if (active) 5.5f else 4f).dp.toPx(), p)
                }
            }

            if (dragging in draft.bands.indices) {
                val b = draft.bands[dragging]
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${freqLabel(b.freqHz)} · ${if (b.db > 0) "+" else ""}${b.db} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalLime,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth()) {
            draft.bands.forEachIndexed { i, b ->
                Text(
                    freqLabel(b.freqHz),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = when (i) {
                        0 -> TextAlign.Start
                        draft.bands.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        OutlinedTextField(
            value = draft.name,
            onValueChange = { v ->
                draft = draft.copy(name = v.take(20))
            },
            label = { Text("Name") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor = accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlowButton(
                text = if (dirty) "Save" else "Saved",
                onClick = { onSave(draft) },
                enabled = dirty,
            )
            PressableGlass(
                onClick = {
                    draft = draft.copy(bands = draft.bands.map { it.copy(db = 0) })
                    onChange(draft)
                },
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    "Flat",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (onDelete != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDelete,
                        )
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/**
 * Horizontal-tangent cubic between each pair of nodes.
 *
 * Control points share the midpoint x and take their neighbour's y, so the
 * curve flattens as it arrives at every node and never overshoots past a
 * band's set value — which a Catmull-Rom through all points would do.
 */
private fun smoothPath(pts: List<Offset>): Path {
    val p = Path()
    if (pts.isEmpty()) return p
    p.moveTo(pts[0].x, pts[0].y)
    for (i in 0 until pts.size - 1) {
        val a = pts[i]
        val b = pts[i + 1]
        val midX = (a.x + b.x) / 2f
        p.cubicTo(midX, a.y, midX, b.y, b.x, b.y)
    }
    return p
}
