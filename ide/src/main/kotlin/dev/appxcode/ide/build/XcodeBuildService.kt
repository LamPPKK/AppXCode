package dev.appxcode.ide.build

import dev.appxcode.ide.toolchain.AppleToolchain
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

data class XcodeBuildRequest(
    val container: Path,
    val scheme: String,
    val destination: String,
    val configuration: String = "Debug",
    val action: String = "build",
    val arguments: List<String> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
)

data class XcodeBuildResult(
    val exitCode: Int?,
    val output: String,
    val timedOut: Boolean,
) {
    val succeeded: Boolean get() = exitCode == 0 && !timedOut
    val diagnostics: List<BuildDiagnostic> get() = XcodeDiagnosticParser.parse(output)
}

class XcodeBuildService(
    private val toolchain: AppleToolchain,
    private val processFactory: (List<String>, Path) -> Process = { command, directory ->
        ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start()
    },
    private val processFactoryWithEnvironment: ((List<String>, Path, Map<String, String>) -> Process)? = null,
) {
    fun execute(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult =
        execute(XcodeBuildRequest(container, configuration.scheme, configuration.destination.xcodebuildSpecifier(), configuration.configuration, "build", configuration.arguments, configuration.environment), timeout)

    fun execute(request: XcodeBuildRequest, timeout: Duration = Duration.ofMinutes(15), cancellation: BuildCancellation? = null): XcodeBuildResult {
        require(!timeout.isNegative && !timeout.isZero) { "timeout must be positive" }
        val executable = toolchain.xcodebuildPath?.takeIf { java.nio.file.Files.isExecutable(it) }
            ?: return XcodeBuildResult(null, "xcodebuild is unavailable", false)
        if (!java.nio.file.Files.exists(request.container)) return XcodeBuildResult(null, "Xcode container not found: ${request.container}", false)
        val containerFlag = if (request.container.fileName.toString().endsWith(".xcworkspace")) "-workspace" else "-project"
        val command = listOf(executable.toString(), "-scheme", request.scheme, "-destination", request.destination, "-configuration", request.configuration, request.action, containerFlag, request.container.toString()) + request.arguments
        val process = processFactoryWithEnvironment?.let { it(command, request.container.parent, request.environment) }
            ?: if (request.environment.isEmpty()) processFactory(command, request.container.parent)
            else ProcessBuilder(command).directory(request.container.parent.toFile()).apply {
                environment().putAll(request.environment)
                redirectErrorStream(true)
            }.start()
        val outputBuffer = StringBuffer()
        val reader = Thread { process.inputStream.bufferedReader().use { outputBuffer.append(it.readText()) } }
        reader.isDaemon = true
        reader.start()
        val deadline = System.nanoTime() + timeout.toNanos()
        var finished = false
        while (!finished && System.nanoTime() < deadline && cancellation?.isCancelled() != true) finished = process.waitFor(250, TimeUnit.MILLISECONDS)
        if (!finished || cancellation?.isCancelled() == true) process.destroyForcibly()
        reader.join(2_000)
        val timedOut = !finished && cancellation?.isCancelled() != true
        return XcodeBuildResult(if (timedOut) null else process.exitValue(), outputBuffer.toString(), timedOut)
    }
}
