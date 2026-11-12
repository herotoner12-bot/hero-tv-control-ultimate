package com.herotoner.herotv.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** إعدادات التطبيق المحلية (اهتزاز، صوت النقر، إبقاء الشاشة مضاءة أثناء يد الألعاب). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("hero_settings", Context.MODE_PRIVATE)

    private val _haptics = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val haptics: StateFlow<Boolean> = _haptics.asStateFlow()

    private val _clickSound = MutableStateFlow(prefs.getBoolean(KEY_SOUND, false))
    val clickSound: StateFlow<Boolean> = _clickSound.asStateFlow()

    private val _keepAwake = MutableStateFlow(prefs.getBoolean(KEY_AWAKE, true))
    val keepAwake: StateFlow<Boolean> = _keepAwake.asStateFlow()

    fun setHaptics(value: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
        _haptics.value = value
    }

    fun setClickSound(value: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, value).apply()
        _clickSound.value = value
    }

    fun setKeepAwake(value: Boolean) {
        prefs.edit().putBoolean(KEY_AWAKE, value).apply()
        _keepAwake.value = value
    }

    private companion object {
        const val KEY_HAPTICS = "haptics"
        const val KEY_SOUND = "click_sound"
        const val KEY_AWAKE = "keep_awake"
    }
}
