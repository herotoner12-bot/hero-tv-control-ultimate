package com.herotoner.herotv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.model.TvCommand
import com.herotoner.herotv.ui.components.ConnectionChip
import com.herotoner.herotv.ui.components.HeroKey
import com.herotoner.herotv.ui.theme.HeroColors
import kotlinx.coroutines.delay

@Composable
fun RemoteScreen(
    connection: ConnectionState,
    statusMessage: String?,
    onBack: () -> Unit,
    onCommand: (TvCommand) -> Unit,
    onStatusShown: () -> Unit
) {
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(3500)
            onStatusShown()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // الترويسة (تتبع اتجاه RTL)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = HeroColors.TextPrimary)
            }
            Text(
                text = "الريموت الذكي",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = HeroColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            ConnectionChip(state = connection)
            Spacer(Modifier.width(8.dp))
        }

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
                    LabeledKey("رجوع", Icons.AutoMirrored.Filled.ArrowBack) { onCommand(TvCommand.BACK) }
                    LabeledKey("الرئيسية", Icons.Filled.Home) { onCommand(TvCommand.HOME) }
                    LabeledKey("القائمة", Icons.Filled.Menu) { onCommand(TvCommand.MENU) }
                }

                DPad(onCommand)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledKey("خفض", Icons.Filled.VolumeDown) { onCommand(TvCommand.VOLUME_DOWN) }
                    LabeledKey("كتم", Icons.Filled.VolumeOff) { onCommand(TvCommand.MUTE) }
                    LabeledKey("رفع", Icons.Filled.VolumeUp) { onCommand(TvCommand.VOLUME_UP) }
                }

                HeroKey(
                    onClick = { onCommand(TvCommand.PLAY_PAUSE) },
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

                StatusPill(statusMessage ?: "جاهز")
            }
        }
    }
}

@Composable
private fun RowScope.LabeledKey(label: String, icon: ImageVector, onClick: () -> Unit) {
    HeroKey(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(74.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DPad(onCommand: (TvCommand) -> Unit) {
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
        ArrowKey("▲", Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) { onCommand(TvCommand.UP) }
        ArrowKey("▼", Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) { onCommand(TvCommand.DOWN) }
        ArrowKey("◀", Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) { onCommand(TvCommand.LEFT) }
        ArrowKey("▶", Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) { onCommand(TvCommand.RIGHT) }
        HeroKey(
            onClick = { onCommand(TvCommand.SELECT) },
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
private fun ArrowKey(glyph: String, modifier: Modifier, onClick: () -> Unit) {
    HeroKey(
        onClick = onClick,
        modifier = modifier.size(60.dp),
        shape = CircleShape
    ) {
        Text(glyph, color = Color.White, fontSize = 20.sp)
    }
}

@Composable
private fun StatusPill(text: String) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HeroColors.Surface)
            .border(1.dp, HeroColors.Outline, shape)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = HeroColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
