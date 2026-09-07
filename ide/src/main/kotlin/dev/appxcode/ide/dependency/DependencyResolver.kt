package dev.appxcode.ide.dependency

import java.nio.file.Path
import java.util.concurrent.TimeUnit

data class ResolveResult(val success: Boolean, val exitCode: Int?, val output: String, val timedOut: Boolean)

class DependencyResolver(private val runner: (List<String>, Path) -> Process = { args, root -> ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start() }) {
    fun resolve(manager: DependencyManager, root: Path, update: Boolean = false, timeoutMillis: Long = 600_000, onOutput: (String) -> Unit = {}): ResolveResult =
        run(manager.command(update), root, timeoutMillis, onOutput)
    fun resolveSwift(root: Path, update: Boolean = false, timeoutMillis: Long = 600_000, onOutput: (String) -> Unit = {}): ResolveResult =
        run(listOf("swift", "package", if (update) "update" else "resolve"), root, timeoutMillis, onOutput)
    fun installPods(root: Path, update: Boolean = false, timeoutMillis: Long = 600_000, onOutput: (String) -> Unit = {}): ResolveResult =
        run(listOf("pod", if (update) "update" else "install"), root, timeoutMillis, onOutput)

    private fun run(command: List<String>, root: Path, timeoutMillis: Long, onOutput: (String) -> Unit): ResolveResult {
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
        if (!java.nio.file.Files.isDirectory(root)) return ResolveResult(false, null, "Project root does not exist: $root", false)
        val process = runCatching { runner(command, root) }.getOrElse {
            return ResolveResult(false, null, "Unable to start ${command.first()}: ${it.message ?: "executable unavailable"}", false)
        }
        val output = StringBuilder()
        val pump = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> output.appendLine(line); runCatching { onOutput(line) } }
            }
        }.apply { isDaemon = true; start() }
        val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        pump.join(1_000)
        val timedOut = !finished
        val exitCode = if (timedOut) null else process.exitValue()
        return ResolveResult(!timedOut && exitCode == 0, exitCode, output.toString(), timedOut)
    }
}

private fun DependencyManager.command(update: Boolean): List<String> = when (this) {
    DependencyManager.SWIFT_PACKAGE_MANAGER -> listOf("swift", "package", if (update) "update" else "resolve")
    DependencyManager.COCOAPODS -> listOf("pod", if (update) "update" else "install")
}
