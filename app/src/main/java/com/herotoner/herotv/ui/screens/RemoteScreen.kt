package com.herotoner.herotv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.model.TvCommand
import com.herotoner.herotv.ui.components.ButtonKind
import com.herotoner.herotv.ui.components.HeroButton
import com.herotoner.herotv.ui.components.HeroKey
import com.herotoner.herotv.ui.components.HeroTextField
import com.herotoner.herotv.ui.components.ScreenHeader
import com.herotoner.herotv.ui.theme.HeroColors
import kotlin.math.abs

@Composable
fun RemoteScreen(
    connection: ConnectionState,
    onBack: () -> Unit,
    onOpenPairing: () -> Unit,
    onCommand: (TvCommand, Boolean) -> Unit,
    onText: (String) -> Unit
) {
    var showTouchpad by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الريموت الذكي", connection, onBack, onChipClick = onOpenPairing)

        // أزرار الريموت تُرتَّب فيزيائياً (يسار/يمين حقيقيان) لذلك نثبّت LTR داخلها
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledKey("رجوع", Icons.AutoMirrored.Filled.ArrowBack) { onCommand(TvCommand.BACK, false) }
                    LabeledKey("الرئيسية", Icons.Filled.Home) { onCommand(TvCommand.HOME, false) }
                    LabeledKey("القائمة", Icons.Filled.Menu) { onCommand(TvCommand.MENU, false) }
                }

                DPad(onCommand)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledKey("خفض", Icons.Filled.VolumeDown, repeat = true) { onCommand(TvCommand.VOLUME_DOWN, it) }
                    LabeledKey("كتم", Icons.Filled.VolumeOff) { onCommand(TvCommand.MUTE, false) }
                    LabeledKey("رفع", Icons.Filled.VolumeUp, repeat = true) { onCommand(TvCommand.VOLUME_UP, it) }
                }

                HeroKey(
                    onPress = { onCommand(TvCommand.PLAY_PAUSE, false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.White)
                        Text("تشغيل / إيقاف مؤقت", color = Color.White, fontSize = 16.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledKey("لوحة اللمس", Icons.Filled.TouchApp) { if (!it) showTouchpad = true }
                    LabeledKey("لوحة المفاتيح", Icons.Filled.Keyboard) { if (!it) showKeyboard = true }
                }
            }
        }
    }

    if (showTouchpad) TouchpadDialog(onDismiss = { showTouchpad = false }, onCommand = onCommand)
    if (showKeyboard) KeyboardDialog(onDismiss = { showKeyboard = false }, onCommand = onCommand, onText = onText)
}

@Composable
private fun RowScope.LabeledKey(
    label: String,
    icon: ImageVector,
    repeat: Boolean = false,
    onPress: (Boolean) -> Unit
) {
    HeroKey(
        onPress = onPress,
        modifier = Modifier
            .weight(1f)
            .height(74.dp),
        repeatOnHold = repeat
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun DPad(onCommand: (TvCommand, Boolean) -> Unit) {
    Box(Modifier.size(252.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(HeroColors.SurfaceHigh, HeroColors.Surface)))
                .border(
                    2.dp,
                    Brush.sweepGradient(listOf(HeroColors.Blue, HeroColors.Cyan, HeroColors.BlueDeep, HeroColors.Blue)),
                    CircleShape
                )
        )
        ArrowKey("▲", Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) { onCommand(TvCommand.UP, it) }
        ArrowKey("▼", Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) { onCommand(TvCommand.DOWN, it) }
        ArrowKey("◀", Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) { onCommand(TvCommand.LEFT, it) }
        ArrowKey("▶", Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) { onCommand(TvCommand.RIGHT, it) }
        HeroKey(
            onPress = { onCommand(TvCommand.SELECT, false) },
            modifier = Modifier
                .align(Alignment.Center)
                .size(92.dp),
            shape = CircleShape,
            accent = true
        ) {
            Text("OK", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ArrowKey(glyph: String, modifier: Modifier, onPress: (Boolean) -> Unit) {
    HeroKey(
        onPress = onPress,
        modifier = modifier.size(60.dp),
        shape = CircleShape,
        repeatOnHold = true
    ) {
        Text(glyph, color = Color.White, fontSize = 20.sp)
    }
}

/**
 * لوحة لمس: السحب يتحول إلى ضغطات أسهم (كل مسافة معينة = ضغطة)، واللمسة القصيرة = OK.
 * لا تتحكم بمؤشر ماوس؛ هي ريموت بالإيماءات لواجهة التلفزيون.
 */
@Composable
private fun TouchpadDialog(onDismiss: () -> Unit, onCommand: (TvCommand, Boolean) -> Unit) {
    val currentCommand by rememberUpdatedState(onCommand)
    val stepPx = with(LocalDensity.current) { 52.dp.toPx() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(HeroColors.Surface)
                .border(1.dp, HeroColors.Outline, RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("لوحة اللمس", color = HeroColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "اسحب للتنقل بين العناصر، واضغط ضغطة قصيرة لـ OK.",
                color = HeroColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(HeroColors.SurfaceHigh, HeroColors.Black)))
                    .border(2.dp, HeroColors.Blue, RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { currentCommand(TvCommand.SELECT, false) })
                    }
                    .pointerInput(stepPx) {
                        var accX = 0f
                        var accY = 0f
                        detectDragGestures(
                            onDragStart = {
                                accX = 0f
                                accY = 0f
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                accX += drag.x
                                accY += drag.y
                                while (abs(accX) >= stepPx) {
                                    currentCommand(if (accX > 0) TvCommand.RIGHT else TvCommand.LEFT, false)
                                    accX -= if (accX > 0) stepPx else -stepPx
                                }
                                while (abs(accY) >= stepPx) {
                                    currentCommand(if (accY > 0) TvCommand.DOWN else TvCommand.UP, false)
                                    accY -= if (accY > 0) stepPx else -stepPx
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("اسحب هنا", color = HeroColors.TextSecondary, fontSize = 14.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroButton("رجوع", { currentCommand(TvCommand.BACK, false) }, Modifier.weight(1f), ButtonKind.Secondary)
                HeroButton("إغلاق", onDismiss, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KeyboardDialog(
    onDismiss: () -> Unit,
    onCommand: (TvCommand, Boolean) -> Unit,
    onText: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(HeroColors.Surface)
                .border(1.dp, HeroColors.Outline, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("لوحة المفاتيح", color = HeroColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "اضغط داخل حقل نص على التلفزيون أولاً، ثم أرسل النص من هنا. المسموح: أحرف إنجليزية وأرقام ومسافات " +
                    "(الأحرف العربية غير مدعومة عبر ADB).",
                color = HeroColors.TextSecondary,
                fontSize = 13.sp
            )
            HeroTextField(text, { text = it.take(64) }, "النص", Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroButton(
                    "إرسال",
                    {
                        onText(text)
                        text = ""
                    },
                    Modifier.weight(1f),
                    enabled = text.isNotEmpty()
                )
                HeroButton("مسح حرف", { onCommand(TvCommand.BACKSPACE, false) }, Modifier.weight(1f), ButtonKind.Secondary)
                HeroButton("Enter", { onCommand(TvCommand.ENTER, false) }, Modifier.weight(1f), ButtonKind.Secondary)
            }
            HeroButton("إغلاق", onDismiss, Modifier.fillMaxWidth(), ButtonKind.Secondary)
        }
    }
}
