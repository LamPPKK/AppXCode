package dev.appxcode.ide.dependency

import java.nio.file.Path
import java.util.concurrent.TimeUnit

data class ResolveResult(val success: Boolean, val exitCode: Int?, val output: String, val timedOut: Boolean)

class DependencyResolver(private val runner: (List<String>, Path) -> Process = { args, root -> ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start() }) {
    fun resolveSwift(root: Path, timeoutMillis: Long = 600_000): ResolveResult = run(listOf("swift", "package", "resolve"), root, timeoutMillis)
    fun installPods(root: Path, timeoutMillis: Long = 600_000): ResolveResult = run(listOf("pod", "install"), root, timeoutMillis)

    private fun run(command: List<String>, root: Path, timeoutMillis: Long): ResolveResult {
        val process = runCatching { runner(command, root) }.getOrNull() ?: return ResolveResult(false, null, "Unable to start ${command.first()}", false)
        val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val timedOut = !finished
        val exitCode = if (timedOut) null else process.exitValue()
        return ResolveResult(!timedOut && exitCode == 0, exitCode, output, timedOut)
    }
}
