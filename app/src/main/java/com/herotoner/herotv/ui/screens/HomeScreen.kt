package com.herotoner.herotv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herotoner.herotv.adb.SavedTv
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.ui.Screen
import com.herotoner.herotv.ui.components.ButtonKind
import com.herotoner.herotv.ui.components.ConnectionChip
import com.herotoner.herotv.ui.components.HeroButton
import com.herotoner.herotv.ui.components.HeroLogo
import com.herotoner.herotv.ui.components.InfoRow
import com.herotoner.herotv.ui.components.SectionCard
import com.herotoner.herotv.ui.components.connectionColor
import com.herotoner.herotv.ui.components.connectionLabel
import com.herotoner.herotv.ui.theme.HeroColors

private data class HomeTile(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val deep: Color,
    /** null = نافذة "حول التطبيق" */
    val target: Screen?
)

private val tiles = listOf(
    HomeTile("يد الألعاب (Gamepad)", Icons.Filled.SportsEsports, HeroColors.Purple, HeroColors.PurpleDeep, Screen.Gamepad),
    HomeTile("الريموت الذكي", Icons.Filled.SettingsRemote, HeroColors.Blue, HeroColors.BlueDeep, Screen.Remote),
    HomeTile("التطبيقات والألعاب", Icons.Filled.Apps, HeroColors.Green, HeroColors.GreenDeep, Screen.Apps),
    HomeTile("اقتران التلفزيون", Icons.Filled.Link, HeroColors.Purple, HeroColors.PurpleDeep, Screen.Pairing),
    HomeTile("الإعدادات", Icons.Filled.Settings, HeroColors.Amber, HeroColors.AmberDeep, Screen.Settings),
    HomeTile("حول التطبيق", Icons.Filled.Info, HeroColors.Blue, HeroColors.BlueDeep, null)
)

@Composable
fun HomeScreen(
    connection: ConnectionState,
    devices: List<SavedTv>,
    onNavigate: (Screen) -> Unit,
    onConnect: (SavedTv) -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroLogo()
        Spacer(Modifier.height(12.dp))
        ConnectionChip(
            state = connection,
            modifier = Modifier.clickable { onNavigate(Screen.Pairing) }
        )
        Spacer(Modifier.height(16.dp))
        Banner()
        Spacer(Modifier.height(16.dp))

        StatusCard(connection, devices, onNavigate, onDisconnect, onRetry)
        Spacer(Modifier.height(16.dp))

        tiles.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { tile ->
                    Tile(
                        tile = tile,
                        onClick = {
                            if (tile.target == null) showAbout = true else onNavigate(tile.target)
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (devices.isNotEmpty()) {
            SectionCard("الأجهزة المحفوظة") {
                devices.sortedByDescending { it.lastConnectedAt }.forEach { device ->
                    val isCurrent = connection is ConnectionState.Connected &&
                        connection.address == "${device.host}:${device.port}"
                    InfoRow(
                        title = device.name,
                        subtitle = "${device.host}:${device.port}"
                    ) {
                        HeroButton(
                            text = if (isCurrent) "متصل" else "اتصال",
                            onClick = { onConnect(device) },
                            enabled = !isCurrent && connection !is ConnectionState.Connecting
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
}

@Composable
private fun StatusCard(
    connection: ConnectionState,
    devices: List<SavedTv>,
    onNavigate: (Screen) -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit
) {
    SectionCard("حالة التلفزيون") {
        val detail = when (connection) {
            is ConnectionState.Connected -> "العنوان: ${connection.address}"
            is ConnectionState.Connecting -> "جارٍ الاتصال بالتلفزيون…"
            is ConnectionState.Reconnecting -> "جارٍ محاولة الوصول إلى آخر تلفزيون متصل…"
            is ConnectionState.Error -> connection.message
            is ConnectionState.Disconnected ->
                if (devices.isEmpty()) "لم تقترن بأي تلفزيون بعد." else "آخر تلفزيون: ${devices.maxByOrNull { it.lastConnectedAt }?.name}"
        }
        Text(connectionLabel(connection), color = connectionColor(connection), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = HeroColors.TextSecondary, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (connection) {
                is ConnectionState.Connected -> {
                    HeroButton("قطع الاتصال", onDisconnect, Modifier.weight(1f), ButtonKind.Secondary)
                }
                is ConnectionState.Connecting, is ConnectionState.Reconnecting -> {
                    HeroButton("إيقاف المحاولة", onDisconnect, Modifier.weight(1f), ButtonKind.Secondary)
                }
                else -> {
                    if (devices.isNotEmpty()) {
                        HeroButton("إعادة الاتصال", onRetry, Modifier.weight(1f))
                    } else {
                        HeroButton("اقتران تلفزيون", { onNavigate(Screen.Pairing) }, Modifier.weight(1f))
                    }
                }
            }
            HeroButton("إدارة الأجهزة", { onNavigate(Screen.Pairing) }, Modifier.weight(1f), ButtonKind.Secondary)
        }
    }
}

@Composable
private fun Banner() {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(HeroColors.BlueDeep, HeroColors.Black, HeroColors.PurpleDeep)))
            .border(1.dp, HeroColors.Outline, shape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Tv, contentDescription = null, tint = HeroColors.BlueBright, modifier = Modifier.size(40.dp))
            Text(
                text = "تحكم أذكى .. تجربة أفضل",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RowScope.Tile(tile: HomeTile, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(112.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tile.deep, HeroColors.Surface)))
            .border(1.dp, tile.accent.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(tile.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = tile.label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "؟"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("حسناً") } },
        title = { Text("HERO TV CONTROL ULTIMATE") },
        text = {
            Text(
                "الإصدار $version\nتطوير: Hero Toner\n\n" +
                    "التحكم بالتلفزيون عبر Wi-Fi محلياً فقط، دون إرسال أي بيانات إلى الإنترنت. " +
                    "مفتاح ADB محفوظ مشفّراً داخل Android Keystore."
            )
        },
        containerColor = HeroColors.Surface,
        titleContentColor = HeroColors.TextPrimary,
        textContentColor = HeroColors.TextSecondary
    )
}
