package dev.appxcode.ide.dependency

import java.nio.file.Path
import java.util.concurrent.TimeUnit

data class ResolveResult(val success: Boolean, val exitCode: Int?, val output: String, val timedOut: Boolean)

class DependencyResolver(private val runner: (List<String>, Path) -> Process = { args, root -> ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start() }) {
    fun resolve(manager: DependencyManager, root: Path, update: Boolean = false, timeoutMillis: Long = 600_000): ResolveResult =
        run(manager.command(update), root, timeoutMillis)
    fun resolveSwift(root: Path, timeoutMillis: Long = 600_000): ResolveResult = run(listOf("swift", "package", "resolve"), root, timeoutMillis)
    fun installPods(root: Path, timeoutMillis: Long = 600_000): ResolveResult = run(listOf("pod", "install"), root, timeoutMillis)

    private fun run(command: List<String>, root: Path, timeoutMillis: Long): ResolveResult {
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
        if (!java.nio.file.Files.isDirectory(root)) return ResolveResult(false, null, "Project root does not exist: $root", false)
        val process = runCatching { runner(command, root) }.getOrElse {
            return ResolveResult(false, null, "Unable to start ${command.first()}: ${it.message ?: "executable unavailable"}", false)
        }
        val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val timedOut = !finished
        val exitCode = if (timedOut) null else process.exitValue()
        return ResolveResult(!timedOut && exitCode == 0, exitCode, output, timedOut)
    }
}

private fun DependencyManager.command(update: Boolean): List<String> = when (this) {
    DependencyManager.SWIFT_PACKAGE_MANAGER -> listOf("swift", "package", "resolve")
    DependencyManager.COCOAPODS -> listOf("pod", if (update) "update" else "install")
}
