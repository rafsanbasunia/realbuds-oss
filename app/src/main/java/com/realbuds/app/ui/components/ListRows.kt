package com.realbuds.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.realbuds.app.ui.theme.Accent500
import com.realbuds.app.ui.theme.BudsColors
import com.realbuds.app.ui.theme.LocalGlass
import com.realbuds.app.ui.theme.OnAccent

/**
 * Plain list row inside a card.
 *
 * No per-row surface, no border, no fill — rows are separated by spacing and
 * marked by a check when selected. That is what keeps a list of six options
 * from looking like six competing buttons.
 */
@Composable
fun PlainRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean = false,
    chevron: Boolean = false,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        if (selected) Accent500 else MaterialTheme.colorScheme.onSurface,
        tween(200), label = "rowtint",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .noRipple(onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = tint,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            selected && !chevron -> Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Accent500,
                modifier = Modifier.size(19.dp),
            )
            chevron -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/**
 * Circular mode button, as in the reference's Ambient Sound row: a filled
 * accent disc when active, a dark disc when not.
 */
@Composable
fun CircleMode(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (selected) Accent500 else LocalGlass.current.fillStrong,
        tween(220), label = "circlebg",
    )
    val fg by animateColorAsState(
        if (selected) OnAccent else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220), label = "circlefg",
    )
    val scale by animateFloatAsState(
        if (selected) 1f else 0.94f,
        spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "circlescale",
    )

    Column(
        Modifier.width(96.dp).noRipple(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            Modifier
                .size((58 * scale).dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(24.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** Compact L / R / case battery readout for the device header. */
@Composable
fun BatteryChip(tag: String, level: Int?, charging: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(LocalGlass.current.fillStrong),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                tag,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(6.dp))
        val reading = level?.takeIf { it in 1..100 }
        Text(
            if (reading != null) "$reading%" else "--",
            style = MaterialTheme.typography.labelMedium,
            color = if (reading != null) BudsColors.forLevel(reading)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (charging) {
            Spacer(Modifier.width(3.dp))
            Icon(
                Icons.Default.Bolt,
                contentDescription = "Charging",
                tint = Accent500,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * Flowing grid of selectable chips.
 *
 * The reference uses this for EQ presets instead of a vertical list: eight
 * options fit in three rows rather than eight full-width rows, so the section
 * stays glanceable. Layout wraps manually to avoid the experimental FlowRow.
 */
@Composable
fun ChipGrid(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    androidx.compose.ui.layout.Layout(
        content = {
            items.forEachIndexed { i, label ->
                Chip(label, i == selectedIndex) { onSelect(i) }
            }
        },
    ) { measurables, constraints ->
        val gap = 8.dp.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        var x = 0
        var y = 0
        var rowH = 0
        val pos = mutableListOf<Pair<Int, Int>>()
        placeables.forEach { p ->
            if (x + p.width > constraints.maxWidth && x > 0) {
                x = 0
                y += rowH + gap
                rowH = 0
            }
            pos.add(x to y)
            x += p.width + gap
            rowH = maxOf(rowH, p.height)
        }
        layout(constraints.maxWidth, y + rowH) {
            placeables.forEachIndexed { i, p -> p.placeRelative(pos[i].first, pos[i].second) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) Accent500 else LocalGlass.current.fillStrong,
        tween(200), label = "chipbg",
    )
    val fg by animateColorAsState(
        if (selected) OnAccent else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "chipfg",
    )
    Box(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .noRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** Tap target with no ripple. Composable so it can remember its own source. */
@Composable
internal fun Modifier.noRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)
