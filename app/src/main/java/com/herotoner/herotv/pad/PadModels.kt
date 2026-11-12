package com.herotoner.herotv.pad

/**
 * كل ما يمكن لزر في يد الألعاب أن يرسله. قائمة مغلقة: لا يوجد إدخال حر.
 * source = "gamepad" يرسل الحدث بمصدر Gamepad (أرقام KeyEvent القياسية BUTTON_A=96 ...)،
 * و"keyboard" يرسله كمفتاح ريموت عادي.
 */
enum class PadAction(val id: String, val labelAr: String, val source: String, val keyCode: Int) {
    NONE("none", "بدون وظيفة", "", 0),

    BTN_A("btn_a", "زر A", "gamepad", 96),
    BTN_B("btn_b", "زر B", "gamepad", 97),
    BTN_X("btn_x", "زر X", "gamepad", 99),
    BTN_Y("btn_y", "زر Y", "gamepad", 100),
    BTN_L1("btn_l1", "L1", "gamepad", 102),
    BTN_R1("btn_r1", "R1", "gamepad", 103),
    BTN_L2("btn_l2", "L2", "gamepad", 104),
    BTN_R2("btn_r2", "R2", "gamepad", 105),
    BTN_THUMBL("btn_thumbl", "ضغط العصا اليسرى", "gamepad", 106),
    BTN_THUMBR("btn_thumbr", "ضغط العصا اليمنى", "gamepad", 107),
    BTN_START("btn_start", "START", "gamepad", 108),
    BTN_SELECT("btn_select", "SELECT", "gamepad", 109),
    BTN_MODE("btn_mode", "MODE", "gamepad", 110),

    DPAD_UP("dpad_up", "D-pad أعلى", "gamepad", 19),
    DPAD_DOWN("dpad_down", "D-pad أسفل", "gamepad", 20),
    DPAD_LEFT("dpad_left", "D-pad يسار", "gamepad", 21),
    DPAD_RIGHT("dpad_right", "D-pad يمين", "gamepad", 22),
    DPAD_CENTER("dpad_center", "D-pad OK", "gamepad", 23),

    TV_BACK("tv_back", "رجوع (ريموت)", "keyboard", 4),
    TV_HOME("tv_home", "الرئيسية (ريموت)", "keyboard", 3),
    TV_MENU("tv_menu", "القائمة (ريموت)", "keyboard", 82),
    TV_PLAY_PAUSE("tv_play_pause", "تشغيل/إيقاف (ريموت)", "keyboard", 85),
    TV_VOL_UP("tv_vol_up", "رفع الصوت", "keyboard", 24),
    TV_VOL_DOWN("tv_vol_down", "خفض الصوت", "keyboard", 25),
    TV_MUTE("tv_mute", "كتم الصوت", "keyboard", 164);

    companion object {
        fun fromId(id: String?): PadAction? = entries.firstOrNull { it.id == id }
    }
}

enum class PadButton(
    val id: String,
    val label: String,
    val defaultAction: PadAction,
    val defaultX: Float,
    val defaultY: Float
) {
    L2("l2", "L2", PadAction.BTN_L2, 0.09f, 0.27f),
    L1("l1", "L1", PadAction.BTN_L1, 0.21f, 0.27f),
    R1("r1", "R1", PadAction.BTN_R1, 0.79f, 0.27f),
    R2("r2", "R2", PadAction.BTN_R2, 0.91f, 0.27f),

    DPAD_UP("dpad_up", "▲", PadAction.DPAD_UP, 0.30f, 0.58f),
    DPAD_DOWN("dpad_down", "▼", PadAction.DPAD_DOWN, 0.30f, 0.86f),
    DPAD_LEFT("dpad_left", "◀", PadAction.DPAD_LEFT, 0.22f, 0.72f),
    DPAD_RIGHT("dpad_right", "▶", PadAction.DPAD_RIGHT, 0.38f, 0.72f),

    Y("y", "Y", PadAction.BTN_Y, 0.82f, 0.46f),
    A("a", "A", PadAction.BTN_A, 0.82f, 0.78f),
    X("x", "X", PadAction.BTN_X, 0.74f, 0.62f),
    B("b", "B", PadAction.BTN_B, 0.90f, 0.62f),

    SELECT("select", "SELECT", PadAction.BTN_SELECT, 0.44f, 0.50f),
    START("start", "START", PadAction.BTN_START, 0.56f, 0.50f),
    PAUSE("pause", "PAUSE", PadAction.TV_PLAY_PAUSE, 0.50f, 0.68f);

    companion object {
        fun fromId(id: String): PadButton? = entries.firstOrNull { it.id == id }
    }
}

enum class StickMode(val id: String, val labelAr: String) {
    /** تحويل اتجاه العصا إلى ضغطات D-pad متكررة: يعمل مع واجهة التلفزيون والألعاب التي تقبل D-pad. */
    DIGITAL("digital", "رقمي (اتجاهات)"),

    /** محاولة إرسال حدث محور حقيقي X/Y عبر `input joystick motionevent`. تجريبي ولا تضمنه كل الأجهزة والألعاب. */
    ANALOG("analog", "تناظري (تجريبي)");

    companion object {
        fun fromId(id: String?): StickMode = entries.firstOrNull { it.id == id } ?: DIGITAL
    }
}

data class Pos(val x: Float, val y: Float)

const val STICK_ID = "stick"
val STICK_DEFAULT = Pos(0.17f, 0.40f)

data class PadProfile(
    val id: String,
    val name: String,
    val stickMode: StickMode = StickMode.DIGITAL,
    val holdRepeat: Boolean = true,
    val actions: Map<String, String> = emptyMap(),
    val positions: Map<String, Pos> = emptyMap()
) {
    fun actionFor(button: PadButton): PadAction =
        PadAction.fromId(actions[button.id]) ?: button.defaultAction

    fun positionFor(button: PadButton): Pos =
        positions[button.id] ?: Pos(button.defaultX, button.defaultY)

    fun stickPosition(): Pos = positions[STICK_ID] ?: STICK_DEFAULT

    fun withAction(button: PadButton, action: PadAction) =
        copy(actions = actions + (button.id to action.id))

    fun withPosition(id: String, pos: Pos) =
        copy(positions = positions + (id to Pos(pos.x.coerceIn(0.04f, 0.96f), pos.y.coerceIn(0.08f, 0.94f))))

    fun resetLayout() = copy(actions = emptyMap(), positions = emptyMap())
}
