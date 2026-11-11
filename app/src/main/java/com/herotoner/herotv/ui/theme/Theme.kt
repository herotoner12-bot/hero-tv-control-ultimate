package com.herotoner.herotv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object HeroColors {
    val Black = Color(0xFF04060C)
    val Surface = Color(0xFF0A1020)
    val SurfaceHigh = Color(0xFF121B30)
    val Outline = Color(0xFF22304F)
    val Blue = Color(0xFF1E90FF)
    val BlueBright = Color(0xFF42B0FF)
    val BlueDeep = Color(0xFF0B3C8C)
    val Cyan = Color(0xFF38D9FF)
    val Green = Color(0xFF3DDC84)
    val GreenDeep = Color(0xFF0E5A38)
    val Purple = Color(0xFF6C4CFF)
    val PurpleDeep = Color(0xFF2B1B7A)
    val Amber = Color(0xFFFFB300)
    val AmberDeep = Color(0xFF6B4A00)
    val Red = Color(0xFFE53935)
    val TextPrimary = Color(0xFFF2F6FF)
    val TextSecondary = Color(0xFF9AA8C4)
}

private val HeroDarkScheme = darkColorScheme(
    primary = HeroColors.Blue,
    onPrimary = Color.White,
    secondary = HeroColors.Cyan,
    background = HeroColors.Black,
    onBackground = HeroColors.TextPrimary,
    surface = HeroColors.Surface,
    onSurface = HeroColors.TextPrimary,
    surfaceVariant = HeroColors.SurfaceHigh,
    onSurfaceVariant = HeroColors.TextSecondary,
    outline = HeroColors.Outline,
    error = HeroColors.Red
)

@Composable
fun HeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HeroDarkScheme, content = content)
}
