package com.herotoner.herotv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herotoner.herotv.adb.SavedTv
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.model.TvCommand
import com.herotoner.herotv.pad.PadProfile
import com.herotoner.herotv.ui.components.ButtonKind
import com.herotoner.herotv.ui.components.HeroButton
import com.herotoner.herotv.ui.components.InfoRow
import com.herotoner.herotv.ui.components.ScreenHeader
import com.herotoner.herotv.ui.components.SectionCard
import com.herotoner.herotv.ui.components.connectionColor
import com.herotoner.herotv.ui.components.connectionLabel
import com.herotoner.herotv.ui.theme.HeroColors

@Composable
fun SettingsScreen(
    connection: ConnectionState,
    devices: List<SavedTv>,
    profiles: List<PadProfile>,
    activeProfileId: String,
    haptics: Boolean,
    clickSound: Boolean,
    keepAwake: Boolean,
    deviceInfo: String?,
    deviceInfoLoading: Boolean,
    commandLog: List<String>,
    onBack: () -> Unit,
    onOpenPairing: () -> Unit,
    onConnect: (SavedTv) -> Unit,
    onForget: (SavedTv) -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetClickSound: (Boolean) -> Unit,
    onSetKeepAwake: (Boolean) -> Unit,
    onCommand: (TvCommand, Boolean) -> Unit,
    onSelectProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onEditPad: () -> Unit,
    onLoadDeviceInfo: () -> Unit,
    onClearLog: () -> Unit
) {
    val connected = connection is ConnectionState.Connected
    val working = connection is ConnectionState.Connecting || connection is ConnectionState.Reconnecting

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الإعدادات", connection, onBack = null, onChipClick = onOpenPairing)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ───── الاتصال ─────
            SectionCard("حالة الاتصال") {
                Text(connectionLabel(connection), color = connectionColor(connection), fontSize = 16.sp)
                if (connection is ConnectionState.Connected) {
                    Text("العنوان: ${connection.address}", color = HeroColors.TextSecondary, fontSize = 13.sp)
                }
                if (connection is ConnectionState.Error) {
                    Text(connection.message, color = HeroColors.Red, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (connected || working) {
                        HeroButton("قطع الاتصال", onDisconnect, Modifier.weight(1f), ButtonKind.Secondary)
                    } else if (devices.isNotEmpty()) {
                        HeroButton("إعادة الاتصال", onRetry, Modifier.weight(1f))
                    }
                    HeroButton("اقتران تلفزيون جديد", onOpenPairing, Modifier.weight(1f), ButtonKind.Secondary)
                }
                HeroButton(
                    text = if (deviceInfoLoading) "جارٍ القراءة…" else "قراءة معلومات التلفزيون",
                    onClick = onLoadDeviceInfo,
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Secondary,
                    enabled = connected && !deviceInfoLoading
                )
                if (deviceInfo != null) {
                    Text(deviceInfo, color = HeroColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }

            // ───── إدارة الأجهزة ─────
            SectionCard("الأجهزة المقترنة") {
                if (devices.isEmpty()) {
                    Text("لا توجد أجهزة محفوظة.", color = HeroColors.TextSecondary, fontSize = 13.sp)
                }
                devices.sortedByDescending { it.lastConnectedAt }.forEach { device ->
                    InfoRow(title = device.name, subtitle = "${device.host}:${device.port}") {
                        HeroButton("اتصال", { onConnect(device) }, enabled = !working)
                        HeroButton("حذف", { onForget(device) }, kind = ButtonKind.Danger)
                    }
                }
            }

            // ───── الاهتزاز والصوت ─────
            SectionCard("الاهتزاز والصوت") {
                SwitchRow("اهتزاز عند لمس الأزرار", "يحترم إعداد الاهتزاز في هاتفك", haptics, onSetHaptics)
                SwitchRow("صوت نقر عند لمس الأزرار", "صوت النظام للمس على الهاتف", clickSound, onSetClickSound)
                SwitchRow("إبقاء الشاشة مضاءة في يد الألعاب", null, keepAwake, onSetKeepAwake)
                Text("التحكم بصوت التلفزيون", color = HeroColors.TextPrimary, fontSize = 15.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroButton("خفض", { onCommand(TvCommand.VOLUME_DOWN, false) }, Modifier.weight(1f), ButtonKind.Secondary, enabled = connected)
                    HeroButton("كتم", { onCommand(TvCommand.MUTE, false) }, Modifier.weight(1f), ButtonKind.Secondary, enabled = connected)
                    HeroButton("رفع", { onCommand(TvCommand.VOLUME_UP, false) }, Modifier.weight(1f), ButtonKind.Secondary, enabled = connected)
                }
            }

            // ───── تخصيص يد الألعاب ─────
            SectionCard("تخصيص أزرار يد الألعاب") {
                Text(
                    "لكل ملف مواضع أزرار ووظائف مستقلة. اختر الملف النشط أو افتح وضع التخصيص لتحريك الأزرار وتغيير وظائفها.",
                    color = HeroColors.TextSecondary,
                    fontSize = 13.sp
                )
                profiles.forEach { p ->
                    InfoRow(
                        title = p.name,
                        subtitle = if (p.id == activeProfileId) "الملف النشط" else null
                    ) {
                        if (p.id != activeProfileId) {
                            HeroButton("تفعيل", { onSelectProfile(p.id) }, kind = ButtonKind.Secondary)
                        }
                        if (profiles.size > 1) {
                            HeroButton("حذف", { onDeleteProfile(p.id) }, kind = ButtonKind.Danger)
                        }
                    }
                }
                HeroButton("فتح وضع التخصيص", onEditPad, Modifier.fillMaxWidth())
            }

            // ───── اللغة ─────
            SectionCard("اللغة") {
                InfoRow(
                    title = "العربية (RTL)",
                    subtitle = "لغة التطبيق الوحيدة المتوفرة حالياً. لا توجد ترجمات أخرى."
                )
            }

            // ───── سجل الأوامر ─────
            SectionCard("سجل آخر الأوامر") {
                if (commandLog.isEmpty()) {
                    Text("لا توجد أوامر بعد.", color = HeroColors.TextSecondary, fontSize = 13.sp)
                } else {
                    commandLog.forEach { Text(it, color = HeroColors.TextSecondary, fontSize = 12.sp) }
                    HeroButton("مسح السجل", onClearLog, kind = ButtonKind.Secondary)
                }
            }

            // ───── حول ─────
            val context = LocalContext.current
            val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                .getOrNull() ?: "؟"
            SectionCard("حول التطبيق") {
                Text(
                    "HERO TV CONTROL ULTIMATE • الإصدار $version\nتطوير: Hero Toner\n" +
                        "الاتصال محلي عبر Wi-Fi فقط. مفتاح ADB محفوظ مشفّراً داخل Android Keystore ولا يغادر هاتفك.",
                    color = HeroColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    InfoRow(title = title, subtitle = subtitle) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = HeroColors.Blue
            )
        )
    }
}
