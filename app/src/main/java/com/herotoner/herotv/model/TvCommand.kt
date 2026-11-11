package com.herotoner.herotv.model

/**
 * القائمة البيضاء للأوامر المسموحة (نفس أرقام مشروع Termux القديم في legacy-termux/src/commands.js).
 * كل رقم هو android.view.KeyEvent القياسي. لا يُسمح بأي أمر خارج هذه القائمة.
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
    PLAY_PAUSE("play_pause", 85, "تشغيل / إيقاف مؤقت");

    companion object {
        fun fromId(id: String): TvCommand? = entries.firstOrNull { it.id == id }
    }
}
