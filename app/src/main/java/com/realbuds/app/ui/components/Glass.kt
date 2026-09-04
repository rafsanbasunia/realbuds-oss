package com.realbuds.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.realbuds.app.ui.theme.EyebrowStyle
import com.realbuds.app.ui.theme.LocalGlass

/**
 * Frosted panel.
 *
 * Real glass needs three things, not just a translucent fill:
 *   1. a bright top edge where light catches the bevel,
 *   2. a hairline stroke all round,
 *   3. a shadow tinted with the background hue rather than black.
 * The gradient overlay below supplies (1); [border] and [shadow] the rest.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    raised: Boolean = false,
    elevation: Dp = 18.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val g = LocalGlass.current
    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = g.shadow, spotColor = g.shadow)
            .clip(shape)
            .background(if (raised) g.fillStrong else g.fill)
            .border(BorderStroke(1.dp, if (raised) g.strokeStrong else g.stroke), shape)

        ,
        content = content,
    )
}

/**
 * Gradient card. Same structure as [GlassCard] but with a per-section tint,
 * so the page reads as a set of distinct surfaces instead of a stack of
 * identical black rectangles.
 */
@Composable
fun TintedCard(
    title: String,
    tint: List<Color>,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tint.first())
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                }
                Text(
                    title,
                    style = EyebrowStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
            }
            content()
        }
    }
}

/**
 * Row that opens another screen. The reference uses these for Sound Effects
 * and Controls: label, current value, chevron.
 */
@Composable
fun DrillRow(
    title: String,
    value: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    PressableGlass(onClick = onClick, shape = shape, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(13.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (value != null) {
                    Text(
                        value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A titled glass card. The eyebrow label is uppercase micro-type, which reads
 * as a section marker without needing a heavier heading.
 */
@Composable
fun GlassCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        title,
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    trailing?.invoke()
                }
            }
            content()
        }
    }
}

/**
 * Press-responsive glass. Springs down slightly on touch — the physical cue
 * that makes a surface feel tappable rather than painted on.
 */
@Composable
fun PressableGlass(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    selected: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val g = LocalGlass.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "press",
    )

    val fill = when {
        !enabled -> g.fillPressed
        selected -> g.fillStrong
        pressed -> g.fillPressed
        else -> g.fill
    }
    val stroke = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else g.stroke

    Box(
        modifier
            .scale(scale)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, stroke), shape)
            .then(
                if (enabled) Modifier.clickableNoRipple(interaction, onClick) else Modifier
            )

    ) { content() }
}

private fun Modifier.clickableNoRipple(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
) = this.clickable(
    interactionSource = interaction,
    indication = null,
    onClick = onClick,
)

/** Solid accent button with a coloured glow instead of a grey drop shadow. */
@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 1000f),
        label = "glow",
    )
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier
            .scale(scale)
            .shadow(
                if (enabled) 14.dp else 0.dp,
                shape,
                ambientColor = accent.copy(alpha = 0.5f),
                spotColor = accent.copy(alpha = 0.6f),
            )
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.82f)))
                else Brush.horizontalGradient(listOf(Color(0x1FFFFFFF), Color(0x1FFFFFFF)))
            )
            .clickableNoRipple(interaction) { if (enabled) onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
