package dev.appxcode.ide.remote

import java.net.URI

data class BuildAgentEndpoint(val uri: URI, val certificateFingerprint: String, val token: String) {
    init {
        require(uri.scheme == "https" || uri.scheme == "http") { "unsupported agent URI scheme" }
        require(certificateFingerprint.isNotBlank()) { "certificate fingerprint is required" }
        require(token.isNotBlank()) { "agent token is required" }
    }
}

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

class BuildAgentConnection {
    @Volatile var state: ConnectionState = ConnectionState.DISCONNECTED
        private set
    fun connecting() { state = ConnectionState.CONNECTING }
    fun connected() { state = ConnectionState.CONNECTED }
    fun failed() { state = ConnectionState.FAILED }
    fun disconnect() { state = ConnectionState.DISCONNECTED }
}
