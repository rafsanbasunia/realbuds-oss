package com.realbuds.app.ui.theme

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/** Light / dark / follow-system, persisted across launches. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemePref {
    private const val FILE = "realbuds_prefs"
    private const val KEY = "theme_mode"

    lateinit var state: MutableState<ThemeMode>
        private set

    fun init(context: Context) {
        if (::state.isInitialized) return
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val name = sp.getString(KEY, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        state = mutableStateOf(runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM))
    }

    fun set(context: Context, mode: ThemeMode) {
        state.value = mode
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, mode.name).apply()
    }

    /** Cycles system -> light -> dark -> system. */
    fun next(context: Context) = set(
        context,
        when (state.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        },
    )
}
