package com.realbuds.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vector earbud, drawn to match the Buds Air 8 silhouette: a rounded driver
 * housing with a long flat stem angled outward, a mesh grille and an LED dot.
 *
 * Drawn rather than photographed because every official product render has the
 * buds sitting inside the charging case, overlapping it — there is no clean
 * cut that yields a single isolated bud. A vector also stays crisp at any size
 * and can be tinted to match the UI.
 */
/** How a bud is being worn, which changes how it is drawn. */
enum class BudWear { IN_EAR, OUT, IN_CASE, UNKNOWN }

@Composable
fun EarbudGraphic(
    left: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    body: Color = Color(0xFF2A2A2E),
    highlight: Color = Color(0xFF4E4E56),
    accent: Color = Color(0xFF9EF01A),
    wear: BudWear = BudWear.UNKNOWN,
) {
    val dim = wear == BudWear.IN_CASE
    val shellBody = if (dim) body.copy(alpha = 0.45f) else body
    val shellHigh = if (dim) highlight.copy(alpha = 0.45f) else highlight
    val lit = wear == BudWear.IN_EAR

    Canvas(modifier.size(size)) {
        if (left) {
            drawContext.transform.scale(-1f, 1f, Offset(this.size.width / 2f, 0f))
            drawBud(shellBody, shellHigh, accent, lit, dim)
            drawContext.transform.scale(-1f, 1f, Offset(this.size.width / 2f, 0f))
        } else {
            drawBud(shellBody, shellHigh, accent, lit, dim)
        }
    }
}

/** Grid is 100x100; the bud occupies roughly x 26..74, y 8..92. */
private fun DrawScope.drawBud(
    body: Color,
    highlight: Color,
    accent: Color,
    lit: Boolean,
    dim: Boolean,
) {
    val u = size.minDimension / 100f

    val shell = Brush.linearGradient(
        listOf(highlight, body, Color(0xFF17171A)),
        start = Offset(30 * u, 8 * u),
        end = Offset(72 * u, 92 * u),
    )

    val head = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(Offset(30 * u, 8 * u), Size(34 * u, 30 * u)),
                CornerRadius(16 * u, 15 * u),
            )
        )
    }
    drawPath(head, shell)

    rotate(degrees = -8f, pivot = Offset(47 * u, 34 * u)) {
        drawRoundRect(
            brush = shell,
            topLeft = Offset(39 * u, 30 * u),
            size = Size(17 * u, 60 * u),
            cornerRadius = CornerRadius(8.5f * u, 8.5f * u),
        )
    }

    rotate(degrees = -8f, pivot = Offset(47 * u, 34 * u)) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                startY = 30 * u,
                endY = 70 * u,
            ),
            topLeft = Offset(41 * u, 32 * u),
            size = Size(4 * u, 46 * u),
            cornerRadius = CornerRadius(2 * u, 2 * u),
        )
    }

    drawRoundRect(
        brush = Brush.linearGradient(listOf(body, Color(0xFF101013))),
        topLeft = Offset(56 * u, 16 * u),
        size = Size(14 * u, 13 * u),
        cornerRadius = CornerRadius(6 * u, 6 * u),
    )

    val tipBase = if (dim) Color(0xFF4A4A52).copy(alpha = 0.5f) else Color(0xFF4A4A52)
    val tipEdge = if (dim) Color(0xFF2C2C33).copy(alpha = 0.5f) else Color(0xFF2C2C33)
    drawPath(
        Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset(64 * u, 13.5f * u), Size(17 * u, 18 * u)),
                    CornerRadius(8.5f * u, 9 * u),
                )
            )
        },
        Brush.linearGradient(
            listOf(tipBase, tipEdge),
            start = Offset(64 * u, 13.5f * u),
            end = Offset(81 * u, 31.5f * u),
        ),
    )
    drawRoundRect(
        color = Color(0xFF14141A).copy(alpha = if (dim) 0.5f else 1f),
        topLeft = Offset(75 * u, 19 * u),
        size = Size(5 * u, 7 * u),
        cornerRadius = CornerRadius(2.5f * u, 3.5f * u),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = if (dim) 0.05f else 0.16f),
        topLeft = Offset(67 * u, 15.5f * u),
        size = Size(9 * u, 3 * u),
        cornerRadius = CornerRadius(1.5f * u, 1.5f * u),
    )

    drawRoundRect(
        color = Color(0xFF0C0C0F),
        topLeft = Offset(38 * u, 14 * u),
        size = Size(11 * u, 3 * u),
        cornerRadius = CornerRadius(1.5f * u, 1.5f * u),
    )

    drawCircle(
        color = if (lit) accent else Color(0xFF3A3A40),
        radius = 2.2f * u,
        center = Offset(46 * u, 44 * u),
    )
    if (lit) {
        drawCircle(
            color = accent.copy(alpha = 0.28f),
            radius = 5.5f * u,
            center = Offset(46 * u, 44 * u),
        )
    }
}
