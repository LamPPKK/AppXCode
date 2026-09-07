package dev.appxcode.ide.debug

import java.nio.file.Path
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LldbDebuggerAdapter(private val command: (List<String>) -> String = { args ->
    ProcessBuilder(args).redirectErrorStream(true).start().inputStream.bufferedReader().readText()
}) : DebuggerAdapter {
    private val processes = ConcurrentHashMap<String, Process>()
    private val breakpoints = ConcurrentHashMap<String, MutableSet<Breakpoint>>()

    override fun launch(executable: Path, arguments: List<String>): String {
        require(Files.isRegularFile(executable) && Files.isExecutable(executable)) { "Debug executable is not runnable: $executable" }
        val process = ProcessBuilder(listOf(executable.toString()) + arguments).redirectErrorStream(true).start()
        return UUID.randomUUID().toString().also { processes[it] = process }
    }
    fun isAlive(sessionId: String): Boolean = processes[sessionId]?.isAlive == true
    fun setBreakpoint(sessionId: String, breakpoint: Breakpoint): Boolean {
        require(breakpoint.line > 0) { "Breakpoint line must be positive" }
        if (!processes.containsKey(sessionId)) return false
        breakpoints.computeIfAbsent(sessionId) { ConcurrentHashMap.newKeySet() }.add(breakpoint)
        return true
    }
    fun clearBreakpoint(sessionId: String, breakpoint: Breakpoint) { breakpoints[sessionId]?.remove(breakpoint) }
    fun listBreakpoints(sessionId: String): List<Breakpoint> = breakpoints[sessionId]?.toList()?.sortedWith(compareBy({ it.file.toString() }, { it.line })) ?: emptyList()
    fun clearBreakpoints(sessionId: String) { breakpoints.remove(sessionId) }
    override fun pause(sessionId: String) { processes[sessionId]?.let { command(listOf("kill", "-STOP", it.pid().toString())) } }
    override fun resume(sessionId: String) { processes[sessionId]?.let { command(listOf("kill", "-CONT", it.pid().toString())) } }
    override fun terminate(sessionId: String) {
        processes.remove(sessionId)?.let { process ->
            process.destroy()
            if (process.isAlive) process.destroyForcibly()
        }
        breakpoints.remove(sessionId)
    }
    override fun stack(sessionId: String): List<String> = processes[sessionId]?.let { command(listOf("lldb", "-p", it.pid().toString(), "-o", "bt", "-o", "detach", "-o", "quit")).lineSequence().toList() } ?: emptyList()
    override fun variables(sessionId: String): List<DebugVariable> = processes[sessionId]?.let {
        command(listOf("lldb", "-p", it.pid().toString(), "-o", "frame variable", "-o", "detach", "-o", "quit")).lineSequence().mapNotNull { line ->
            val match = Regex("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.*)$").find(line) ?: return@mapNotNull null
            DebugVariable(match.groupValues[1], match.groupValues[2])
        }.toList()
    } ?: emptyList()
}
