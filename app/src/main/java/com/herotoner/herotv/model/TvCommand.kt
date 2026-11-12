package com.herotoner.herotv.model

/**
 * القائمة البيضاء لأوامر الريموت (أرقام android.view.KeyEvent القياسية).
 * أول 12 أمراً هي نفس أوامر مشروع Termux القديم في legacy-termux/src/commands.js.
 * BACKSPACE وENTER لإدخال النصوص.
 */
enum class TvCommand(val id: String, val keyCode: Int, val labelAr: String) {
    UP("up", 19, "أعلى"),
    DOWN("down", 20, "أسفل"),
    LEFT("left", 21, "يسار"),
    RIGHT("right", 22, "يمين"),
    SELECT("select", 23, "OK"),
    BACK("back", 4, "رجوع"),
    HOME("home", 3, "الرئيسية"),
    MENU("menu", 82, "القائمة"),
    VOLUME_UP("volume_up", 24, "رفع الصوت"),
    VOLUME_DOWN("volume_down", 25, "خفض الصوت"),
    MUTE("mute", 164, "كتم"),
    PLAY_PAUSE("play_pause", 85, "تشغيل / إيقاف مؤقت"),
    BACKSPACE("backspace", 67, "مسح حرف"),
    ENTER("enter", 66, "Enter");

    companion object {
        fun fromId(id: String): TvCommand? = entries.firstOrNull { it.id == id }
    }
}
