package com.herotoner.herotv.adb

/** تلفزيون محفوظ بعد اقتران/اتصال ناجح. لا يحتوي أي أسرار: المفاتيح تُحفظ منفصلة في KeyVault. */
data class SavedTv(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val serviceName: String?,
    val lastConnectedAt: Long
)

enum class ServiceKind { CONNECT, PAIRING, LEGACY }

/** خدمة ADB عُثر عليها عبر mDNS على الشبكة المحلية. */
data class DiscoveredTv(
    val serviceName: String,
    val host: String,
    val port: Int,
    val kind: ServiceKind
)

/** تطبيق مثبّت على التلفزيون كما تُرجعه أوامر النظام. */
data class TvApp(
    val packageName: String,
    val component: String,
    val label: String
)

class TvResult(val ok: Boolean, val message: String)
