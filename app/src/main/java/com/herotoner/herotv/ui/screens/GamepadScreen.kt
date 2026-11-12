package com.herotoner.herotv.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.pad.PadAction
import com.herotoner.herotv.pad.PadButton
import com.herotoner.herotv.pad.PadProfile
import com.herotoner.herotv.pad.Pos
import com.herotoner.herotv.pad.STICK_ID
import com.herotoner.herotv.pad.StickMode
import com.herotoner.herotv.ui.components.ButtonKind
import com.herotoner.herotv.ui.components.HeroButton
import com.herotoner.herotv.ui.components.HeroKey
import com.herotoner.herotv.ui.components.HeroTextField
import com.herotoner.herotv.ui.components.connectionColor
import com.herotoner.herotv.ui.components.connectionLabel
import com.herotoner.herotv.ui.theme.HeroColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * يد الألعاب. تُرسل أزرار Gamepad إلى التلفزيون عبر ADB (`input gamepad keyevent`).
 * العصا: وضع رقمي (اتجاهات D-pad متكررة) أو تناظري تجريبي (`input joystick motionevent`).
 * ADB لا يخلق جهاز Gamepad حقيقياً؛ الألعاب التي تتطلب Gamepad فعلياً أو عصا تناظرية دقيقة قد لا تستجيب.
 */
@Composable
fun GamepadScreen(
    connection: ConnectionState,
    profiles: List<PadProfile>,
    activeId: String,
    keepAwake: Boolean,
    startEditing: Boolean,
    onPress: (PadAction, Boolean) -> Unit,
    onStick: (Float, Float) -> Unit,
    onSave: (PadProfile) -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onExit: () -> Unit
) {
    val profile = profiles.firstOrNull { it.id == activeId } ?: profiles.first()
    var editing by rememberSaveable { mutableStateOf(startEditing) }
    var pickerFor by remember { mutableStateOf<PadButton?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val stickVector = remember { mutableStateOf(Offset.Zero) }

    val currentPress by rememberUpdatedState(onPress)
    val currentStick by rememberUpdatedState(onStick)

    ImmersiveLandscape(keepAwake = keepAwake, onLeave = { currentStick(0f, 0f) })

    BackHandler { if (editing) editing = false else onExit() }

    // ───── العصا: تحويل الاتجاه إلى ضغطات D-pad متكررة (الوضع الرقمي) ─────
    val vector = stickVector.value
    val direction: PadAction? = when {
        profile.stickMode != StickMode.DIGITAL -> null
        editing -> null
        vector.getDistance() < 0.5f -> null
        abs(vector.x) > abs(vector.y) -> if (vector.x > 0) PadAction.DPAD_RIGHT else PadAction.DPAD_LEFT
        else -> if (vector.y > 0) PadAction.DPAD_DOWN else PadAction.DPAD_UP
    }
    LaunchedEffect(direction, profile.holdRepeat) {
        if (direction != null) {
            currentPress(direction, false)
            if (profile.holdRepeat) {
                delay(350)
                while (true) {
                    currentPress(direction, true)
                    delay(130)
                }
            }
        }
    }

    // ───── العصا التناظرية التجريبية ─────
    val analogEngaged = profile.stickMode == StickMode.ANALOG && !editing && vector != Offset.Zero
    var wasEngaged by remember { mutableStateOf(false) }
    LaunchedEffect(analogEngaged) {
        if (analogEngaged) {
            wasEngaged = true
            while (true) {
                val v = stickVector.value
                currentStick(v.x, v.y)
                delay(70)
            }
        } else if (wasEngaged) {
            wasEngaged = false
            currentStick(0f, 0f)
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeroColors.Black, Color(0xFF07122A), HeroColors.Black)))
    ) {
        val areaW = constraints.maxWidth.toFloat()
        val areaH = constraints.maxHeight.toFloat()

        // العصا
        Slot(
            id = STICK_ID,
            pos = profile.stickPosition(),
            width = STICK_SIZE,
            height = STICK_SIZE,
            areaW = areaW,
            areaH = areaH,
            editing = editing,
            caption = if (profile.stickMode == StickMode.DIGITAL) "عصا رقمية" else "عصا تناظرية",
            onMoved = { onSave(profile.withPosition(STICK_ID, it)) },
            onEditTap = {}
        ) {
            Joystick(diameter = STICK_SIZE, enabled = !editing, onVector = { stickVector.value = it })
        }

        // الأزرار
        PadButton.entries.forEach { button ->
            val action = profile.actionFor(button)
            val (w, h) = sizeOf(button)
            key(button.id) {
                Slot(
                    id = button.id,
                    pos = profile.positionFor(button),
                    width = w,
                    height = h,
                    areaW = areaW,
                    areaH = areaH,
                    editing = editing,
                    caption = action.labelAr,
                    onMoved = { onSave(profile.withPosition(button.id, it)) },
                    onEditTap = { pickerFor = button }
                ) {
                    HeroKey(
                        onPress = { rep -> currentPress(action, rep) },
                        modifier = Modifier.fillMaxSize(),
                        shape = if (isRound(button)) CircleShape else RoundedCornerShape(12.dp),
                        borderColor = faceColor(button),
                        repeatOnHold = profile.holdRepeat,
                        enabled = !editing
                    ) {
                        Text(
                            button.label,
                            color = Color.White,
                            fontSize = if (isRound(button)) 18.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // الشريط العلوي: إيقاف فوري + حالة + ملف التحكم + تخصيص
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroButton("إيقاف يد التحكم", onExit, kind = ButtonKind.Danger)

                val statusColor = connectionColor(connection)
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(HeroColors.SurfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(statusColor))
                    Text(
                        if (connection is ConnectionState.Connected) "وضع الألعاب نشط • ${connection.deviceName}" else connectionLabel(connection),
                        color = statusColor,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                Box {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HeroColors.SurfaceHigh)
                            .clickable { menuOpen = true }
                            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الملف: ${profile.name}", color = HeroColors.TextPrimary, fontSize = 13.sp, maxLines = 1)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = HeroColors.TextPrimary)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        profiles.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(if (p.id == profile.id) "✓ ${p.name}" else p.name) },
                                onClick = {
                                    onSelect(p.id)
                                    menuOpen = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("＋ ملف جديد…") },
                            onClick = {
                                menuOpen = false
                                showCreate = true
                            }
                        )
                    }
                }

                IconButton(onClick = { editing = !editing }) {
                    Icon(
                        if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = if (editing) "إنهاء التخصيص" else "تخصيص الأزرار",
                        tint = if (editing) HeroColors.Amber else HeroColors.TextPrimary
                    )
                }
            }

            if (editing) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xCC0A1020))
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("اسحب الأزرار لتحريكها • اضغط زراً لتغيير وظيفته", color = HeroColors.TextSecondary, fontSize = 12.sp)
                    StickMode.entries.forEach { mode ->
                        FilterChip(
                            selected = profile.stickMode == mode,
                            onClick = { onSave(profile.copy(stickMode = mode)) },
                            label = { Text(mode.labelAr, fontSize = 12.sp) }
                        )
                    }
                    FilterChip(
                        selected = profile.holdRepeat,
                        onClick = { onSave(profile.copy(holdRepeat = !profile.holdRepeat)) },
                        label = { Text("تكرار عند الضغط المستمر", fontSize = 12.sp) }
                    )
                    HeroButton("إعادة التخطيط الافتراضي", { onSave(profile.resetLayout()) }, kind = ButtonKind.Secondary)
                    if (profiles.size > 1) {
                        HeroButton("حذف الملف", { onDelete(profile.id) }, kind = ButtonKind.Danger)
                    }
                }
            }
        }
    }

    pickerFor?.let { button ->
        ActionPickerDialog(
            title = "وظيفة الزر ${button.label}",
            current = profile.actionFor(button),
            onPick = {
                onSave(profile.withAction(button, it))
                pickerFor = null
            },
            onDismiss = { pickerFor = null }
        )
    }

    if (showCreate) {
        CreateProfileDialog(
            onCreate = {
                onCreate(it)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }
}

private val STICK_SIZE = 150.dp

private fun sizeOf(button: PadButton): Pair<Dp, Dp> = when (button) {
    PadButton.L1, PadButton.L2, PadButton.R1, PadButton.R2 -> 72.dp to 40.dp
    PadButton.DPAD_UP, PadButton.DPAD_DOWN, PadButton.DPAD_LEFT, PadButton.DPAD_RIGHT -> 50.dp to 50.dp
    PadButton.Y, PadButton.A, PadButton.X, PadButton.B -> 56.dp to 56.dp
    PadButton.SELECT, PadButton.START -> 78.dp to 34.dp
    PadButton.PAUSE -> 44.dp to 44.dp
}

private fun isRound(button: PadButton): Boolean = when (button) {
    PadButton.L1, PadButton.L2, PadButton.R1, PadButton.R2, PadButton.SELECT, PadButton.START -> false
    else -> true
}

private fun faceColor(button: PadButton): Color? = when (button) {
    PadButton.A -> HeroColors.Green
    PadButton.B -> HeroColors.Red
    PadButton.X -> HeroColors.Blue
    PadButton.Y -> HeroColors.Amber
    else -> null
}

/** مكان عنصر على اللوحة بإحداثيات نسبية (0..1). في وضع التخصيص يمكن سحبه ويُحفظ مكانه في الملف. */
@Composable
private fun Slot(
    id: String,
    pos: Pos,
    width: Dp,
    height: Dp,
    areaW: Float,
    areaH: Float,
    editing: Boolean,
    caption: String?,
    onMoved: (Pos) -> Unit,
    onEditTap: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val wPx = with(density) { width.toPx() }
    val hPx = with(density) { height.toPx() }
    var live by remember(id, pos) { mutableStateOf(pos) }
    val currentMoved by rememberUpdatedState(onMoved)
    val currentTap by rememberUpdatedState(onEditTap)

    Box(
        Modifier
            .offset { IntOffset((live.x * areaW - wPx / 2f).roundToInt(), (live.y * areaH - hPx / 2f).roundToInt()) }
            .size(width, height)
    ) {
        content()
        if (editing) {
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0x55FFB300))
                    .border(1.5.dp, HeroColors.Amber, shape)
                    .pointerInput(areaW, areaH) {
                        detectDragGestures(
                            onDrag = { change, drag ->
                                change.consume()
                                live = Pos(
                                    (live.x + drag.x / areaW).coerceIn(0.04f, 0.96f),
                                    (live.y + drag.y / areaH).coerceIn(0.08f, 0.94f)
                                )
                            },
                            onDragEnd = { currentMoved(live) },
                            onDragCancel = { currentMoved(live) }
                        )
                    }
                    .pointerInput(Unit) { detectTapGestures(onTap = { currentTap() }) },
                contentAlignment = Alignment.BottomCenter
            ) {
                if (caption != null) {
                    Text(caption, color = Color.White, fontSize = 9.sp, maxLines = 1, modifier = Modifier.padding(bottom = 1.dp))
                }
            }
        }
    }
}

@Composable
private fun Joystick(diameter: Dp, enabled: Boolean, onVector: (Offset) -> Unit) {
    val density = LocalDensity.current
    val travelPx = with(density) { (diameter * 0.29f).toPx() }
    val knobSize = diameter * 0.42f
    var knob by remember { mutableStateOf(Offset.Zero) }
    val currentOnVector by rememberUpdatedState(onVector)

    Box(
        Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(HeroColors.SurfaceHigh, HeroColors.Surface)))
            .border(
                2.dp,
                Brush.sweepGradient(listOf(HeroColors.Blue, HeroColors.Cyan, HeroColors.BlueDeep, HeroColors.Blue)),
                CircleShape
            )
            .pointerInput(enabled, travelPx) {
                if (!enabled) return@pointerInput
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                fun update(point: Offset) {
                    var v = (point - center) / travelPx
                    val length = v.getDistance()
                    if (length > 1f) v = v / length
                    knob = v
                    currentOnVector(v)
                }
                detectDragGestures(
                    onDragStart = { update(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        update(change.position)
                    },
                    onDragEnd = {
                        knob = Offset.Zero
                        currentOnVector(Offset.Zero)
                    },
                    onDragCancel = {
                        knob = Offset.Zero
                        currentOnVector(Offset.Zero)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .offset { IntOffset((knob.x * travelPx).roundToInt(), (knob.y * travelPx).roundToInt()) }
                .size(knobSize)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(HeroColors.BlueBright, HeroColors.BlueDeep)))
                .border(2.dp, HeroColors.Cyan, CircleShape)
        )
    }
}

@Composable
private fun ActionPickerDialog(
    title: String,
    current: PadAction,
    onPick: (PadAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(PadAction.entries.toList()) { action ->
                    Text(
                        text = (if (action == current) "✓ " else "") + action.labelAr,
                        color = if (action == current) HeroColors.BlueBright else HeroColors.TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(action) }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        },
        containerColor = HeroColors.Surface,
        titleContentColor = HeroColors.TextPrimary,
        textContentColor = HeroColors.TextPrimary
    )
}

@Composable
private fun CreateProfileDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("إنشاء") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("ملف تحكم جديد") },
        text = {
            HeroTextField(name, { name = it.take(30) }, "اسم الملف (مثلاً لعبة سباق)", Modifier.fillMaxWidth(), ltr = false)
        },
        containerColor = HeroColors.Surface,
        titleContentColor = HeroColors.TextPrimary
    )
}

/** يقفل الاتجاه أفقياً ويخفي أشرطة النظام أثناء صفحة اللعب، ويعيد كل شيء عند الخروج. */
@Composable
private fun ImmersiveLandscape(keepAwake: Boolean, onLeave: () -> Unit) {
    val context = LocalContext.current
    val currentLeave by rememberUpdatedState(onLeave)
    DisposableEffect(context, keepAwake) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        if (keepAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            currentLeave()
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
