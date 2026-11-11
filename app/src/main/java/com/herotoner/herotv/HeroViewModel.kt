package com.herotoner.herotv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.herotoner.herotv.model.ConnectionState
import com.herotoner.herotv.model.TvCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * المرحلة الثانية: الهيكل فقط. حالة الاتصال دائماً "غير متصل" ولا يوجد منفّذ أوامر بعد.
 * المرحلة الثالثة ستضيف الاقتران عبر ADB وإرسال الأوامر الفعلي هنا.
 */
class HeroViewModel : ViewModel() {

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    /** رسالة قصيرة تظهر في صفحة الريموت. */
    var statusMessage by mutableStateOf<String?>(null)
        private set

    fun send(command: TvCommand) {
        statusMessage = if (_connection.value is ConnectionState.Connected) {
            "منفّذ الأوامر يُضاف في المرحلة الثالثة (${command.labelAr})"
        } else {
            "غير متصل. اقترن بالتلفزيون أولاً من صفحة اقتران التلفزيون."
        }
    }

    fun clearStatus() {
        statusMessage = null
    }
}
