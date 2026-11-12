package com.herotoner.herotv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herotoner.herotv.AppsUi
import com.herotoner.herotv.HeroViewModel
import com.herotoner.herotv.adb.TvApp
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.pad.PadProfile
import com.herotoner.herotv.ui.components.ButtonKind
import com.herotoner.herotv.ui.components.HeroButton
import com.herotoner.herotv.ui.components.ScreenHeader
import com.herotoner.herotv.ui.theme.HeroColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    connection: ConnectionState,
    apps: AppsUi,
    games: Set<String>,
    profiles: List<PadProfile>,
    profileForGame: (String) -> String?,
    onLoad: () -> Unit,
    onOpen: (TvApp) -> Unit,
    onOpenGame: (TvApp) -> Unit,
    onToggleGame: (String, Boolean) -> Unit,
    onBindProfile: (String, String) -> Unit,
    onOpenPairing: () -> Unit,
    onBack: () -> Unit
) {
    var gamesTab by rememberSaveable { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<TvApp?>(null) }
    val connected = connection is ConnectionState.Connected

    // نحمّل القائمة تلقائياً عند الاتصال (أو عند فتح الصفحة وهو متصل)
    LaunchedEffect(connected) {
        if (connected && (apps is AppsUi.Idle || apps is AppsUi.Failed)) onLoad()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("التطبيقات والألعاب", connection, onBack = null, onChipClick = onOpenPairing)

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroButton("التطبيقات", { gamesTab = false }, Modifier.weight(1f), if (!gamesTab) ButtonKind.Primary else ButtonKind.Secondary)
            HeroButton("الألعاب", { gamesTab = true }, Modifier.weight(1f), if (gamesTab) ButtonKind.Primary else ButtonKind.Secondary)
            IconButton(onClick = onLoad, enabled = connected && apps !is AppsUi.Loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "تحديث القائمة", tint = HeroColors.TextPrimary)
            }
        }
        Spacer(Modifier.height(10.dp))

        when {
            !connected -> Message(
                text = "اتصل بالتلفزيون أولاً لعرض التطبيقات المثبتة عليه.",
                actionText = "اقتران التلفزيون",
                onAction = onOpenPairing
            )

            apps is AppsUi.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HeroColors.BlueBright)
            }

            apps is AppsUi.Failed -> Message(text = apps.message, actionText = "إعادة المحاولة", onAction = onLoad)

            apps is AppsUi.Loaded -> {
                val visible = if (gamesTab) apps.apps.filter { it.packageName in games } else apps.apps
                if (visible.isEmpty()) {
                    Message(
                        text = if (gamesTab) {
                            "لا توجد ألعاب معلَّمة بعد. ADB لا يستطيع معرفة الألعاب تلقائياً: اضغط مطوّلاً على أي تطبيق " +
                                "في تبويب «التطبيقات» ثم اختر «تعليم كلعبة»."
                        } else {
                            "لم يُعثر على تطبيقات قابلة للتشغيل."
                        },
                        actionText = null,
                        onAction = {}
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visible, key = { it.packageName }) { app ->
                            AppCard(
                                app = app,
                                isGame = app.packageName in games,
                                onClick = { if (app.packageName in games) onOpenGame(app) else onOpen(app) },
                                onLongClick = { optionsFor = app }
                            )
                        }
                    }
                }
            }

            else -> Box(Modifier.fillMaxSize())
        }
    }

    optionsFor?.let { app ->
        val isGame = app.packageName in games
        val boundId = profileForGame(app.packageName)
        AlertDialog(
            onDismissRequest = { optionsFor = null },
            confirmButton = { TextButton(onClick = { optionsFor = null }) { Text("إغلاق") } },
            title = { Text(HeroViewModel.displayName(app)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptionRow("فتح التطبيق") {
                        onOpen(app)
                        optionsFor = null
                    }
                    if (isGame) {
                        OptionRow("فتح مع يد الألعاب") {
                            onOpenGame(app)
                            optionsFor = null
                        }
                    }
                    OptionRow(if (isGame) "إزالة علامة اللعبة" else "تعليم كلعبة") {
                        onToggleGame(app.packageName, !isGame)
                    }
                    if (isGame) {
                        Text("ملف التحكم لهذه اللعبة:", color = HeroColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        profiles.forEach { p ->
                            OptionRow((if (p.id == boundId) "✓ " else "") + p.name) {
                                onBindProfile(app.packageName, p.id)
                                optionsFor = null
                            }
                        }
                    }
                }
            },
            containerColor = HeroColors.Surface,
            titleContentColor = HeroColors.TextPrimary
        )
    }
}

@Composable
private fun OptionRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = HeroColors.TextPrimary,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(app: TvApp, isGame: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val name = HeroViewModel.displayName(app)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(HeroColors.Surface)
            .border(1.dp, if (isGame) HeroColors.Purple else HeroColors.Outline, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ADB لا يعيد أيقونات التطبيقات؛ نعرض الحرف الأول من اسمه
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(HeroColors.BlueBright, HeroColors.BlueDeep))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            name,
            color = HeroColors.TextPrimary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (isGame) Text("لعبة", color = HeroColors.Purple, fontSize = 11.sp)
    }
}

@Composable
private fun Message(text: String, actionText: String?, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text, color = HeroColors.TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
        if (actionText != null) {
            Spacer(Modifier.height(14.dp))
            HeroButton(actionText, onAction)
        }
    }
}
