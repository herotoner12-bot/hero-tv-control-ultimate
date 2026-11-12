package com.herotoner.herotv

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herotoner.herotv.adb.DiscoveredTv
import com.herotoner.herotv.adb.Phase
import com.herotoner.herotv.adb.SavedTv
import com.herotoner.herotv.adb.ShellCommands
import com.herotoner.herotv.adb.TvApp
import com.herotoner.herotv.adb.TvRepository
import com.herotoner.herotv.adb.toArabicMessage
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.model.TvCommand
import com.herotoner.herotv.pad.PadAction
import com.herotoner.herotv.pad.PadProfile
import com.herotoner.herotv.pad.ProfilesStore
import com.herotoner.herotv.settings.SettingsStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface AppsUi {
    data object Idle : AppsUi
    data object Loading : AppsUi
    data class Loaded(val apps: List<TvApp>) : AppsUi
    data class Failed(val message: String) : AppsUi
}

class HeroViewModel(application: Application) : AndroidViewModel(application) {

    private val tv: TvRepository = (application as HeroTvApp).tv
    private val pad: ProfilesStore = (application as HeroTvApp).pad
    private val settings: SettingsStore = (application as HeroTvApp).settings

    val connection: StateFlow<ConnectionState> = tv.connection
    val savedDevices: StateFlow<List<SavedTv>> = tv.devices.devices
    val discovered: StateFlow<List<DiscoveredTv>> = tv.discovery.found

    val profiles: StateFlow<List<PadProfile>> = pad.profiles
    val activeProfileId: StateFlow<String> = pad.activeId
    val gamePackages: StateFlow<Set<String>> = pad.games

    val haptics: StateFlow<Boolean> = settings.haptics
    val clickSound: StateFlow<Boolean> = settings.clickSound
    val keepAwake: StateFlow<Boolean> = settings.keepAwake

    /** رسالة قصيرة تظهر كإشعار سفلي (أخطاء وأحداث الاتصال). */
    var statusMessage by mutableStateOf<String?>(null)
        private set

    /** حالة انشغال ونتيجة آخر عملية اقتران/اتصال يدوية. */
    var busy by mutableStateOf(false)
        private set
    var pairingMessage by mutableStateOf<String?>(null)
        private set

    var apps by mutableStateOf<AppsUi>(AppsUi.Idle)
        private set

    var deviceInfo by mutableStateOf<String?>(null)
        private set
    var deviceInfoLoading by mutableStateOf(false)
        private set

    /** آخر الأوامر المرسلة (الأحدث أولاً). */
    val commandLog = mutableStateListOf<String>()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        viewModelScope.launch {
            tv.events.collect { statusMessage = it }
        }
    }

    private fun log(text: String, ok: Boolean) {
        commandLog.add(0, "${timeFormat.format(Date())}  $text" + if (ok) "" else "  (فشل)")
        while (commandLog.size > MAX_LOG) commandLog.removeAt(commandLog.lastIndex)
    }

    fun clearLog() = commandLog.clear()

    fun clearStatus() {
        statusMessage = null
    }

    // ───────── الريموت ─────────

    fun send(command: TvCommand, repeat: Boolean = false) {
        val ok = tv.press(command, repeat)
        if (!repeat) {
            log(command.labelAr, ok)
            if (!ok) statusMessage = NOT_CONNECTED
        }
    }

    fun sendText(text: String) {
        when {
            text.isEmpty() -> statusMessage = "اكتب نصاً أولاً."
            !ShellCommands.isValidText(text) ->
                statusMessage = "المسموح: أحرف إنجليزية وأرقام ومسافات فقط (حتى 64 حرفاً). الأحرف العربية غير مدعومة عبر ADB."
            else -> {
                val ok = tv.typeText(text)
                log("كتابة نص", ok)
                if (!ok) statusMessage = NOT_CONNECTED
            }
        }
    }

    // ───────── الاقتران ─────────

    fun startDiscovery() = tv.discovery.acquire()
    fun stopDiscovery() = tv.discovery.release()

    fun reportPairingError(message: String) {
        pairingMessage = message
    }

    fun pair(host: String, pairPort: Int, code: String, connectPort: Int?) {
        if (busy) return
        busy = true
        pairingMessage = null
        viewModelScope.launch {
            val result = tv.pairAndConnect(host.trim(), pairPort, code.trim(), connectPort)
            pairingMessage = result.message
            busy = false
        }
    }

    fun connectDirect(host: String, port: Int) {
        if (busy) return
        busy = true
        pairingMessage = null
        viewModelScope.launch {
            val result = tv.connectDirect(host.trim(), port)
            pairingMessage = result.message
            busy = false
        }
    }

    fun connectSaved(device: SavedTv) {
        if (busy) return
        busy = true
        pairingMessage = null
        viewModelScope.launch {
            val result = tv.connectSaved(device)
            pairingMessage = result.message
            busy = false
        }
    }

    /** اتصال بخدمة اكتُشفت على الشبكة (التلفزيون مقترن مسبقاً بهذا الهاتف). */
    fun connectDiscovered(service: DiscoveredTv) = connectDirect(service.host, service.port)

    fun forget(device: SavedTv) = tv.forget(device)
    fun disconnect() = tv.disconnect()
    fun retry() = tv.retryNow()

    // ───────── التطبيقات والألعاب ─────────

    fun loadApps() {
        if (apps is AppsUi.Loading) return
        apps = AppsUi.Loading
        viewModelScope.launch {
            apps = try {
                AppsUi.Loaded(tv.listApps())
            } catch (e: Throwable) {
                AppsUi.Failed(e.toArabicMessage(Phase.COMMAND))
            }
        }
    }

    fun openApp(app: TvApp) {
        val ok = tv.openApp(app)
        log("فتح ${displayName(app)}", ok)
        if (!ok) statusMessage = NOT_CONNECTED
    }

    /** يفتح اللعبة ويفعّل ملف التحكم المرتبط بها إن وُجد. */
    fun openGame(app: TvApp) {
        pad.profileForGame(app.packageName)?.let { pad.setActive(it) }
        openApp(app)
    }

    fun setGame(packageName: String, isGame: Boolean) = pad.setGame(packageName, isGame)
    fun profileForGame(packageName: String): String? = pad.profileForGame(packageName)
    fun bindGame(packageName: String, profileId: String) = pad.bindGame(packageName, profileId)

    // ───────── معلومات التلفزيون ─────────

    fun loadDeviceInfo() {
        if (deviceInfoLoading) return
        deviceInfoLoading = true
        viewModelScope.launch {
            deviceInfo = try {
                formatDeviceInfo(tv.deviceInfo())
            } catch (e: Throwable) {
                e.toArabicMessage(Phase.COMMAND)
            }
            deviceInfoLoading = false
        }
    }

    private fun formatDeviceInfo(raw: String): String {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val model = lines.getOrNull(0) ?: "؟"
        val release = lines.getOrNull(1) ?: "؟"
        val sdk = lines.getOrNull(2) ?: "؟"
        val uinput = lines.any { it == "uinput=yes" }
        return buildString {
            append("الطراز: ").append(model).append('\n')
            append("أندرويد: ").append(release).append(" (API ").append(sdk).append(")\n")
            append("أداة uinput على التلفزيون: ").append(if (uinput) "موجودة" else "غير موجودة").append('\n')
            append("يد الألعاب في هذا الإصدار ترسل أزرار Gamepad عبر ADB (بدون عصا تناظرية حقيقية).")
        }
    }

    // ───────── الإعدادات ─────────

    fun setHaptics(value: Boolean) = settings.setHaptics(value)
    fun setClickSound(value: Boolean) = settings.setClickSound(value)
    fun setKeepAwake(value: Boolean) = settings.setKeepAwake(value)

    // ───────── يد الألعاب ─────────

    fun saveProfile(profile: PadProfile) = pad.save(profile)
    fun createProfile(name: String) {
        pad.create(name)
    }
    fun deleteProfile(id: String) = pad.delete(id)
    fun selectProfile(id: String) = pad.setActive(id)

    fun padPress(action: PadAction, repeat: Boolean) {
        val ok = tv.padAction(action, repeat)
        if (!repeat) {
            log(action.labelAr, ok)
            if (!ok) statusMessage = NOT_CONNECTED
        }
    }

    fun padStick(x: Float, y: Float) {
        tv.stick(x, y)
    }

    companion object {
        const val NOT_CONNECTED = "غير متصل بالتلفزيون. اقترن أو اتصل أولاً من صفحة اقتران التلفزيون."
        private const val MAX_LOG = 30

        fun displayName(app: TvApp): String =
            app.label.takeIf { it != app.packageName } ?: app.packageName.substringAfterLast('.')
    }
}
