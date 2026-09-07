package dev.appxcode.ide.language

import java.nio.file.Path

data class LspServerConfig(val executable: Path, val workspace: Path, val arguments: List<String> = emptyList())
enum class LspState { STOPPED, STARTING, RUNNING, FAILED }

class LspProcessManager(private val config: LspServerConfig) : AutoCloseable {
    @Volatile var state: LspState = LspState.STOPPED
        private set
    private var process: Process? = null

    fun start(): Boolean {
        if (state == LspState.RUNNING) return true
        state = LspState.STARTING
        return runCatching {
            process = ProcessBuilder(listOf(config.executable.toString()) + config.arguments).directory(config.workspace.toFile()).start()
            state = LspState.RUNNING
            true
        }.getOrElse { state = LspState.FAILED; false }
    }

    fun isAlive(): Boolean = process?.isAlive == true

    fun restart(): Boolean { close(); return start() }

    override fun close() {
        process?.destroy()
        process = null
        state = LspState.STOPPED
    }
}
