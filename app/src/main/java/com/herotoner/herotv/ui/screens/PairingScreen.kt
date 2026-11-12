package com.herotoner.herotv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herotoner.herotv.adb.DiscoveredTv
import com.herotoner.herotv.adb.SavedTv
import com.herotoner.herotv.adb.ServiceKind
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.ui.components.ButtonKind
import com.herotoner.herotv.ui.components.HeroButton
import com.herotoner.herotv.ui.components.HeroTextField
import com.herotoner.herotv.ui.components.InfoRow
import com.herotoner.herotv.ui.components.ScreenHeader
import com.herotoner.herotv.ui.components.SectionCard
import com.herotoner.herotv.ui.theme.HeroColors

@Composable
fun PairingScreen(
    connection: ConnectionState,
    discovered: List<DiscoveredTv>,
    saved: List<SavedTv>,
    busy: Boolean,
    message: String?,
    onBack: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onPair: (host: String, pairPort: Int, code: String, connectPort: Int?) -> Unit,
    onConnectDirect: (host: String, port: Int) -> Unit,
    onConnectDiscovered: (DiscoveredTv) -> Unit,
    onConnectSaved: (SavedTv) -> Unit,
    onForget: (SavedTv) -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit,
    onError: (String) -> Unit,
    onConnected: () -> Unit
) {
    var host by rememberSaveable { mutableStateOf("") }
    var pairPort by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var connectPort by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }

    // البحث على الشبكة يعمل فقط أثناء ظهور هذه الصفحة
    DisposableEffect(Unit) {
        onStartDiscovery()
        onDispose { onStopDiscovery() }
    }

    // بعد محاولة يدوية ناجحة ننتقل إلى الريموت
    val currentOnConnected by rememberUpdatedState(onConnected)
    LaunchedEffect(connection, attempted) {
        if (attempted && connection is ConnectionState.Connected) {
            attempted = false
            currentOnConnected()
        }
    }

    val isConnected = connection is ConnectionState.Connected
    val working = busy || connection is ConnectionState.Connecting || connection is ConnectionState.Reconnecting

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("اقتران التلفزيون", connection, onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ───── الحالة والتحكم بالاتصال ─────
            SectionCard("الاتصال الحالي") {
                if (working) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = HeroColors.BlueBright, strokeWidth = 2.5.dp)
                        Text("جارٍ العمل…", color = HeroColors.TextSecondary, fontSize = 14.sp)
                    }
                }
                if (message != null) {
                    Text(message, color = if (isConnected) HeroColors.Green else HeroColors.Amber, fontSize = 14.sp)
                }
                if (connection is ConnectionState.Error && connection.message != message) {
                    Text(connection.message, color = HeroColors.Red, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isConnected || working) {
                        HeroButton("قطع الاتصال", onDisconnect, Modifier.weight(1f), ButtonKind.Secondary)
                    }
                    if (!isConnected && saved.isNotEmpty()) {
                        HeroButton("إعادة المحاولة", onRetry, Modifier.weight(1f), enabled = !working)
                    }
                }
            }

            // ───── كيف أُفعّل التصحيح اللاسلكي؟ ─────
            SectionCard("خطوات على التلفزيون") {
                Text(
                    "1. الإعدادات ← تفضيلات الجهاز ← حول ← اضغط «Build» 7 مرات لتفعيل خيارات المطوّر.\n" +
                        "2. الإعدادات ← تفضيلات الجهاز ← خيارات المطوّر ← فعّل «التصحيح اللاسلكي» (Wireless debugging).\n" +
                        "3. اختر «اقتران جهاز برمز الاقتران». ستظهر نافذة فيها رمز من 6 أرقام وعنوان IP ومنفذ.\n" +
                        "4. أدخل هذه القيم أسفل (أو اضغط «استخدام» على النافذة المكتشفة تلقائياً) ثم «اقتران واتصال».\n\n" +
                        "الرمز والمنفذ يتغيران في كل مرة تُفتح فيها النافذة. بعض التلفزيونات لا تُظهر خيار التصحيح اللاسلكي؛ " +
                        "في هذه الحالة جرّب «تصحيح USB» أو «تصحيح الشبكة» إن وُجد ثم «اتصال فقط».",
                    color = HeroColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }

            // ───── ما اكتُشف على الشبكة ─────
            SectionCard("أجهزة مكتشفة على الشبكة") {
                if (discovered.isEmpty()) {
                    Text(
                        "جارٍ البحث… لم يظهر شيء بعد. تأكد أن الهاتف والتلفزيون على شبكة Wi-Fi نفسها وأن التصحيح اللاسلكي " +
                            "مفعّل أو نافذة الاقتران مفتوحة.",
                        color = HeroColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
                discovered.forEach { service ->
                    val title = when (service.kind) {
                        ServiceKind.PAIRING -> "نافذة اقتران مفتوحة"
                        ServiceKind.CONNECT -> "جاهز للاتصال"
                        ServiceKind.LEGACY -> "تصحيح الشبكة (ADB)"
                    }
                    InfoRow(title = title, subtitle = "${service.host}:${service.port}") {
                        if (service.kind == ServiceKind.PAIRING) {
                            HeroButton("استخدام", {
                                host = service.host
                                pairPort = service.port.toString()
                            }, kind = ButtonKind.Secondary)
                        } else {
                            HeroButton("اتصال", {
                                attempted = true
                                onConnectDiscovered(service)
                            }, enabled = !working)
                        }
                    }
                }
            }

            // ───── الإدخال اليدوي ─────
            SectionCard("إدخال يدوي") {
                HeroTextField(host, { host = it.trim() }, "عنوان IP للتلفزيون (مثال 192.168.1.45)", Modifier.fillMaxWidth(), numeric = false)
                HeroTextField(pairPort, { pairPort = it.filter(Char::isDigit).take(5) }, "منفذ الاقتران", Modifier.fillMaxWidth(), numeric = true)
                HeroTextField(code, { code = it.filter(Char::isDigit).take(6) }, "رمز الاقتران (6 أرقام)", Modifier.fillMaxWidth(), numeric = true)
                HeroTextField(
                    connectPort,
                    { connectPort = it.filter(Char::isDigit).take(5) },
                    "منفذ الاتصال (اختياري بعد الاقتران)",
                    Modifier.fillMaxWidth(),
                    numeric = true
                )
                HeroButton(
                    text = "اقتران واتصال",
                    onClick = {
                        val pp = pairPort.toIntOrNull()
                        val cp = connectPort.toIntOrNull()
                        when {
                            host.isBlank() -> onError("أدخل عنوان IP للتلفزيون.")
                            pp == null -> onError("أدخل منفذ الاقتران.")
                            code.length != 6 -> onError("رمز الاقتران يجب أن يكون 6 أرقام.")
                            connectPort.isNotEmpty() && cp == null -> onError("منفذ الاتصال غير صالح.")
                            else -> {
                                attempted = true
                                onPair(host, pp, code, cp)
                                code = "" // لا نُبقي رمز الاقتران في الذاكرة بعد استخدامه
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !working
                )
                HeroButton(
                    text = "اتصال فقط (تلفزيون مقترن مسبقاً)",
                    onClick = {
                        val port = connectPort.toIntOrNull()
                        when {
                            host.isBlank() -> onError("أدخل عنوان IP للتلفزيون.")
                            port == null -> onError("أدخل منفذ الاتصال الظاهر في شاشة التصحيح اللاسلكي (أو 5555 لتصحيح الشبكة).")
                            else -> {
                                attempted = true
                                onConnectDirect(host, port)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Secondary,
                    enabled = !working
                )
            }

            // ───── الأجهزة المحفوظة ─────
            SectionCard("الأجهزة المحفوظة") {
                if (saved.isEmpty()) {
                    Text("لا توجد أجهزة محفوظة. يُحفظ التلفزيون تلقائياً بعد أول اتصال ناجح.", color = HeroColors.TextSecondary, fontSize = 13.sp)
                }
                saved.sortedByDescending { it.lastConnectedAt }.forEach { device ->
                    InfoRow(title = device.name, subtitle = "${device.host}:${device.port}") {
                        HeroButton("اتصال", {
                            attempted = true
                            onConnectSaved(device)
                        }, enabled = !working)
                        HeroButton("حذف", { onForget(device) }, kind = ButtonKind.Danger)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
