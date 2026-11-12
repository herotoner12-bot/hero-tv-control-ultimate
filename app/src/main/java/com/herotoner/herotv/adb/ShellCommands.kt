package com.herotoner.herotv.adb

import java.util.Locale

/**
 * المكان الوحيد الذي تُبنى فيه أسطر Shell المرسلة إلى التلفزيون.
 * لا يمر نص حر من المستخدم إلى Shell: كل أمر قالب ثابت مع أعداد صحيحة أو قيم تُتحقق بنمط صارم.
 */
object ShellCommands {

    private val COMPONENT = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+/[A-Za-z0-9_.$]+$")
    private val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")

    /** source: "gamepad" أو "keyboard" أو null (الافتراضي). */
    fun keyEvent(source: String?, keyCode: Int): String {
        require(keyCode in 1..400) { "keyCode غير صالح" }
        return when (source) {
            null, "", "keyboard" -> "input keyevent $keyCode"
            "gamepad" -> "input gamepad keyevent $keyCode"
            else -> throw IllegalArgumentException("مصدر غير مسموح")
        }
    }

    /** حركة عصا تناظرية يسرى: قيم -1..1 للمحورين X وY. تجريبية وتعتمد على قبول التلفزيون/اللعبة. */
    fun joystickMove(x: Float, y: Float): String {
        val cx = x.coerceIn(-1f, 1f)
        val cy = y.coerceIn(-1f, 1f)
        return String.format(Locale.US, "input joystick motionevent MOVE %.2f %.2f", cx, cy)
    }

    fun startComponent(component: String): String {
        require(COMPONENT.matches(component)) { "اسم مكوّن غير صالح" }
        return "am start -n $component"
    }

    fun startPackage(packageName: String): String {
        require(PACKAGE.matches(packageName)) { "اسم حزمة غير صالح" }
        return "monkey -p $packageName -c android.intent.category.LEANBACK_LAUNCHER 1"
    }

    fun queryLauncherActivities(category: String): String {
        require(category == "android.intent.category.LEANBACK_LAUNCHER" || category == "android.intent.category.LAUNCHER")
        return "cmd package query-activities --brief -a android.intent.action.MAIN -c $category"
    }

    private val TEXT = Regex("^[A-Za-z0-9 ]{1,64}$")

    /** `input text` لا يدعم إلا ASCII بأمان؛ لذلك نقبل أحرفاً إنجليزية وأرقاماً ومسافات فقط. */
    fun isValidText(value: String) = TEXT.matches(value)

    fun inputText(text: String): String {
        require(TEXT.matches(text)) { "نص غير مسموح" }
        return "input text " + text.replace(" ", "%s")
    }

    /** أمر ثابت للقراءة فقط: طراز التلفزيون، إصدار أندرويد، مستوى API، ووجود أداة uinput. */
    const val DEVICE_INFO =
        "getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.version.sdk; " +
            "if [ -e /system/bin/uinput ]; then echo uinput=yes; else echo uinput=no; fi"

    const val LIST_THIRD_PARTY_PACKAGES = "pm list packages -3"
    const val DEVICE_MODEL = "getprop ro.product.model"
    const val PING = "echo ok"

    fun isValidComponent(value: String) = COMPONENT.matches(value)
    fun isValidPackage(value: String) = PACKAGE.matches(value)
}
