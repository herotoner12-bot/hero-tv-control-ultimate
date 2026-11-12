package com.herotoner.herotv.adb

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.model.TvCommand
import com.herotoner.herotv.pad.PadAction
import io.github.muntashirakon.adb.AdbStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * قلب الاتصال بالتلفزيون: اقتران ADB اللاسلكي، الاتصال، إعادة الاتصال، وتنفيذ الأوامر.
 *
 * - كل نداءات ADB المحجوبة تجري على خيط مخصص أو على Dispatchers.IO، فلا تُجمّد الواجهة.
 * - كل أسطر Shell تُبنى من ShellCommands فقط (قائمة بيضاء).
 * - رمز الاقتران يمر مباشرة إلى المكتبة ولا يُخزَّن ولا يُسجَّل.
 */
class TvRepository(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val adbExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "hero-adb").apply { isDaemon = true }
    }
    private val connectMutex = Mutex()

    private val keyVault = KeyVault(app)
    private val managerLazy = lazy {
        HeroAdbManager(keyVault.loadOrCreate()).also { it.setTimeout(15, TimeUnit.SECONDS) }
    }
    private val manager: HeroAdbManager get() = managerLazy.value

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val devices = DevicesStore(app)
    val discovery = NsdDiscovery(app)

    @Volatile private var activeDevice: SavedTv? = null
    @Volatile private var userDisconnected = false
    @Volatile private var wifiUp = false
    @Volatile private var session: ShellSession? = null
    private var reconnectJob: Job? = null
    private var monitorJob: Job? = null
    private val queued = AtomicInteger(0)

    private val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        registerWifiCallback()
        devices.last()?.let { last ->
            // إعادة الاتصال تلقائياً بآخر تلفزيون عند تشغيل التطبيق
            activeDevice = last
            userDisconnected = false
            scheduleReconnect()
        }
    }

    // ───────────────────────── الشبكة ─────────────────────────

    private fun registerWifiCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        try {
            connectivity.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // نربط عمليات التطبيق بشبكة Wi-Fi المحلية كي لا يمر الاتصال بالتلفزيون عبر VPN
                    runCatching { connectivity.bindProcessToNetwork(network) }
                    wifiUp = true
                    if (activeDevice != null && !userDisconnected && _connection.value !is ConnectionState.Connected) {
                        scheduleReconnect()
                    }
                }

                override fun onLost(network: Network) {
                    runCatching { connectivity.bindProcessToNetwork(null) }
                    wifiUp = false
                    scope.launch(Dispatchers.IO) { handleConnectionLost("انقطعت شبكة Wi-Fi. سأعيد الاتصال تلقائياً عند عودتها.") }
                }
            })
        } catch (e: Exception) {
            wifiUp = true
        }
    }

    // ───────────────────────── الاقتران والاتصال ─────────────────────────

    /** اقتران جديد برمز ثم اتصال. connectPort اختياري: إن لم يُعطَ نبحث عنه عبر mDNS. */
    suspend fun pairAndConnect(host: String, pairPort: Int, code: String, connectPort: Int?): TvResult =
        withContext(Dispatchers.IO) {
            if (!isValidIpv4(host)) return@withContext TvResult(false, "عنوان IP غير صالح.")
            if (!isValidPort(pairPort)) return@withContext TvResult(false, "منفذ الاقتران غير صالح.")
            if (connectPort != null && !isValidPort(connectPort)) return@withContext TvResult(false, "منفذ الاتصال غير صالح.")
            if (!Regex("^\\d{6}$").matches(code)) return@withContext TvResult(false, "رمز الاقتران يجب أن يكون 6 أرقام.")

            connectMutex.withLock {
                userDisconnected = false
                _connection.value = ConnectionState.Connecting
                try {
                    probePort(host, pairPort, 4000)
                    manager.pair(host, pairPort, code)
                } catch (e: Throwable) {
                    val message = e.toArabicMessage(Phase.PAIRING)
                    _connection.value = ConnectionState.Error(message)
                    return@withLock TvResult(false, message)
                }

                val service = if (connectPort == null) findConnectService(host, null, 10_000) else null
                val port = connectPort ?: service?.port
                if (port == null) {
                    val message = "تم الاقتران بنجاح، لكن لم أجد منفذ الاتصال تلقائياً. " +
                        "أدخل «منفذ الاتصال» الظاهر في شاشة التصحيح اللاسلكي على التلفزيون ثم اضغط اتصال."
                    _connection.value = ConnectionState.Error(message)
                    return@withLock TvResult(false, message)
                }
                connectLocked(host, port, service?.serviceName, 15)
            }
        }

    /** اتصال مباشر (اقتران سابق أو «تصحيح الشبكة» على منفذ مثل 5555). */
    suspend fun connectDirect(host: String, port: Int, waitForApprovalSeconds: Long = 45): TvResult =
        withContext(Dispatchers.IO) {
            if (!isValidIpv4(host)) return@withContext TvResult(false, "عنوان IP غير صالح.")
            if (!isValidPort(port)) return@withContext TvResult(false, "المنفذ غير صالح.")
            connectMutex.withLock {
                userDisconnected = false
                _connection.value = ConnectionState.Connecting
                connectLocked(host, port, null, waitForApprovalSeconds)
            }
        }

    suspend fun connectSaved(device: SavedTv): TvResult = withContext(Dispatchers.IO) {
        connectMutex.withLock {
            userDisconnected = false
            _connection.value = ConnectionState.Connecting
            connectWithRediscovery(device, 15)
        }
    }

    /** يجب استدعاؤها داخل connectMutex. */
    private suspend fun connectLocked(host: String, port: Int, serviceName: String?, timeoutSec: Long): TvResult {
        return try {
            connectBlocking(host, port, timeoutSec)
            onConnected(host, port, serviceName)
            TvResult(true, "تم الاتصال.")
        } catch (e: Throwable) {
            val message = e.toArabicMessage(Phase.CONNECTING)
            _connection.value = ConnectionState.Error(message)
            TvResult(false, message)
        }
    }

    private suspend fun connectWithRediscovery(device: SavedTv, timeoutSec: Long): TvResult {
        try {
            connectBlocking(device.host, device.port, timeoutSec)
            onConnected(device.host, device.port, device.serviceName, device.name)
            return TvResult(true, "تم الاتصال.")
        } catch (first: Throwable) {
            // منفذ التصحيح اللاسلكي يتغير عند كل تفعيل، وعنوان IP قد يتغير بالـ DHCP: نبحث عن الخدمة من جديد
            val found = findConnectService(device.host, device.serviceName, 8_000)
            if (found != null && (found.port != device.port || found.host != device.host)) {
                try {
                    connectBlocking(found.host, found.port, timeoutSec)
                    onConnected(found.host, found.port, found.serviceName, device.name)
                    return TvResult(true, "تم الاتصال.")
                } catch (second: Throwable) {
                    val message = second.toArabicMessage(Phase.CONNECTING)
                    _connection.value = ConnectionState.Error(message)
                    return TvResult(false, message)
                }
            }
            val message = first.toArabicMessage(Phase.CONNECTING)
            _connection.value = ConnectionState.Error(message)
            return TvResult(false, message)
        }
    }

    private fun connectBlocking(host: String, port: Int, timeoutSec: Long) {
        closeSession()
        runCatching { if (managerLazy.isInitialized()) manager.disconnect() }
        probePort(host, port, 4000)
        manager.setTimeout(timeoutSec, TimeUnit.SECONDS)
        val ok = manager.connect(host, port)
        if (!ok || !manager.isConnected) throw IOException("لم يكتمل الاتصال")
    }

    private fun probePort(host: String, port: Int, timeoutMs: Int) {
        Socket().use { it.connect(InetSocketAddress(host, port), timeoutMs) }
    }

    private suspend fun onConnected(host: String, port: Int, serviceName: String?, knownName: String? = null) {
        val name = knownName ?: readDeviceName() ?: "تلفزيون $host"
        val device = SavedTv(
            id = serviceName ?: host,
            name = name,
            host = host,
            port = port,
            serviceName = serviceName,
            lastConnectedAt = System.currentTimeMillis()
        )
        devices.upsert(device)
        devices.setLast(device.id)
        activeDevice = device
        _connection.value = ConnectionState.Connected(name, "$host:$port")
        startMonitor()
    }

    private suspend fun readDeviceName(): String? = withContext(Dispatchers.IO) {
        runCatching { runShell(ShellCommands.DEVICE_MODEL, 5000).trim() }
            .getOrNull()
            ?.filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' || it == '.' }
            ?.take(40)
            ?.takeIf { it.isNotBlank() }
    }

    private suspend fun findConnectService(host: String?, serviceName: String?, timeoutMs: Long): DiscoveredTv? {
        discovery.acquire()
        try {
            return withTimeoutOrNull(timeoutMs) {
                discovery.found
                    .map { list ->
                        list.firstOrNull {
                            it.kind == ServiceKind.CONNECT &&
                                ((serviceName != null && it.serviceName == serviceName) || (host != null && it.host == host))
                        }
                    }
                    .filterNotNull()
                    .first()
            }
        } finally {
            discovery.release()
        }
    }

    fun disconnect() {
        userDisconnected = true
        reconnectJob?.cancel()
        monitorJob?.cancel()
        scope.launch(Dispatchers.IO) {
            closeSession()
            runCatching { if (managerLazy.isInitialized()) manager.disconnect() }
            _connection.value = ConnectionState.Disconnected
        }
    }

    fun forget(device: SavedTv) {
        if (activeDevice?.id == device.id) {
            activeDevice = null
            disconnect()
        }
        devices.remove(device.id)
    }

    fun retryNow() {
        val device = activeDevice ?: devices.last() ?: return
        activeDevice = device
        userDisconnected = false
        reconnectJob?.cancel()
        scheduleReconnect()
    }

    // ───────────────────────── إعادة الاتصال والمراقبة ─────────────────────────

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch(Dispatchers.IO) {
            var attempt = 0
            while (isActive && !userDisconnected) {
                val device = activeDevice ?: return@launch
                attempt++
                _connection.value = ConnectionState.Reconnecting(attempt)
                if (wifiUp) {
                    val result = connectMutex.withLock {
                        if (_connection.value is ConnectionState.Connected) TvResult(true, "") else {
                            _connection.value = ConnectionState.Reconnecting(attempt)
                            connectWithRediscovery(device, 10)
                        }
                    }
                    if (result.ok) {
                        _events.tryEmit("تمت إعادة الاتصال بالتلفزيون.")
                        return@launch
                    }
                    if (userDisconnected) return@launch
                }
                if (attempt >= MAX_RECONNECT_ATTEMPTS) {
                    _connection.value = ConnectionState.Error(
                        "تعذر إعادة الاتصال تلقائياً. تأكد أن التلفزيون يعمل والتصحيح اللاسلكي مفعّل ثم اضغط إعادة المحاولة."
                    )
                    return@launch
                }
                _connection.value = ConnectionState.Reconnecting(attempt)
                delay(minOf(2_000L * attempt, 15_000L))
            }
        }
    }

    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(12_000)
                if (_connection.value !is ConnectionState.Connected) continue
                val alive = runCatching { runShell(ShellCommands.PING, 4000).contains("ok") }.getOrDefault(false)
                if (!alive) {
                    handleConnectionLost("انقطع الاتصال بالتلفزيون. أعيد المحاولة…")
                    return@launch
                }
            }
        }
    }

    private suspend fun handleConnectionLost(message: String) {
        if (_connection.value is ConnectionState.Disconnected && activeDevice == null) return
        _events.tryEmit(message)
        monitorJob?.cancel()
        closeSession()
        runCatching { if (managerLazy.isInitialized()) manager.disconnect() }
        if (userDisconnected || activeDevice == null) {
            _connection.value = ConnectionState.Disconnected
        } else {
            _connection.value = ConnectionState.Reconnecting(0)
            scheduleReconnect()
        }
    }

    // ───────────────────────── تنفيذ الأوامر (قائمة بيضاء فقط) ─────────────────────────

    /** ضغطة ريموت. repeat=true يعني ضغطة متكررة أثناء الضغط المستمر (قد تُهمل إذا ازدحم الطابور). */
    fun press(command: TvCommand, repeat: Boolean = false): Boolean =
        inject(ShellCommands.keyEvent(null, command.keyCode), important = !repeat)

    fun padAction(action: PadAction, repeat: Boolean = false): Boolean {
        if (action == PadAction.NONE) return false
        return inject(ShellCommands.keyEvent(action.source, action.keyCode), important = !repeat)
    }

    fun stick(x: Float, y: Float): Boolean = inject(ShellCommands.joystickMove(x, y), important = false)

    fun typeText(text: String): Boolean {
        if (!ShellCommands.isValidText(text)) return false
        return inject(ShellCommands.inputText(text), important = true)
    }

    suspend fun deviceInfo(): String = withContext(Dispatchers.IO) {
        if (!isConnected) throw IOException("غير متصل بالتلفزيون")
        runShell(ShellCommands.DEVICE_INFO, 8_000)
    }

    fun openApp(app: TvApp): Boolean {
        val line = if (ShellCommands.isValidComponent(app.component)) {
            ShellCommands.startComponent(app.component)
        } else {
            ShellCommands.startPackage(app.packageName)
        }
        return inject(line, important = true)
    }

    val isConnected: Boolean get() = _connection.value is ConnectionState.Connected

    private fun inject(line: String, important: Boolean): Boolean {
        if (_connection.value !is ConnectionState.Connected) return false
        if (!important && queued.get() >= 4) return true // تخفيف الازدحام: نُسقط الأحداث المتكررة
        queued.incrementAndGet()
        adbExecutor.execute {
            try {
                val shell = ensureSession()
                if (shell != null) {
                    shell.write(line, important)
                } else {
                    runShell(line, 5000)
                }
            } catch (e: Throwable) {
                scope.launch(Dispatchers.IO) {
                    handleConnectionLost(e.toArabicMessage(Phase.COMMAND))
                }
            } finally {
                queued.decrementAndGet()
            }
        }
        return true
    }

    /** يُستدعى فقط من خيط ADB. */
    private fun ensureSession(): ShellSession? {
        session?.let { if (it.alive) return it }
        return try {
            ShellSession(manager.openStream("shell:")).also { session = it }
        } catch (e: Throwable) {
            null
        }
    }

    private fun closeSession() {
        session?.close()
        session = null
    }

    /** يشغّل أمراً ويقرأ ناتجه حتى نهايته أو انتهاء المهلة. يُستدعى من خيط غير الواجهة. */
    fun runShell(command: String, timeoutMs: Long): String {
        val stream = manager.openStream("shell:$command")
        val timedOut = AtomicBoolean(false)
        val watchdog = scope.launch {
            delay(timeoutMs)
            timedOut.set(true)
            runCatching { stream.close() }
        }
        try {
            val input = stream.openInputStream()
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val n = try {
                    input.read(buffer)
                } catch (e: IOException) {
                    -1
                }
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_OUTPUT_BYTES) break
            }
            if (timedOut.get()) throw SocketTimeoutException("انتهت مهلة الأمر")
            return out.toString(Charsets.UTF_8.name())
        } finally {
            watchdog.cancel()
            runCatching { stream.close() }
        }
    }

    suspend fun listApps(): List<TvApp> = withContext(Dispatchers.IO) {
        if (!isConnected) throw IOException("غير متصل بالتلفزيون")
        val components = LinkedHashMap<String, String>() // package -> component
        for (category in listOf("android.intent.category.LEANBACK_LAUNCHER", "android.intent.category.LAUNCHER")) {
            val output = runShell(ShellCommands.queryLauncherActivities(category), 20_000)
            output.lineSequence().map { it.trim() }.forEach { line ->
                if (ShellCommands.isValidComponent(line)) {
                    val pkg = line.substringBefore('/')
                    components.putIfAbsent(pkg, line)
                }
            }
        }
        if (components.isEmpty()) {
            // احتياط للأنظمة التي لا تدعم query-activities: نعرض التطبيقات المثبتة من المستخدم ونفتحها عبر monkey
            runShell(ShellCommands.LIST_THIRD_PARTY_PACKAGES, 15_000).lineSequence()
                .map { it.trim().removePrefix("package:") }
                .filter { ShellCommands.isValidPackage(it) }
                .forEach { components.putIfAbsent(it, "") }
        }
        components.map { (pkg, component) -> TvApp(pkg, component, AppNames.labelFor(pkg)) }
            .sortedBy { it.label.lowercase() }
    }

    // ───────────────────────── جلسة Shell دائمة ─────────────────────────

    /**
     * جلسة shell تفاعلية واحدة تبقى مفتوحة لتقليل زمن الاستجابة. بعد كل أمر نضيف علامة
     * (echo HEROX$((1+1))) ونعدّ ظهورها في المخرجات لمعرفة كم أمراً ما زال قيد التنفيذ.
     */
    private inner class ShellSession(private val stream: AdbStream) {
        private val output = stream.openOutputStream()
        private val pending = AtomicInteger(0)
        @Volatile private var lastActivity = System.currentTimeMillis()
        @Volatile var alive = true
        @Volatile private var closing = false

        init {
            Thread({ readLoop() }, "hero-adb-read").apply { isDaemon = true }.start()
        }

        private fun readLoop() {
            val input = stream.openInputStream()
            val buffer = ByteArray(2048)
            val acc = StringBuilder()
            try {
                while (!closing) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    acc.append(String(buffer, 0, n, Charsets.UTF_8))
                    var index = acc.indexOf(MARKER_DONE)
                    while (index >= 0) {
                        pending.updateAndGet { if (it > 0) it - 1 else 0 }
                        lastActivity = System.currentTimeMillis()
                        acc.delete(0, index + MARKER_DONE.length)
                        index = acc.indexOf(MARKER_DONE)
                    }
                    if (acc.length > 256) acc.delete(0, acc.length - 32)
                }
            } catch (e: Throwable) {
                // الجلسة انتهت؛ الأمر التالي سيفتح جلسة جديدة أو يكشف انقطاع الاتصال
            }
            alive = false
        }

        fun write(line: String, important: Boolean) {
            val limit = if (important) 8 else 3
            val now = System.currentTimeMillis()
            if (pending.get() > 0 && now - lastActivity > 3000) pending.set(0) // لا نعلق إن لم تصل العلامة
            if (pending.get() >= limit) return
            pending.incrementAndGet()
            try {
                output.write("$line; echo $MARKER_CMD\n".toByteArray(Charsets.UTF_8))
                output.flush()
            } catch (e: IOException) {
                alive = false
                pending.decrementAndGet()
                throw e
            }
        }

        fun close() {
            closing = true
            alive = false
            runCatching { stream.close() }
        }
    }

    private companion object {
        const val MARKER_CMD = "HEROX\$((1+1))"
        const val MARKER_DONE = "HEROX2"
        const val MAX_OUTPUT_BYTES = 512 * 1024
        const val MAX_RECONNECT_ATTEMPTS = 20
    }
}

/** أسماء عرض لحزم معروفة فقط. أي حزمة أخرى تُعرض باسم حزمتها الحقيقي دون اختراع اسم. */
object AppNames {
    private val known = mapOf(
        "com.google.android.youtube.tv" to "YouTube",
        "com.google.android.youtube.tvunplugged" to "YouTube TV",
        "com.netflix.ninja" to "Netflix",
        "com.amazon.amazonvideo.livingroom" to "Prime Video",
        "com.disney.disneyplus" to "Disney+",
        "com.android.vending" to "Google Play",
        "com.spotify.tv.android" to "Spotify",
        "com.android.tv.settings" to "إعدادات التلفزيون"
    )

    fun labelFor(packageName: String): String = known[packageName] ?: packageName
}
