package com.herotoner.herotv.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Reconnecting(val attempt: Int) : ConnectionState
    data class Connected(val deviceName: String, val address: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}
