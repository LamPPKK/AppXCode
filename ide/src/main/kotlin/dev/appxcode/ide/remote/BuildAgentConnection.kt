package dev.appxcode.ide.remote

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

class BuildAgentConnection(val endpoint: BuildAgentEndpoint? = null) : AutoCloseable {
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(ConnectionState) -> Unit>()
    @Volatile var state: ConnectionState = ConnectionState.DISCONNECTED
        private set
    val isConnected: Boolean get() = state == ConnectionState.CONNECTED
    val isConnecting: Boolean get() = state == ConnectionState.CONNECTING
    val hasFailed: Boolean get() = state == ConnectionState.FAILED
    fun onStateChanged(listener: (ConnectionState) -> Unit): AutoCloseable {
        listeners += listener
        runCatching { listener(state) }
        return AutoCloseable { listeners.remove(listener) }
    }
    fun connecting() = transition(ConnectionState.CONNECTING)
    fun connected() = transition(ConnectionState.CONNECTED)
    fun failed() = transition(ConnectionState.FAILED)
    fun disconnect() = transition(ConnectionState.DISCONNECTED)

    fun validate(policy: BuildAgentTransportPolicy = BuildAgentTransportPolicy.SECURE_DEFAULT): String? =
        endpoint?.let(policy::validationError) ?: "Build agent endpoint is not configured"

    fun canConnect(policy: BuildAgentTransportPolicy = BuildAgentTransportPolicy.SECURE_DEFAULT): Boolean =
        endpoint?.let(policy::permits) == true

    fun connect(policy: BuildAgentTransportPolicy = BuildAgentTransportPolicy.SECURE_DEFAULT): Boolean {
        if (!canConnect(policy)) { failed(); return false }
        connecting()
        return true
    }

    private fun transition(next: ConnectionState) {
        val previous = state
        state = next
        if (previous != next) listeners.forEach { listener -> runCatching { listener(next) } }
    }

    override fun close() {
        disconnect()
        listeners.clear()
    }
}
