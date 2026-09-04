package com.realbuds.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.realbuds.app.ui.theme.BudsColors
import com.realbuds.app.ui.theme.LocalGlass

/**
 * Battery gauge. The arc is drawn with a gradient sweep and sits over a faint
 * track, with a soft bloom behind it so it reads as emitting light rather than
 * being a flat stroke.
 */
@Composable
fun BatteryRing(
    label: String,
    level: Int?,
    charging: Boolean,
    modifier: Modifier = Modifier,
) {
    val known = level != null
    val target = (level ?: 0).coerceIn(0, 100) / 100f
    val pct by animateFloatAsState(target, spring(stiffness = 120f), label = "battery")
    val ringColor = if (known) BudsColors.forLevel(level) else Color(0x33FFFFFF)
    val g = LocalGlass.current

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(86.dp)) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2 + 2.dp.toPx()
                val arc = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = g.stroke,
                    startAngle = 130f,
                    sweepAngle = 280f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                if (known && pct > 0f) {
                    drawArc(
                        color = ringColor.copy(alpha = 0.28f),
                        startAngle = 130f,
                        sweepAngle = 280f * pct,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arc,
                        style = Stroke(stroke * 2.6f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(ringColor.copy(alpha = 0.55f), ringColor),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                        ),
                        startAngle = 130f,
                        sweepAngle = 280f * pct,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arc,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        if (known) "$level" else "—",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (known) {
                        Text(
                            "%",
                            style = com.realbuds.app.ui.theme.UnitStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, start = 2.dp),
                        )
                    }
                }
                if (charging) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = "Charging",
                        tint = BudsColors.charging,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Large glass tile for the three noise-control modes. */
@Composable
fun ModeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(240), label = "tilefg",
    )
    PressableGlass(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        selected = selected,
    ) {
        Column(
            Modifier.padding(vertical = 18.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/** Title + supporting text + switch, on a tappable glass row. */
@Composable
fun ToggleRow(
    title: String,
    description: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    PressableGlass(
        onClick = { if (enabled) onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = Color.Transparent,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

/**
 * Card-style selection row.
 *
 * Guidance for touch UI is a 48dp minimum target (WCAG 2.2 asks 24, Android
 * and iOS both recommend more); the previous 44dp row with an 18dp dot was
 * under that. This row is 64dp with the whole surface tappable.
 *
 * Selection is signalled three ways — fill, border, and a check mark — so it
 * never depends on colour alone.
 */
@Composable
fun ChoiceRow(
    title: String,
    description: String?,
    selected: Boolean,
    accent: Color? = null,
    leading: ImageVector? = null,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    val tone = accent ?: MaterialTheme.colorScheme.primary
    val check by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "check",
    )

    PressableGlass(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        selected = selected,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (selected) tone.copy(alpha = 0.20f)
                            else Color.White.copy(alpha = 0.06f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        leading,
                        contentDescription = null,
                        tint = if (selected) tone
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (description != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (trailing != null) {
                Text(
                    trailing,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
            }

            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) tone else Color.White.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center,
            ) {
                if (check > 0.01f) {
                    Canvas(Modifier.size(14.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.18f, h * 0.52f)
                            lineTo(w * 0.42f, h * 0.75f)
                            lineTo(w * 0.84f, h * 0.26f)
                        }
                        val measure = PathMeasure().apply { setPath(path, false) }
                        val dst = Path()
                        measure.getSegment(0f, measure.length * check, dst, true)
                        drawPath(
                            dst,
                            color = Color(0xFF0B0812),
                            style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
    }
}

/** Live connection pill for the header. */
@Composable
fun StatusPill(label: String, color: Color) {
    val g = LocalGlass.current
    Row(
        Modifier
            .clip(CircleShape)
            .background(g.fill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Monospace hex/log line. */
@Composable
fun MonoText(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = color,
        modifier = modifier,
    )
}
