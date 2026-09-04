package com.realbuds.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * RealBuds visual language.
 *
 * Restrained on purpose. A true-black ground, flat dark grey cards, and ONE
 * accent colour used only to mark the active thing. No gradients on the page,
 * no per-section tints, no glass.
 *
 * The earlier gradient-heavy version had every card in its own colour, which
 * meant nothing stood out because everything did. Here colour carries a single
 * job — "this is selected" — so it actually communicates.
 *
 * Not Material You: dynamic colour would undo the deliberate restraint.
 */

val NightBase      = Color(0xFF000000)
val VioletDeep     = Color(0xFF000000)   // kept for API compatibility
val VioletMid      = Color(0xFF000000)
val MagentaGlow    = Color(0xFF000000)
val IndigoEdge     = Color(0xFF000000)

val CardBlack      = Color(0xFF16171A)   // standard card
val CardRaised     = Color(0xFF202226)   // inset control inside a card

val TextPrimary    = Color(0xFFF5F6F7)
val TextSecondary  = Color(0xFF8E9297)
val TextTertiary   = Color(0xFF5F6368)

/**
 * One accent. Everything interactive that is *active* uses this and nothing
 * else, so the eye learns it means "current".
 */
/**
 * Fallback accent, used when the platform cannot supply a dynamic one:
 * Android 11 and older, or a device with Material You disabled.
 */
val AccentFallback = Color(0xFF2ED8F7)
val OnAccentFallback = Color(0xFF00232B)

/**
 * The app's accent, taken from the system wallpaper accent where available.
 *
 * A composable getter rather than a constant so every existing call site
 * resolves against the active theme with no change: it reads
 * `colorScheme.primary`, which RealBudsTheme fills from the platform's dynamic
 * palette on API 31+ and from [AccentFallback] below that.
 */
val Accent500: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

/** Text and icons sitting on top of [Accent500]. */
val OnAccent: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onPrimary

val SignalLime: Color @Composable @ReadOnlyComposable get() = Accent500
val SignalMint: Color @Composable @ReadOnlyComposable get() = Accent500
val SignalBlue: Color @Composable @ReadOnlyComposable get() = Accent500
val SignalPink: Color @Composable @ReadOnlyComposable get() = Accent500
val SignalSlate    = Color(0xFF8E9297)

val SignalAmber    = Color(0xFFE5A93C)
val SignalCoral    = Color(0xFFE05563)

val Accent: Color @Composable @ReadOnlyComposable get() = Accent500
val Positive: Color @Composable @ReadOnlyComposable get() = Accent500
val Caution        = SignalAmber
val Danger         = SignalCoral

val Ink            = NightBase
val OrbBlue        = VioletMid
val OrbViolet      = MagentaGlow

/**
 * Per-card gradient pairs. Each card owns a hue so the page reads as a set of
 * distinct surfaces rather than a stack of identical black rectangles.
 */
/**
 * Card fill. Reads from the theme rather than a fixed colour so the same card
 * works in both modes; the per-section tints are gone entirely.
 */
object CardTints {
    val neutral: List<Color>
        @Composable get() = LocalGlass.current.fill.let { listOf(it, it) }
    val battery: List<Color> @Composable get() = neutral
    val noise: List<Color> @Composable get() = neutral
    val audio: List<Color> @Composable get() = neutral
    val device: List<Color> @Composable get() = neutral
}

/** Card/glass tokens. Alpha-based, so kept outside the M3 scheme. */
data class GlassTokens(
    /**
     * True when this bundle is the dark one. Components that paint their own
     * ground (the page background, the nav bar) must branch on this rather
     * than calling isSystemInDarkTheme(), which ignores an explicit
     * light/dark choice and leaves them stuck on the system value.
     */
    val isDark: Boolean = true,
    val fill: Color = CardBlack,
    val fillStrong: Color = CardRaised,
    val fillPressed: Color = Color(0xFF101114),
    val stroke: Color = Color(0x00FFFFFF),      // no hairlines; spacing separates
    val strokeStrong: Color = Color(0x1FFFFFFF),
    val innerGlow: Color = Color(0x00FFFFFF),   // no glass highlight
    val shadow: Color = Color(0x00000000),      // flat, no elevation
    val sheer: Color = CardBlack,
    val sheerStroke: Color = Color(0x00FFFFFF),
)

val LocalGlass = staticCompositionLocalOf { GlassTokens() }

private val DarkScheme = darkColorScheme(
    primary = AccentFallback,
    onPrimary = OnAccentFallback,
    primaryContainer = Color(0x332ED8F7),
    onPrimaryContainer = AccentFallback,
    secondary = AccentFallback,
    tertiary = AccentFallback,
    background = NightBase,
    onBackground = TextPrimary,
    surface = CardBlack,
    onSurface = TextPrimary,
    surfaceVariant = CardRaised,
    onSurfaceVariant = TextSecondary,
    outline = Color(0x1FFFFFFF),
    outlineVariant = Color(0x14FFFFFF),
    error = SignalCoral,
    onError = Color(0xFF2A0009),
)

/**
 * Light mode, matching the reference's white version: an off-white ground with
 * pure-white cards, so cards read as raised without needing a shadow.
 */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF0AA9C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF4FB),
    onPrimaryContainer = Color(0xFF00323C),
    secondary = Color(0xFF0AA9C7),
    tertiary = Color(0xFF0AA9C7),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF14171A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14171A),
    surfaceVariant = Color(0xFFEDF0F3),
    onSurfaceVariant = Color(0xFF6B7178),
    outline = Color(0x14000000),
    outlineVariant = Color(0x0F000000),
    error = Color(0xFFC0334A),
    onError = Color.White,
)

private val GlassDark = GlassTokens()

private val GlassLight = GlassTokens(
    isDark = false,
    fill = Color(0xFFFFFFFF),
    fillStrong = Color(0xFFEDF0F3),
    fillPressed = Color(0xFFE7EBEF),
    stroke = Color(0x00000000),
    strokeStrong = Color(0x14000000),
    innerGlow = Color(0x00FFFFFF),
    shadow = Color(0x00000000),
    sheer = Color(0xFFFFFFFF),
    sheerStroke = Color(0x00000000),
)

/**
 * Battery colour by charge level.
 *
 * Composable because the healthy colour now follows the system accent, while
 * the warning colours stay fixed — a low battery has to read as a warning
 * regardless of what the wallpaper happens to be.
 */
object BudsColors {
    val charging: Color @Composable @ReadOnlyComposable get() = Accent500

    @Composable
    @ReadOnlyComposable
    fun forLevel(level: Int): Color = when {
        level <= 15 -> SignalCoral
        level <= 35 -> SignalAmber
        else -> Accent500
    }
}

@Composable
fun RealBudsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val base = if (darkTheme) DarkScheme else LightScheme
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val ctx = LocalContext.current
        val dyn = if (darkTheme) dynamicDarkColorScheme(ctx)
                  else dynamicLightColorScheme(ctx)
        base.copy(
            primary = dyn.primary,
            onPrimary = dyn.onPrimary,
            primaryContainer = dyn.primary.copy(alpha = 0.20f),
            onPrimaryContainer = dyn.primary,
            secondary = dyn.primary,
            tertiary = dyn.primary,
        )
    } else base

    CompositionLocalProvider(LocalGlass provides if (darkTheme) GlassDark else GlassLight) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BudsTypography,
            shapes = BudsShapes,
            content = content,
        )
    }
}
