package com.herotoner.herotv.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.ui.theme.HeroColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** تُضبط من الإعدادات وتُقرأ داخل كل زر تحكم. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }
val LocalClickSoundEnabled = staticCompositionLocalOf { false }

private const val HOLD_DELAY_MS = 380L
private const val HOLD_INTERVAL_MS = 130L

fun connectionLabel(state: ConnectionState): String = when (state) {
    is ConnectionState.Connected -> "متصل • ${state.deviceName}"
    is ConnectionState.Connecting -> "جارٍ الاتصال…"
    is ConnectionState.Reconnecting ->
        if (state.attempt > 0) "إعادة الاتصال… (${state.attempt})" else "إعادة الاتصال…"
    is ConnectionState.Error -> "فشل الاتصال"
    is ConnectionState.Disconnected -> "غير متصل"
}

fun connectionColor(state: ConnectionState): Color = when (state) {
    is ConnectionState.Connected -> HeroColors.Green
    is ConnectionState.Connecting -> HeroColors.Amber
    is ConnectionState.Reconnecting -> HeroColors.Amber
    is ConnectionState.Error -> HeroColors.Red
    is ConnectionState.Disconnected -> HeroColors.TextSecondary
}

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
    val color = connectionColor(state)
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
        Text(
            text = connectionLabel(state),
            color = color,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** ترويسة موحّدة لكل الصفحات: رجوع + عنوان + حالة الاتصال (تفتح صفحة الاقتران عند الضغط). */
@Composable
fun ScreenHeader(
    title: String,
    connection: ConnectionState,
    onBack: (() -> Unit)?,
    onChipClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = HeroColors.TextPrimary)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = HeroColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val chipModifier = if (onChipClick != null) {
            Modifier
                .clip(RoundedCornerShape(50))
                .pointerInput(Unit) { detectTapGestures(onTap = { onChipClick() }) }
        } else {
            Modifier
        }
        ConnectionChip(state = connection, modifier = chipModifier)
        Spacer(Modifier.width(8.dp))
    }
}

/**
 * زر تحكم: يستجيب لحظة اللمس (وليس عند الرفع) لتقليل التأخير.
 * repeatOnHold = تكرار الأمر أثناء الضغط المستمر.
 * onPress(repeat): repeat=false للضغطة الأولى، true للتكرار.
 */
@Composable
fun HeroKey(
    onPress: (repeat: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    accent: Boolean = false,
    borderColor: Color? = null,
    repeatOnHold: Boolean = false,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val hapticsOn = LocalHapticsEnabled.current
    val soundOn = LocalClickSoundEnabled.current
    val currentOnPress by rememberUpdatedState(onPress)
    var pressed by remember { mutableStateOf(false) }

    val background = if (accent) {
        Brush.verticalGradient(listOf(HeroColors.BlueBright, HeroColors.Blue))
    } else {
        Brush.verticalGradient(listOf(HeroColors.SurfaceHigh, HeroColors.Surface))
    }
    val edge = borderColor ?: if (accent) HeroColors.BlueBright else HeroColors.Outline

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = if (pressed) 0.94f else 1f
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .background(background)
            .border(if (pressed) 2.dp else 1.dp, if (pressed) HeroColors.Cyan else edge, shape)
            .pointerInput(enabled, repeatOnHold, hapticsOn, soundOn) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    pressed = true
                    if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (soundOn) view.playSoundEffect(SoundEffectConstants.CLICK)
                    currentOnPress(false)
                    if (repeatOnHold) {
                        coroutineScope {
                            val job = launch {
                                delay(HOLD_DELAY_MS)
                                while (true) {
                                    currentOnPress(true)
                                    delay(HOLD_INTERVAL_MS)
                                }
                            }
                            tryAwaitRelease()
                            job.cancel()
                        }
                    } else {
                        tryAwaitRelease()
                    }
                    pressed = false
                })
            }
            .padding(10.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

enum class ButtonKind { Primary, Secondary, Danger }

@Composable
fun HeroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true
) {
    val container = when (kind) {
        ButtonKind.Primary -> HeroColors.Blue
        ButtonKind.Secondary -> HeroColors.SurfaceHigh
        ButtonKind.Danger -> HeroColors.Red
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color.White,
            disabledContainerColor = HeroColors.SurfaceHigh,
            disabledContentColor = HeroColors.TextSecondary
        )
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun HeroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    ltr: Boolean = true,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        textStyle = TextStyle(
            color = HeroColors.TextPrimary,
            fontSize = 16.sp,
            textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HeroColors.Blue,
            unfocusedBorderColor = HeroColors.Outline,
            focusedLabelColor = HeroColors.BlueBright,
            unfocusedLabelColor = HeroColors.TextSecondary,
            cursorColor = HeroColors.BlueBright,
            focusedTextColor = HeroColors.TextPrimary,
            unfocusedTextColor = HeroColors.TextPrimary
        )
    )
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HeroColors.Surface)
            .border(1.dp, HeroColors.Outline, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = HeroColors.BlueBright, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

/** صف نصي عادي داخل بطاقة: عنوان + سطر فرعي اختياري + محتوى في النهاية (زر/مفتاح). */
@Composable
fun InfoRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = HeroColors.TextPrimary, fontSize = 15.sp)
            if (subtitle != null) {
                Text(subtitle, color = HeroColors.TextSecondary, fontSize = 12.sp)
            }
        }
        trailing()
    }
}
