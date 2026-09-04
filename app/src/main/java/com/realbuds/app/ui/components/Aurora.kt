package com.realbuds.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.realbuds.app.ui.theme.NightBase

/**
 * Plain ground.
 *
 * Deliberately not a gradient any more. The animated mesh looked impressive in
 * isolation but it competed with every card on top of it, and combined with
 * per-section tints it made the page read as decoration rather than as
 * information. A flat black ground lets the cards and the single accent do the
 * communicating — which is what the reference does.
 *
 * The name is kept so call sites do not churn.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        content = content,
    )
}
