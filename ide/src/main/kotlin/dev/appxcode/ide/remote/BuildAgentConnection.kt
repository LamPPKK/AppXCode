package dev.appxcode.ide.remote

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

class BuildAgentConnection(val endpoint: BuildAgentEndpoint? = null) {
    @Volatile var state: ConnectionState = ConnectionState.DISCONNECTED
        private set
    fun connecting() { state = ConnectionState.CONNECTING }
    fun connected() { state = ConnectionState.CONNECTED }
    fun failed() { state = ConnectionState.FAILED }
    fun disconnect() { state = ConnectionState.DISCONNECTED }
}
