package com.realbuds.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.realbuds.app.adaptive.AdaptiveRules
import com.realbuds.app.proto.CodecSupport
import com.realbuds.app.proto.AncMode
import com.realbuds.app.proto.BatteryInfo
import com.realbuds.app.proto.BudsState
import com.realbuds.app.proto.ConnState
import com.realbuds.app.proto.CustomEq
import com.realbuds.app.proto.EqPreset
import com.realbuds.app.proto.Feature
import com.realbuds.app.proto.KeyBinding
import com.realbuds.app.proto.LogLine
import com.realbuds.app.ui.components.*
import androidx.compose.material3.Switch
import com.realbuds.app.proto.BassBand
import com.realbuds.app.ui.components.StepSlider
import com.realbuds.app.ui.components.QuickTile
import com.realbuds.app.ui.components.QuickTileStrip
import com.realbuds.app.ui.components.BudWear
import com.realbuds.app.ui.components.EarbudGraphic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import com.realbuds.app.ui.theme.OnAccent
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Warning
import com.realbuds.app.proto.BudsError
import com.realbuds.app.ui.theme.SignalCoral as SignalCoral2
import com.realbuds.app.ui.theme.Accent500
import com.realbuds.app.ui.theme.ThemeMode
import com.realbuds.app.ui.theme.ThemePref
import com.realbuds.app.ui.theme.EyebrowStyle
import com.realbuds.app.ui.theme.LocalGlass
import com.realbuds.app.ui.theme.Positive
import com.realbuds.app.ui.theme.CardTints
import com.realbuds.app.ui.theme.SignalAmber as SignalAmber2
import com.realbuds.app.ui.theme.SignalSlate
import com.realbuds.app.ui.theme.SignalLime
import com.realbuds.app.ui.theme.SignalBlue
import com.realbuds.app.ui.theme.SignalMint
import com.realbuds.app.ui.theme.SignalPink

/** Sub-screens reached from the main list. */
private enum class Route { HOME, SOUND_EFFECTS, NOISE, DYNAMIC }

private enum class Tab(val label: String) {
    SOUND("Sound"),
    DEVICE("Device"),
    CONTROLS("Controls"),
    SETTINGS("Settings"),
}

/** Drawn icon per tab, animating on selection. */
@Composable
private fun TabIcon(tab: Tab, active: Boolean, tint: androidx.compose.ui.graphics.Color) {
    when (tab) {
        Tab.SOUND -> NavIconSound(active, tint)
        Tab.DEVICE -> NavIconDevice(active, tint)
        Tab.CONTROLS -> NavIconTouch(active, tint)
        Tab.SETTINGS -> NavIconSettings(active, tint)
    }
}

@Composable
fun BudsScreen(vm: BudsViewModel, onRefresh: () -> Unit) {
    val state by vm.state.collectAsState()
    val buds by vm.buds.collectAsState()
    val name by vm.connectedName.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.SOUND) }
    val connected = state == ConnState.CONNECTED

    AuroraBackground {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = !connected) {
                Header(state, name)
            }

            Box(Modifier.weight(1f)) {
                if (!connected) {
                    ConnectScreen(vm, state, onRefresh)
                } else {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            val dir = if (forward)
                                AnimatedContentTransitionScope.SlideDirection.Left
                            else
                                AnimatedContentTransitionScope.SlideDirection.Right
                            (slideIntoContainer(dir, tween(320)) + fadeIn(tween(260)))
                                .togetherWith(
                                    slideOutOfContainer(dir, tween(320)) + fadeOut(tween(180))
                                )
                                .using(SizeTransform(clip = false))
                        },
                        label = "tabs",
                    ) { current ->
                        when (current) {
                            Tab.SOUND -> SoundTab(vm, buds)
                            Tab.DEVICE -> DeviceTab(vm, buds)
                            Tab.CONTROLS -> ControlsTab(vm, buds)
                            Tab.SETTINGS -> SettingsTab(vm, buds, onRefresh)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = connected,
                enter = fadeIn() + slideInVertically { it },
            ) {
                GlassNavBar(tab) { tab = it }
            }
        }

        ErrorAlert(vm)
    }
}

@Composable
private fun Header(
    state: ConnState,
    name: String?,
) {
    val label: String
    val color: androidx.compose.ui.graphics.Color
    when (state) {
        ConnState.CONNECTED -> { label = "Live"; color = Positive }
        ConnState.CONNECTING -> { label = "Linking"; color = MaterialTheme.colorScheme.tertiary }
        ConnState.DISCONNECTED -> { label = "Offline"; color = MaterialTheme.colorScheme.onSurfaceVariant }
    }

    Row(
        Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RealBudsLogo(size = 30.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "RealBuds",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        StatusPill(label, color)
    }
}

/**
 * Bottom navigation.
 *
 * A conventional docked tab bar, which is what the evidence actually
 * supports. Every popular audio app ships this — Spotify, Apple Music,
 * YouTube Music, Nothing X, Soundcore — and Spotify notably moved *back*
 * toward it rather than away. The floating pill this replaced is a pattern
 * nobody ships.
 *
 * The rules it follows, each from a specific source:
 *
 *  - **Labels always visible.** Icon-only navigation hurts recognition and
 *    accessibility; the earlier version showed the label only on the active
 *    tab, leaving three unlabelled glyphs.
 *  - **Docked and opaque**, edge to edge. No inset, shadow, translucency or
 *    border: those were four decorations doing one edge's job.
 *  - **64dp with 6dp item padding**, per M3 Expressive (down from 80dp and
 *    12/16dp).
 *  - **56dp pill indicator**, per stable M3 Expressive (down from 64dp), and
 *    the active label is *not* bolded — M3 Expressive dropped that.
 *  - **One accent.** Four per-tab colours meant the bar changed hue as you
 *    moved, so colour signalled nothing.
 *
 * Character comes from the drawn icons, which animate their own geometry on
 * selection, not from decorating the container.
 */
@Composable
private fun GlassNavBar(current: Tab, onSelect: (Tab) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                NavItem(
                    tab = t,
                    selected = t == current,
                    modifier = Modifier.weight(1f),
                ) { onSelect(t) }
            }
        }
        // Keeps the bar clear of the gesture area without padding the items.
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** One destination: pill indicator behind the icon, permanent label below. */
@Composable
private fun NavItem(
    tab: Tab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        if (selected) Accent500 else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "navtint",
    )
    val ind by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "navind",
    )

    Column(
        modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.height(32.dp).width(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (ind > 0.01f) {
                Box(
                    Modifier
                        .graphicsLayer { scaleX = ind; alpha = ind }
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Accent500.copy(alpha = 0.16f))
                )
            }
            TabIcon(tab, selected, tint)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = tint,
            maxLines = 1,
        )
    }
}

/** Shared scroll container so every tab has the same rhythm and safe padding. */
@Composable
private fun TabScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ConnectScreen(vm: BudsViewModel, state: ConnState, onRefresh: () -> Unit) {
    val devices by vm.devices.collectAsState()

    TabScroll {
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drawn rather than a product photo: the manufacturer's render is
            // copyrighted and cannot be relicensed with this project.
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EarbudGraphic(left = true, size = 120.dp, wear = BudWear.OUT)
                EarbudGraphic(left = false, size = 120.dp, wear = BudWear.OUT)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Connect your earbuds",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a paired device below to begin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state == ConnState.CONNECTING) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth().clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = LocalGlass.current.fill,
            )
        }

        TintedCard(
            title = "Paired earbuds",
            tint = CardTints.audio,
            accent = SignalLime,
            icon = Icons.Default.Cable,
            trailing = {
                Text(
                    "Scan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .noRippleClick(onRefresh)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            },
        ) {
            if (devices.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Nothing paired yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Pair your earbuds in Android Bluetooth settings, then grant this " +
                            "app the Bluetooth permission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            devices.forEach { d ->
                PressableGlass(
                    onClick = { vm.connect(d) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                d.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            MonoText(d.mac, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "Connect",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundTab(vm: BudsViewModel, buds: BudsState) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val pushing = targetState != Route.HOME
            if (pushing) {
                (slideInHorizontally(tween(300)) { it } + fadeIn(tween(200)))
                    .togetherWith(
                        slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(200))
                    )
            } else {
                (slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(200)))
                    .togetherWith(
                        slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200))
                    )
            }.using(SizeTransform(clip = false))
        },
        label = "route",
    ) { current ->
        when (current) {
            Route.HOME -> SoundHome(vm, buds) { route = it }
            Route.SOUND_EFFECTS -> SoundEffectsScreen(vm, buds) { route = Route.HOME }
            Route.NOISE -> NoiseScreen(vm, buds) { route = Route.HOME }
            Route.DYNAMIC -> DynamicAudioScreen(vm, buds) { route = Route.HOME }
        }
    }
}

/** Main list: device header, ambient sound, then drill-down rows. */
@Composable
private fun SoundHome(vm: BudsViewModel, buds: BudsState, go: (Route) -> Unit) {
    val name by vm.connectedName.collectAsState()
    val activeEq = buds.activeEqId?.let { id ->
        EqPreset.byId(id)?.label
            ?: buds.customEqs.firstOrNull { it.eqId == id }?.name?.ifBlank { "Custom" }
    }

    TabScroll {
        DeviceHeader(name ?: "Earbuds", buds)
        AmbientSoundCard(vm, buds)
        QuickSettingsCard(vm, buds)
        DrillRow(
            title = "Noise Cancellation",
            value = if (buds.anc?.group == AncMode.Group.ANC)
                "${buds.anc?.label} strength" else "Off",
            icon = Icons.Default.HearingDisabled,
        ) { go(Route.NOISE) }
        DrillRow(
            title = "Sound Effects",
            value = activeEq ?: "Not set",
            icon = Icons.Default.GraphicEq,
        ) { go(Route.SOUND_EFFECTS) }
        // 0x010D is authoritative about support; a status-0 reply is not.
        if (Feature.DYNAMIC_BASS.id in buds.supportedFeatures) {
            val on = buds.features[Feature.DYNAMIC_BASS.id] == true
            DrillRow(
                title = "Dynamic audio",
                value = if (on) {
                    buds.bassBands
                        .sortedBy { BassBand.ORDER.indexOf(it.band) }
                        .joinToString(" / ") { if (it.value > 0) "+${it.value}" else "${it.value}" }
                        .ifBlank { "On" }
                } else "Off",
                icon = Icons.Default.Tune,
            ) { go(Route.DYNAMIC) }
        }
        FindBudsCard(vm)
    }
}

/**
 * Product shot, name, then a compact L / R / case strip. Identity first,
 * controls after — the order the reference uses.
 */
@Composable
private fun DeviceHeader(name: String, buds: BudsState) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Drawn rather than a product photo: the manufacturer's render is
        // copyrighted and cannot be relicensed with this project.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EarbudGraphic(
                left = true,
                size = 128.dp,
                wear = buds.budWear(BatteryInfo.Slot.LEFT),
            )
            EarbudGraphic(
                left = false,
                size = 128.dp,
                wear = buds.budWear(BatteryInfo.Slot.RIGHT),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))

        var expanded by rememberSaveable { mutableStateOf(false) }

        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).noRipple { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                "L" to BatteryInfo.Slot.LEFT,
                "R" to BatteryInfo.Slot.RIGHT,
                "C" to BatteryInfo.Slot.CASE,
            ).forEach { (tag, slot) ->
                val info = buds.battery(slot)
                BatteryChip(tag, info?.level, info?.charging == true)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Hide detail" else "Show detail",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            BudDetail(buds)
        }
    }
}

/**
 * Per-bud detail: the drawn earbud, its wear state and lifetime hours.
 *
 * Deliberately has no percentage — that already lives in the strip above, and
 * repeating it was the duplication to avoid. This panel answers the questions
 * the strip cannot: is this bud in my ear, in the case, or just out?
 */
@Composable
private fun BudDetail(buds: BudsState) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(
                "Left" to BatteryInfo.Slot.LEFT,
                "Right" to BatteryInfo.Slot.RIGHT,
            ).forEach { (label, slot) ->
                val used = buds.useTime.firstOrNull { it.slot == slot }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (used != null) "${used.hours}h" else "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "listened",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientSoundCard(vm: BudsViewModel, buds: BudsState) {
    val group = buds.anc?.group
    TintedCard("Ambient Sound", CardTints.neutral, Accent500, icon = Icons.Default.GraphicEq) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CircleMode(
                "Noise\nCancellation", Icons.Default.HearingDisabled,
                group == AncMode.Group.ANC,
            ) { vm.setAnc(vm.lastAncLevel()) }
            CircleMode(
                "Off", Icons.AutoMirrored.Filled.VolumeUp,
                group == AncMode.Group.OFF,
            ) { vm.setAnc(AncMode.OFF) }
            CircleMode(
                "Transparency", Icons.Default.Hearing,
                group == AncMode.Group.TRANSPARENCY,
            ) { vm.setAnc(AncMode.TRANSPARENCY) }
        }

    }
}

/**
 * Sound Effects. Presets are chips in a flowing grid rather than a vertical
 * list — the reference does this, and it means eight options fit without
 * eight full-width rows dominating the screen.
 */
@Composable
private fun SoundEffectsScreen(vm: BudsViewModel, buds: BudsState, back: () -> Unit) {
    val editingId by vm.editingEqId.collectAsState()

    TabScroll {
        SubHeader("Sound Effects", back)

        TintedCard("Presets", CardTints.neutral, Accent500) {
            val presets = EqPreset.entries.sortedBy { it.id }
            ChipGrid(
                items = presets.map { it.label },
                selectedIndex = presets.indexOfFirst { it.id == buds.activeEqId },
            ) { i -> vm.setEqPreset(presets[i]) }
        }

        if (buds.customEqs.isNotEmpty()) {
            TintedCard("Custom EQ", CardTints.neutral, Accent500) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    buds.customEqs.forEach { slot ->
                        PlainRow(
                            title = slot.name.ifBlank { "Custom ${slot.eqId - 3}" },
                            subtitle = if (slot.isEmpty) "Tap to create" else "Six-band",
                            selected = buds.activeEqId == slot.eqId,
                            chevron = true,
                        ) {
                            if (!slot.isEmpty) vm.selectCustomEq(slot)
                            vm.toggleEqEditor(slot.eqId)
                        }
                        AnimatedVisibility(
                            visible = editingId == slot.eqId,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Box(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                                val editable = if (slot.isEmpty)
                                    CustomEq.flat(slot.eqId, "Custom${slot.eqId - 3}") else slot
                                EqEditor(
                                    eq = editable,
                                    onChange = {},
                                    onSave = { vm.saveCustomEq(it) },
                                    onReset = { vm.refreshCustomEqs() },
                                    onDelete = if (slot.isEmpty) null else {
                                        { vm.deleteCustomEq(slot.eqId) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Controls: touch gestures, per earbud.
 *
 * Per-side is real: the deviceType byte in the 0x0401 payload selects the
 * side, with 1 = left, 2 = right and 4 = both.
 *
 * Each write sends a single entry.
 *
 * Actions come from the device's own 0x8108 reply rather than a hardcoded
 * list, so a gesture this model does not have never appears.
 */
@Composable
private fun ControlsTab(vm: BudsViewModel, buds: BudsState) {
    var open by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }
    var mirror by rememberSaveable { mutableStateOf(false) }

    // Intersect what the device reports with what it actually acts on:
    // action 1 (single tap) has a slot and stores a value but never fires.
    val actions = buds.keys
        .filter { it.deviceType in 1..2 }
        .map { it.action }
        .distinct()
        .filter { it in KeyBinding.USABLE_ACTIONS }
        .sorted()

    TabScroll {
        TabTitle("Controls")

        if (buds.keys.isEmpty()) {
            TintedCard("Touch gestures", CardTints.neutral, Accent500, icon = Icons.Default.TouchApp) {
                Text(
                    "Reading gestures from the earbuds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            TintedCard(
                "Both earbuds",
                CardTints.neutral,
                Accent500,
                icon = Icons.Default.TouchApp,
                trailing = { Switch(checked = mirror, onCheckedChange = { mirror = it }) },
            ) {
                Text(
                    if (mirror) "Changing one side changes the other."
                    else "Left and right are set separately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            listOf(1 to "Left earbud", 2 to "Right earbud").forEach { (side, title) ->
                TintedCard(title, CardTints.neutral, Accent500, icon = Icons.Default.TouchApp) {
                    actions.forEach { action ->
                        val k = buds.keys.firstOrNull {
                            it.deviceType == side && it.action == action
                        }
                        val isOpen = open == (side to action)

                        PlainRow(
                            title = k?.actionLabel ?: "Action $action",
                            subtitle = k?.functionLabel ?: "Unknown",
                            selected = isOpen,
                            chevron = true,
                        ) { open = if (isOpen) null else (side to action) }

                        AnimatedVisibility(
                            visible = isOpen,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Box(Modifier.padding(start = 8.dp, bottom = 6.dp)) {
                                ChipGrid(
                                    items = KeyBinding.ASSIGNABLE.map {
                                        KeyBinding.FUNCTIONS[it] ?: "Function $it"
                                    },
                                    selectedIndex =
                                        KeyBinding.ASSIGNABLE.indexOf(k?.function ?: 0),
                                ) { i ->
                                    vm.setKeyBinding(
                                        action = action,
                                        function = KeyBinding.ASSIGNABLE[i],
                                        deviceType = if (mirror) null else side,
                                    )
                                    open = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick settings: four fixed feature tiles.
 *
 * Only ids the earbuds actually reported in 0x010D appear, and only ones we
 * have a confident label for. Dynamic bass is excluded: it has its own screen
 * with three bands, so a bare on/off tile would misrepresent it.
 */
@Composable
private fun QuickSettingsCard(vm: BudsViewModel, buds: BudsState) {
    // LHDC only earns a tile where the phone can actually negotiate it. On a
    // ROM without the Savitech library the toggle just restarts the earbuds'
    // audio stage — dropping their gain until reconnect — for a codec that
    // can never be selected. Auto play/pause takes the slot instead.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val lhdcUsable = remember { CodecSupport.hasLhdc(ctx) }
    val wanted = listOf(
        Feature.GAME_MODE,
        Feature.WIND_NOISE,
        if (lhdcUsable) Feature.HD_AUDIO else Feature.AUTO_PLAY_PAUSE,
        Feature.ENHANCE_VOICE,
    )
    val tiles = wanted
        .filter { it.id in buds.supportedFeatures }
        .map { f ->
            QuickTile(
                id = f.id,
                label = f.shortLabel,
                glyph = f.glyph,
                enabled = buds.features[f.id] == true,
            )
        }

    if (tiles.isEmpty()) return

    TintedCard("Quick settings", CardTints.neutral, Accent500, icon = Icons.Default.Bolt) {
        QuickTileStrip(tiles) { tile, want -> vm.setFeatureId(tile.id, want) }
    }
}

/**
 * Find my earbuds.
 *
 * A latch, not a one-shot: the buds beep until told to stop, and the protocol
 * offers no status query and no push for find mode, so the button reflects
 * what we last sent rather than what the buds are doing. That is why it is a
 * toggle - you need a way to stop the noise.
 */
@Composable
private fun FindBudsCard(vm: BudsViewModel) {
    val finding by vm.finding.collectAsState()

    val pulse = rememberInfiniteTransition(label = "findpulse")
    val glow by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "findglow",
    )
    val alpha = if (finding) glow else 1f

    TintedCard("Find my earbuds", CardTints.neutral, Accent500, icon = Icons.Default.Vibration) {
        Text(
            if (finding) "Beeping. Listen for your earbuds."
            else "Play a sound on both earbuds to locate them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (finding) Accent500.copy(alpha = alpha)
                    else LocalGlass.current.fillStrong
                )
                .noRipple { vm.setFindMode(!finding) }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (finding) Icons.Default.Stop else Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (finding) OnAccent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                if (finding) "Stop" else "Play sound",
                style = MaterialTheme.typography.labelLarge,
                color = if (finding) OnAccent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Dynamic audio: a master switch plus one stepped slider per frequency band.
 *
 * The range is whatever the device reports (-5..+5 on these buds, read from
 * 0x012C) rather than a hardcoded scale, and each band is addressed by its
 * own id so a reordered reply cannot silently move the sliders.
 */
@Composable
private fun DynamicAudioScreen(vm: BudsViewModel, buds: BudsState, back: () -> Unit) {
    val on = buds.features[Feature.DYNAMIC_BASS.id] == true

    TabScroll {
        SubHeader("Dynamic audio", back)

        TintedCard(
            "Dynamic audio",
            CardTints.neutral,
            Accent500,
            icon = Icons.Default.Tune,
            trailing = {
                Switch(checked = on, onCheckedChange = { vm.setDynamicAudio(it) })
            },
        ) {
            Text(
                if (on) "Shape the low, mid and high bands."
                else "Turn on to shape the low, mid and high bands.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (buds.bassBands.isEmpty()) {
                Text(
                    "Reading bands",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BassBand.ORDER.forEach { id ->
                val band = buds.bass(id) ?: return@forEach
                StepSlider(
                    label = band.label,
                    value = band.value,
                    min = band.min,
                    max = band.max,
                    enabled = on,
                ) { v -> vm.setBassBand(band, v) }
            }
        }
    }
}

/**
 * Adaptive noise control.
 *
 * Two layers: the earbuds' own Adaptive mode (wire 32) varies cancellation
 * strength from ambient noise in firmware; the phone knows what you are
 * doing, which the buds cannot. So motion picks the mode, and "Still"
 * defaults to wire 32 so the two compose rather than compete.
 */
@Composable
private fun AdaptiveCard(vm: BudsViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val on by vm.adaptiveOn.collectAsState()
    val available by vm.adaptiveAvailable.collectAsState()
    val motion by vm.activity.collectAsState()

    var rules by remember(on) { mutableStateOf(AdaptiveRules.rules(ctx)) }

    TintedCard(
        "Adaptive",
        CardTints.neutral,
        Accent500,
        icon = Icons.Default.AutoAwesome,
        trailing = {
            Switch(
                checked = on,
                enabled = available,
                onCheckedChange = { vm.setAdaptive(ctx, it) },
            )
        },
    ) {
        Text(
            when {
                !available -> "This phone has no step sensor, so motion cannot be detected."
                on -> "Switching automatically. Detected: ${motion.label}."
                else -> "Switch noise control automatically when you start moving."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AnimatedVisibility(
            visible = on && available,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AdaptiveRules.order.forEach { act ->
                    val rule = rules.firstOrNull { it.activity == act } ?: return@forEach
                    val isNow = motion == act
                    PlainRow(
                        title = act.label,
                        subtitle = if (rule.enabled) rule.mode.label else "Ignore",
                        selected = isNow,
                    ) {
                        val choices = listOf(
                            AncMode.ANC_SMART,
                            AncMode.TRANSPARENCY,
                            AncMode.OFF,
                        ) + AncMode.ancLevels
                        if (!rule.enabled) {
                            AdaptiveRules.setRuleEnabled(ctx, act, true)
                            AdaptiveRules.setMode(ctx, act, choices.first())
                        } else {
                            val i = choices.indexOf(rule.mode)
                            if (i == choices.lastIndex) {
                                AdaptiveRules.setRuleEnabled(ctx, act, false)
                            } else {
                                AdaptiveRules.setMode(
                                    ctx, act,
                                    choices[(i + 1).coerceAtMost(choices.lastIndex)],
                                )
                            }
                        }
                        rules = AdaptiveRules.rules(ctx)
                    }
                }
                Text(
                    "Tap a row to change its mode. A manual choice pauses " +
                        "switching until you move differently.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }
        }
    }
}

/** Title row for a tab root: same height and baseline as SubHeader, no back arrow. */
@Composable
private fun TabTitle(title: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.height(40.dp).wrapContentHeight(),
        )
    }
}

/** Back arrow + title, standard for a pushed screen. */
@Composable
private fun SubHeader(title: String, back: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).noRippleClick(back),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Noise cancellation strength, as its own screen.
 *
 * Tiles rather than radio rows: four options read as four objects you pick
 * between, which is what the choice actually is. When cancelling is off the
 * tiles stay visible but dim and inert — that communicates "this control
 * exists, it just needs cancelling on" far better than hiding it does.
 */
@Composable
private fun NoiseScreen(vm: BudsViewModel, buds: BudsState, back: () -> Unit) {
    val current = buds.anc
    val on = current?.group == AncMode.Group.ANC

    TabScroll {
        SubHeader("Noise Cancellation", back)

        AdaptiveCard(vm)

        AncCycleCard(vm)

        TintedCard(
            "Cancellation",
            CardTints.neutral,
            Accent500,
            icon = Icons.Default.HearingDisabled,
            trailing = {
                Switch(
                    checked = on,
                    onCheckedChange = { want ->
                        vm.setAnc(if (want) vm.lastAncLevel() else AncMode.OFF)
                    },
                )
            },
        ) {
            Text(
                if (on) "Pick how hard the earbuds work to cut outside sound."
                else "Turn cancellation on to choose a strength.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            AncMode.ancLevels.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { level ->
                        StrengthTile(
                            level = level,
                            selected = on && level == current,
                            enabled = on,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { vm.setAnc(level) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                if (row !== AncMode.ancLevels.chunked(2).last()) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/**
 * Which modes the noise-control gesture walks through.
 *
 * Separate from the strength tiles because it is a different thing: the tiles
 * pick the mode now, this picks what a long press cycles between. An
 * unconfigured cycle is why a noise-control gesture can fire and change
 * nothing.
 *
 * Write-only on this model, so the selection is remembered locally rather
 * than read back from the device.
 */
@Composable
private fun AncCycleCard(vm: BudsViewModel) {
    val current by vm.ancCycle.collectAsState()

    TintedCard("Gesture cycle", CardTints.neutral, Accent500, icon = Icons.Default.Refresh) {
        Text(
            "What a noise-control gesture switches between.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AncMode.Companion.Cycle.entries.forEach { c ->
            PlainRow(
                title = c.label,
                selected = c == current,
            ) { vm.setAncCycle(c) }
        }
    }
}

/**
 * One strength choice. Greys out wholesale when cancelling is off, using a
 * single alpha on the whole tile so icon, label and description dim together.
 */
@Composable
private fun StrengthTile(
    level: AncMode,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val g = LocalGlass.current
    val alpha by animateFloatAsState(
        if (enabled) 1f else 0.38f,
        tween(220), label = "tilealpha",
    )
    val bg by animateColorAsState(
        if (selected) Accent500.copy(alpha = 0.16f) else g.fillStrong,
        tween(220), label = "tilebg",
    )
    val borderColor by animateColorAsState(
        if (selected) Accent500 else Color.Transparent,
        tween(220), label = "tileborder",
    )
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.5.dp, borderColor), shape)
            .then(if (enabled) Modifier.noRipple(onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            when (level) {
                AncMode.ANC_SMART -> Icons.Default.AutoAwesome
                AncMode.ANC_MAX -> Icons.Default.SignalCellularAlt
                AncMode.ANC_MODERATE -> Icons.Default.SignalCellularAlt2Bar
                else -> Icons.Default.SignalCellularAlt1Bar
            },
            contentDescription = null,
            tint = if (selected) Accent500 else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
        Text(
            level.label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Accent500 else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            level.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 2,
        )
    }
}

/** App-level actions that used to sit in the header. */
@Composable
private fun SettingsTab(vm: BudsViewModel, buds: BudsState, onRefresh: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val mode by ThemePref.state
    val state by vm.state.collectAsState()
    val name by vm.connectedName.collectAsState()

    TabScroll {
        TabTitle("Settings")

        TintedCard("Appearance", CardTints.neutral, Accent500, icon = Icons.Default.DarkMode) {
            PlainRow(
                title = "Theme",
                subtitle = when (mode) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.SYSTEM -> "Follow system"
                },
                chevron = true,
            ) { ThemePref.next(ctx) }
        }

        if (buds.highVolume != null) {
            TintedCard("Audio", CardTints.neutral, Accent500, icon = Icons.AutoMirrored.Filled.VolumeUp) {
                ToggleRow(
                    "High volume",
                    "Lift the regional volume limit. Loud output can damage hearing.",
                    buds.highVolume == true,
                ) { vm.setHighVolume(it) }
            }
        }

        val refreshing by vm.refreshing.collectAsState()
        var toast by remember { mutableStateOf<Pair<String, String>?>(null) }
        LaunchedEffect(toast) {
            if (toast != null) {
                kotlinx.coroutines.delay(2200)
                toast = null
            }
        }

        TintedCard("Connection", CardTints.neutral, Accent500, icon = Icons.Default.Refresh) {
            PlainRow(
                title = "Refresh device state",
                subtitle = if (refreshing) "Reading…"
                           else "Re-read battery, modes and features",
                chevron = !refreshing,
            ) {
                vm.refresh()
                toast = "conn" to "Refreshing device state"
            }
            PlainRow(
                title = if (state == ConnState.CONNECTED) "Reconnect" else "Connect",
                subtitle = name ?: "No device selected",
                chevron = true,
            ) {
                onRefresh()
                toast = "conn" to "Scanning for earbuds"
            }
            PlainRow(
                title = "Disconnect",
                subtitle = when (state) {
                    ConnState.CONNECTED -> "Connected"
                    ConnState.CONNECTING -> "Connecting…"
                    ConnState.DISCONNECTED -> "Not connected"
                },
                chevron = state == ConnState.CONNECTED,
            ) {
                if (state != ConnState.DISCONNECTED) {
                    vm.disconnect()
                    toast = "conn" to "Disconnected"
                } else {
                    toast = "conn" to "Already disconnected"
                }
            }

            AnimatedVisibility(
                visible = toast?.first == "conn",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    toast?.second ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent500,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
        }

        TintedCard("Debugging", CardTints.neutral, Accent500, icon = Icons.Default.Terminal) {
            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            val logs by vm.logs.collectAsState()
            PlainRow(
                title = "Copy debug log",
                subtitle = if (logs.isEmpty()) "Nothing recorded yet"
                           else "${logs.size} lines, oldest first",
                chevron = logs.isNotEmpty(),
            ) {
                if (logs.isEmpty()) {
                    toast = "dbg" to "No log to copy"
                } else {
                    clipboard.setText(AnnotatedString(vm.logForClipboard()))
                    toast = "dbg" to "Copied ${logs.size} lines"
                }
            }
            AnimatedVisibility(
                visible = toast?.first == "dbg",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    toast?.second ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent500,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DeviceTab(vm: BudsViewModel, buds: BudsState) {
    val supported = buds.supportedFeatures
    val named = supported.mapNotNull { id -> Feature.byId(id)?.takeIf { it.certain } }
    val unnamed = supported.filter { id -> Feature.byId(id)?.certain != true }

    TabScroll {
        TabTitle("Device")
        TintedCard("Features", CardTints.device, SignalBlue, icon = Icons.Default.Tune) {
            if (supported.isEmpty()) {
                Text(
                    "Reading supported features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                named.forEach { f ->
                    ToggleRow(f.label, f.description, buds.features[f.id] == true) {
                        vm.setFeature(f, it)
                    }
                }
            }
        }

        if (unnamed.isNotEmpty()) {
            TintedCard("Unlabelled", CardTints.neutral, SignalAmber2, icon = Icons.Default.Science) {
                Text(
                    "Reported by these earbuds but not yet identified. " +
                        "Flip one and tell me what changed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unnamed.forEach { id ->
                        ToggleRow("Setting $id", null, buds.features[id] == true) {
                            vm.setFeatureId(id, it)
                        }
                    }
                }
            }
        }

        if (buds.keys.isNotEmpty()) {
            TouchControlsCard(vm, buds)
        }

        TintedCard("Connection", CardTints.neutral, SignalSlate) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlowButton("Refresh state", { vm.refresh() })
                PressableGlass(onClick = { vm.disconnect() }, shape = RoundedCornerShape(14.dp)) {
                    Text(
                        "Disconnect",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)

/**
 * Touch-gesture editor.
 *
 * Left and right are written together, so one list covers both. Tapping a
 * gesture expands a function picker inline rather than opening a dialog —
 * fewer layers to get back out of.
 */
@Composable
private fun TouchControlsCard(vm: BudsViewModel, buds: BudsState) {
    var openAction by rememberSaveable { mutableStateOf(-1) }

    TintedCard(
        "Touch controls",
        CardTints.audio,
        SignalPink,
        icon = Icons.Default.TouchApp,
    ) {
        Text(
            "Applies to both earbuds.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buds.keys
                .filter { it.deviceType == 1 && it.action in 1..4 }
                .sortedBy { it.action }
                .forEach { k ->
                    val open = openAction == k.action
                    ChoiceRow(
                        title = k.actionLabel,
                        description = k.functionLabel,
                        selected = open,
                        accent = SignalPink,
                        leading = when (k.action) {
                            1 -> Icons.Default.TouchApp
                            2 -> Icons.Default.Tab
                            3 -> Icons.Default.TouchApp
                            else -> Icons.Default.Timer
                        },
                    ) {
                        openAction = if (open) -1 else k.action
                    }

                    AnimatedVisibility(
                        visible = open,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(
                            Modifier.padding(start = 10.dp, top = 2.dp, bottom = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            KeyBinding.ASSIGNABLE.forEach { fn ->
                                val name = KeyBinding.FUNCTIONS[fn] ?: "Function $fn"
                                ChoiceRow(
                                    title = name,
                                    description = null,
                                    selected = k.function == fn,
                                    accent = SignalPink,
                                ) {
                                    vm.setKeyBinding(k.action, fn)
                                    openAction = -1
                                }
                            }
                        }
                    }
                }
        }
    }
}

/**
 * The one place errors surface.
 *
 * A dialog rather than an inline banner: these are failures the user asked
 * for — a refused write, a dropped connection — so they need acknowledging,
 * not a message that scrolls away unread.
 *
 * Body text is capped by BudsError.body(); a malformed reply can carry an
 * arbitrarily long hex dump, and an uncapped string would push the dismiss
 * button off screen.
 */
@Composable
private fun ErrorAlert(vm: BudsViewModel) {
    val err by vm.error.collectAsState()
    val extra by vm.suppressedErrors.collectAsState()
    val e = err ?: return

    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = { vm.dismissError() },
        icon = {
            Icon(
                when (e.kind) {
                    BudsError.Kind.CONNECTION -> Icons.Default.Cable
                    BudsError.Kind.REFUSED -> Icons.Default.Block
                    else -> Icons.Default.Warning
                },
                contentDescription = null,
                tint = SignalCoral2,
            )
        },
        title = {
            Text(
                when (e.kind) {
                    BudsError.Kind.CONNECTION -> "Connection problem"
                    BudsError.Kind.REFUSED -> "Earbuds refused that"
                    BudsError.Kind.UNEXPECTED -> "Unexpected response"
                    BudsError.Kind.PROTOCOL -> "Something went wrong"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column {
                Text(
                    e.body(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (extra > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$extra more problem${if (extra == 1) "" else "s"} " +
                            "were reported at the same time.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.dismissError() }) { Text("Dismiss") }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(vm.logForClipboard()))
                vm.dismissError()
            }) { Text("Copy log") }
        },
    )
}
