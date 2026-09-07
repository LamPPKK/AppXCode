package dev.appxcode.ide.remote

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

class BuildAgentConnection(val endpoint: BuildAgentEndpoint? = null) {
    @Volatile var state: ConnectionState = ConnectionState.DISCONNECTED
        private set
    val isConnected: Boolean get() = state == ConnectionState.CONNECTED
    val isConnecting: Boolean get() = state == ConnectionState.CONNECTING
    val hasFailed: Boolean get() = state == ConnectionState.FAILED
    fun connecting() { state = ConnectionState.CONNECTING }
    fun connected() { state = ConnectionState.CONNECTED }
    fun failed() { state = ConnectionState.FAILED }
    fun disconnect() { state = ConnectionState.DISCONNECTED }

    fun validate(policy: BuildAgentTransportPolicy = BuildAgentTransportPolicy.SECURE_DEFAULT): String? =
        endpoint?.let(policy::validationError) ?: "Build agent endpoint is not configured"

    fun canConnect(policy: BuildAgentTransportPolicy = BuildAgentTransportPolicy.SECURE_DEFAULT): Boolean =
        endpoint?.let(policy::permits) == true

    fun connect(policy: BuildAgentTransportPolicy = BuildAgentTransportPolicy.SECURE_DEFAULT): Boolean {
        if (!canConnect(policy)) { failed(); return false }
        connecting()
        return true
    }
}
