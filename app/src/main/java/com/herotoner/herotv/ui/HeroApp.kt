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
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herotoner.herotv.HeroViewModel
import com.herotoner.herotv.ui.components.LocalClickSoundEnabled
import com.herotoner.herotv.ui.components.LocalHapticsEnabled
import com.herotoner.herotv.ui.screens.AppsScreen
import com.herotoner.herotv.ui.screens.GamepadScreen
import com.herotoner.herotv.ui.screens.HomeScreen
import com.herotoner.herotv.ui.screens.PairingScreen
import com.herotoner.herotv.ui.screens.RemoteScreen
import com.herotoner.herotv.ui.screens.SettingsScreen
import com.herotoner.herotv.ui.theme.HeroColors

enum class Screen { Home, Pairing, Remote, Gamepad, Apps, Settings }

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Screen.Home, "الرئيسية", Icons.Filled.Home),
    Tab(Screen.Remote, "الريموت", Icons.Filled.SettingsRemote),
    Tab(Screen.Gamepad, "الألعاب", Icons.Filled.SportsEsports),
    Tab(Screen.Apps, "التطبيقات", Icons.Filled.Apps),
    Tab(Screen.Settings, "الإعدادات", Icons.Filled.Settings)
)

@Composable
fun HeroApp(vm: HeroViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var gamepadEditing by rememberSaveable { mutableStateOf(false) }

    val connection by vm.connection.collectAsStateWithLifecycle()
    val savedDevices by vm.savedDevices.collectAsStateWithLifecycle()
    val discovered by vm.discovered.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val activeProfileId by vm.activeProfileId.collectAsStateWithLifecycle()
    val games by vm.gamePackages.collectAsStateWithLifecycle()
    val haptics by vm.haptics.collectAsStateWithLifecycle()
    val clickSound by vm.clickSound.collectAsStateWithLifecycle()
    val keepAwake by vm.keepAwake.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.statusMessage) {
        val message = vm.statusMessage
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.clearStatus()
        }
    }

    val showBottomBar = screen != Screen.Gamepad && tabs.any { it.screen == screen }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    CompositionLocalProvider(
        LocalHapticsEnabled provides haptics,
        LocalClickSoundEnabled provides clickSound
    ) {
        Scaffold(
            containerColor = HeroColors.Black,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = HeroColors.Surface) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = screen == tab.screen,
                                onClick = {
                                    gamepadEditing = false
                                    screen = tab.screen
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
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
                        devices = savedDevices,
                        onNavigate = { screen = it },
                        onConnect = vm::connectSaved,
                        onDisconnect = vm::disconnect,
                        onRetry = vm::retry
                    )

                    Screen.Pairing -> PairingScreen(
                        connection = connection,
                        discovered = discovered,
                        saved = savedDevices,
                        busy = vm.busy,
                        message = vm.pairingMessage,
                        onBack = { screen = Screen.Home },
                        onStartDiscovery = vm::startDiscovery,
                        onStopDiscovery = vm::stopDiscovery,
                        onPair = vm::pair,
                        onConnectDirect = vm::connectDirect,
                        onConnectDiscovered = vm::connectDiscovered,
                        onConnectSaved = vm::connectSaved,
                        onForget = vm::forget,
                        onDisconnect = vm::disconnect,
                        onRetry = vm::retry,
                        onError = vm::reportPairingError,
                        onConnected = { screen = Screen.Remote }
                    )

                    Screen.Remote -> RemoteScreen(
                        connection = connection,
                        onBack = { screen = Screen.Home },
                        onOpenPairing = { screen = Screen.Pairing },
                        onCommand = vm::send,
                        onText = vm::sendText
                    )

                    Screen.Gamepad -> GamepadScreen(
                        connection = connection,
                        profiles = profiles,
                        activeId = activeProfileId,
                        keepAwake = keepAwake,
                        startEditing = gamepadEditing,
                        onPress = vm::padPress,
                        onStick = vm::padStick,
                        onSave = vm::saveProfile,
                        onSelect = vm::selectProfile,
                        onCreate = vm::createProfile,
                        onDelete = vm::deleteProfile,
                        onExit = {
                            gamepadEditing = false
                            screen = Screen.Home
                        }
                    )

                    Screen.Apps -> AppsScreen(
                        connection = connection,
                        apps = vm.apps,
                        games = games,
                        profiles = profiles,
                        profileForGame = vm::profileForGame,
                        onLoad = vm::loadApps,
                        onOpen = vm::openApp,
                        onOpenGame = {
                            vm.openGame(it)
                            gamepadEditing = false
                            screen = Screen.Gamepad
                        },
                        onToggleGame = vm::setGame,
                        onBindProfile = vm::bindGame,
                        onOpenPairing = { screen = Screen.Pairing },
                        onBack = { screen = Screen.Home }
                    )

                    Screen.Settings -> SettingsScreen(
                        connection = connection,
                        devices = savedDevices,
                        profiles = profiles,
                        activeProfileId = activeProfileId,
                        haptics = haptics,
                        clickSound = clickSound,
                        keepAwake = keepAwake,
                        deviceInfo = vm.deviceInfo,
                        deviceInfoLoading = vm.deviceInfoLoading,
                        commandLog = vm.commandLog,
                        onBack = { screen = Screen.Home },
                        onOpenPairing = { screen = Screen.Pairing },
                        onConnect = vm::connectSaved,
                        onForget = vm::forget,
                        onDisconnect = vm::disconnect,
                        onRetry = vm::retry,
                        onSetHaptics = vm::setHaptics,
                        onSetClickSound = vm::setClickSound,
                        onSetKeepAwake = vm::setKeepAwake,
                        onCommand = vm::send,
                        onSelectProfile = vm::selectProfile,
                        onDeleteProfile = vm::deleteProfile,
                        onEditPad = {
                            gamepadEditing = true
                            screen = Screen.Gamepad
                        },
                        onLoadDeviceInfo = vm::loadDeviceInfo,
                        onClearLog = vm::clearLog
                    )
                }
            }
        }
    }
}
