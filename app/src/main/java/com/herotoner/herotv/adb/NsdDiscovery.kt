package com.herotoner.herotv.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address

/**
 * بحث mDNS عن خدمات ADB على الشبكة المحلية.
 *  - _adb-tls-connect._tcp : التصحيح اللاسلكي مفعّل (منفذ الاتصال يتغير عند كل تفعيل)
 *  - _adb-tls-pairing._tcp : نافذة «اقتران بالرمز» مفتوحة حالياً على التلفزيون
 *  - _adb._tcp             : خدمة ADB التقليدية إن أعلنها الجهاز
 * التفعيل بالعدّ المرجعي: acquire() يبدأ البحث عند أول طالب، و release() يوقفه عند آخر طالب.
 */
class NsdDiscovery(context: Context) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val lock = Any()

    private val _found = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val found: StateFlow<List<DiscoveredTv>> = _found.asStateFlow()

    private var users = 0
    private val listeners = mutableListOf<NsdManager.DiscoveryListener>()
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    fun acquire() {
        synchronized(lock) {
            users++
            if (users > 1) return
            _found.value = emptyList()
            SERVICES.forEach { (type, kind) ->
                val listener = DiscoveryListener(kind)
                try {
                    nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                    listeners.add(listener)
                } catch (_: Exception) {
                    // نتجاهل: يبقى الإدخال اليدوي متاحاً
                }
            }
        }
    }

    fun release() {
        synchronized(lock) {
            if (users == 0) return
            users--
            if (users > 0) return
            listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
            listeners.clear()
            resolveQueue.clear()
            resolving = false
        }
    }

    private fun enqueue(info: NsdServiceInfo) {
        synchronized(lock) {
            resolveQueue.addLast(info)
            resolveNext()
        }
    }

    // لا يُسمح بأكثر من عملية resolve واحدة في نفس الوقت
    private fun resolveNext() {
        if (resolving) return
        val next = resolveQueue.removeFirstOrNull() ?: return
        resolving = true
        try {
            @Suppress("DEPRECATION")
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    synchronized(lock) {
                        resolving = false
                        resolveNext()
                    }
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    synchronized(lock) {
                        record(serviceInfo)
                        resolving = false
                        resolveNext()
                    }
                }
            })
        } catch (_: Exception) {
            resolving = false
        }
    }

    private fun hostOf(info: NsdServiceInfo): String? {
        if (Build.VERSION.SDK_INT >= 34) {
            val v4 = info.hostAddresses.firstOrNull { it is Inet4Address }
            if (v4 != null) return v4.hostAddress
        }
        @Suppress("DEPRECATION")
        val single = info.host
        return (single as? Inet4Address)?.hostAddress
    }

    private fun kindOf(info: NsdServiceInfo): ServiceKind? {
        val type = info.serviceType ?: return null
        return when {
            type.contains("adb-tls-connect") -> ServiceKind.CONNECT
            type.contains("adb-tls-pairing") -> ServiceKind.PAIRING
            type.contains("_adb.") || type.contains("_adb._tcp") -> ServiceKind.LEGACY
            else -> null
        }
    }

    private fun record(info: NsdServiceInfo) {
        val host = hostOf(info) ?: return
        val kind = kindOf(info) ?: return
        val entry = DiscoveredTv(info.serviceName, host, info.port, kind)
        _found.value = _found.value
            .filterNot { it.serviceName == entry.serviceName && it.kind == entry.kind } + entry
    }

    private fun remove(name: String, kind: ServiceKind) {
        _found.value = _found.value.filterNot { it.serviceName == name && it.kind == kind }
    }

    private inner class DiscoveryListener(private val kind: ServiceKind) : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            enqueue(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            synchronized(lock) { remove(serviceInfo.serviceName, kind) }
        }
    }

    private companion object {
        val SERVICES = listOf(
            "_adb-tls-connect._tcp" to ServiceKind.CONNECT,
            "_adb-tls-pairing._tcp" to ServiceKind.PAIRING,
            "_adb._tcp" to ServiceKind.LEGACY
        )
    }
}
