package com.herotoner.herotv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herotoner.herotv.HeroViewModel
import com.herotoner.herotv.ui.screens.HomeScreen
import com.herotoner.herotv.ui.screens.PlaceholderScreen
import com.herotoner.herotv.ui.screens.RemoteScreen
import com.herotoner.herotv.ui.theme.HeroColors

enum class Screen { Home, Pairing, Remote, Gamepad, Apps, Settings }

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Screen.Home, "الرئيسية", Icons.Filled.Home),
    Tab(Screen.Gamepad, "الألعاب", Icons.Filled.SportsEsports),
    Tab(Screen.Apps, "التطبيقات", Icons.Filled.Apps),
    Tab(Screen.Settings, "الإعدادات", Icons.Filled.Settings)
)

@Composable
fun HeroApp(viewModel: HeroViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val showBottomBar = tabs.any { it.screen == screen }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    Scaffold(
        containerColor = HeroColors.Black,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = HeroColors.Surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = screen == tab.screen,
                            onClick = { screen = tab.screen },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HeroColors.BlueBright,
                                selectedTextColor = HeroColors.BlueBright,
                                indicatorColor = HeroColors.SurfaceHigh,
                                unselectedIconColor = HeroColors.TextSecondary,
                                unselectedTextColor = HeroColors.TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.padding(padding),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "screen"
        ) { target ->
            when (target) {
                Screen.Home -> HomeScreen(
                    connection = connection,
                    onNavigate = { screen = it }
                )
                Screen.Remote -> RemoteScreen(
                    connection = connection,
                    statusMessage = viewModel.statusMessage,
                    onBack = { screen = Screen.Home },
                    onCommand = viewModel::send,
                    onStatusShown = viewModel::clearStatus
                )
                Screen.Pairing -> PlaceholderScreen(
                    title = "اقتران التلفزيون",
                    phase = "المرحلة الثالثة",
                    description = "البحث عن التلفزيونات، الاقتران عبر Wireless Debugging، وحفظ الأجهزة.",
                    icon = Icons.Filled.Link,
                    onBack = { screen = Screen.Home }
                )
                Screen.Gamepad -> PlaceholderScreen(
                    title = "يد الألعاب",
                    phase = "المرحلة الرابعة",
                    description = "واجهة Gamepad بوضع أفقي وأزرار قابلة للتخصيص.",
                    icon = Icons.Filled.SportsEsports,
                    onBack = null
                )
                Screen.Apps -> PlaceholderScreen(
                    title = "التطبيقات والألعاب",
                    phase = "المرحلة الخامسة",
                    description = "عرض التطبيقات والألعاب المثبتة على التلفزيون وفتحها.",
                    icon = Icons.Filled.Apps,
                    onBack = null
                )
                Screen.Settings -> PlaceholderScreen(
                    title = "الإعدادات",
                    phase = "المرحلة السادسة",
                    description = "الأجهزة المقترنة، الخدمة بالخلفية، الاهتزاز، وسجل الأوامر.",
                    icon = Icons.Filled.Settings,
                    onBack = null
                )
            }
        }
    }
}
