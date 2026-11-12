package com.herotoner.herotv.adb

import io.github.muntashirakon.adb.AdbAuthenticationFailedException
import io.github.muntashirakon.adb.AdbPairingRequiredException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

enum class Phase { PAIRING, CONNECTING, COMMAND }

private val IPV4 = Regex("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$")

fun isValidIpv4(value: String): Boolean = IPV4.matches(value)
fun isValidPort(value: Int): Boolean = value in 1..65535

/** يحوّل أي خطأ تقني إلى رسالة عربية مفهومة. لا تُسجَّل رموز الاقتران في أي رسالة. */
fun Throwable.toArabicMessage(phase: Phase): String {
    var cause: Throwable? = this
    while (cause != null) {
        when (cause) {
            is AdbPairingRequiredException ->
                return "هذا الجهاز لم يُقترن بعد. نفّذ الاقتران أولاً (التلفزيون ← التصحيح اللاسلكي ← اقتران جهاز برمز)."
            is AdbAuthenticationFailedException ->
                return "رفض التلفزيون المفتاح. وافق على نافذة «السماح بالتصحيح» على شاشة التلفزيون ثم أعد المحاولة."
            is SocketTimeoutException, is ConnectException, is NoRouteToHostException, is UnknownHostException ->
                return "تعذر الوصول إلى التلفزيون. تأكد أن الهاتف والتلفزيون على شبكة Wi-Fi نفسها، وأن التصحيح اللاسلكي " +
                    "مفعّل، وأن العنوان والمنفذ صحيحان (المنفذ يتغير كلما أُعيد تفعيل التصحيح اللاسلكي). " +
                    "إذا كان VPN يعمل على الهاتف فجرّب إيقافه."
            is SSLException ->
                if (phase == Phase.PAIRING) {
                    return "فشل الاقتران. تأكد من الرمز المكوّن من 6 أرقام، ومن أن نافذة الاقتران ما زالت مفتوحة على التلفزيون " +
                        "(الرمز والمنفذ يتغيران في كل مرة تفتح فيها النافذة)."
                }
            is SocketException ->
                return "انقطع الاتصال بالتلفزيون."
        }
        cause = cause.cause
    }
    val text = (message ?: "").lowercase()
    return when {
        phase == Phase.PAIRING &&
            (text.contains("exchang") || text.contains("peer info") || text.contains("pair")) ->
            "فشل الاقتران. تأكد من الرمز المكوّن من 6 أرقام ومن أن نافذة الاقتران ما زالت مفتوحة على التلفزيون."
        text.contains("not connected") || text.contains("closed") ->
            "انقطع الاتصال بالتلفزيون."
        text.contains("timed out") || text.contains("timeout") ->
            "انتهت مهلة الاتصال بالتلفزيون."
        phase == Phase.PAIRING -> "فشل الاقتران بسبب خطأ غير متوقع."
        phase == Phase.CONNECTING -> "تعذر الاتصال بالتلفزيون بسبب خطأ غير متوقع."
        else -> "تعذر تنفيذ الأمر على التلفزيون."
    }
}
