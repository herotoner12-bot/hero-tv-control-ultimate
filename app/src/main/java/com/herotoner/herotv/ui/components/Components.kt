package com.herotoner.herotv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.ui.theme.HeroColors

@Composable
fun HeroLogo(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "HERO",
            style = TextStyle(
                brush = Brush.verticalGradient(listOf(Color.White, HeroColors.BlueBright)),
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 2.sp
            )
        )
        Text(
            text = "TV CONTROL",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = 1.sp
        )
        Text(
            text = "ULTIMATE",
            color = HeroColors.Cyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = 6.sp
        )
    }
}

@Composable
fun ConnectionChip(state: ConnectionState, modifier: Modifier = Modifier) {
    val color: Color
    val text: String
    when (state) {
        is ConnectionState.Connected -> {
            color = HeroColors.Green
            text = "متصل • ${state.deviceName}"
        }
        is ConnectionState.Connecting -> {
            color = HeroColors.Amber
            text = "جارٍ الاتصال…"
        }
        is ConnectionState.Error -> {
            color = HeroColors.Red
            text = state.message
        }
        is ConnectionState.Disconnected -> {
            color = HeroColors.TextSecondary
            text = "غير متصل"
        }
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(HeroColors.SurfaceHigh)
            .border(1.dp, color.copy(alpha = 0.5f), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = text, color = color, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** زر تحكم عام بخلفية متدرجة وإطار واهتزاز خفيف عند اللمس. */
@Composable
fun HeroKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    accent: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val background = if (accent) {
        Brush.verticalGradient(listOf(HeroColors.BlueBright, HeroColors.Blue))
    } else {
        Brush.verticalGradient(listOf(HeroColors.SurfaceHigh, HeroColors.Surface))
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, if (accent) HeroColors.BlueBright else HeroColors.Outline, shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(10.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}
