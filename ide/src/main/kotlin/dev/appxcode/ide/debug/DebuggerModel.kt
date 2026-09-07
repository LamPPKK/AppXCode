package dev.appxcode.ide.debug

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

data class Breakpoint(val file: Path, val line: Int, val enabled: Boolean = true)
data class DebugVariable(val name: String, val value: String, val type: String? = null)
enum class DebugSessionState { CREATED, RUNNING, PAUSED, TERMINATED }

interface DebuggerAdapter {
    fun launch(executable: Path, arguments: List<String> = emptyList()): String
    fun pause(sessionId: String)
    fun resume(sessionId: String)
    fun terminate(sessionId: String)
    fun stack(sessionId: String): List<String>
    fun variables(sessionId: String): List<DebugVariable>
}

class DebugSessionRegistry {
    private val sessions = ConcurrentHashMap<String, DebugSessionState>()
    fun create(sessionId: String) { require(sessionId.isNotBlank()) { "Debug session id must not be blank" }; sessions.putIfAbsent(sessionId, DebugSessionState.CREATED) }
    fun update(sessionId: String, state: DebugSessionState): Boolean = sessions.replace(sessionId, state) != null
    fun state(sessionId: String): DebugSessionState? = sessions[sessionId]
    fun all(): Map<String, DebugSessionState> = sessions.toMap()
    fun clear() { sessions.clear() }
    fun remove(sessionId: String): Boolean = sessions.remove(sessionId) != null
}
