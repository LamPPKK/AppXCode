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
    fun create(sessionId: String) { sessions.putIfAbsent(sessionId, DebugSessionState.CREATED) }
    fun update(sessionId: String, state: DebugSessionState) { if (sessions.containsKey(sessionId)) sessions[sessionId] = state }
    fun state(sessionId: String): DebugSessionState? = sessions[sessionId]
    fun remove(sessionId: String) { sessions.remove(sessionId) }
}
