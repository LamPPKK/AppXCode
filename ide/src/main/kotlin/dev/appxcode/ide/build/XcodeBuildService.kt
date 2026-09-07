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
) {
    init {
        require(container.fileName != null) { "Build container must point to a project or workspace" }
        require(container.fileName.toString().endsWith(".xcodeproj") || container.fileName.toString().endsWith(".xcworkspace")) {
            "Build container must be an .xcodeproj or .xcworkspace"
        }
        require(scheme.isNotBlank()) { "Build scheme must not be blank" }
        require(destination.isNotBlank()) { "Build destination must not be blank" }
        require(configuration.isNotBlank()) { "Build configuration must not be blank" }
        require(action.isNotBlank()) { "Build action must not be blank" }
    }
}

data class XcodeBuildResult(
    val exitCode: Int?,
    val output: String,
    val timedOut: Boolean,
    val cancelled: Boolean = false,
) {
    val succeeded: Boolean get() = exitCode == 0 && !timedOut && !cancelled
    val failed: Boolean get() = !succeeded && !timedOut && !cancelled
    val status: String get() = when { succeeded -> "succeeded"; cancelled -> "cancelled"; timedOut -> "timed_out"; else -> "failed" }
    val diagnostics: List<BuildDiagnostic> by lazy(LazyThreadSafetyMode.PUBLICATION) { XcodeDiagnosticParser.parse(output) }
    val errors: List<BuildDiagnostic> get() = diagnostics.filter { it.severity == DiagnosticSeverity.ERROR }
    val warnings: List<BuildDiagnostic> get() = diagnostics.filter { it.severity == DiagnosticSeverity.WARNING }
    val notes: List<BuildDiagnostic> get() = diagnostics.filter { it.severity == DiagnosticSeverity.NOTE }
    val hasErrors: Boolean get() = errors.isNotEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
    val hasNotes: Boolean get() = notes.isNotEmpty()
}

class XcodeBuildService(
    private val toolchain: AppleToolchain,
    private val processFactory: (List<String>, Path) -> Process = { command, directory ->
        ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start()
    },
    private val processFactoryWithEnvironment: ((List<String>, Path, Map<String, String>) -> Process)? = null,
) {
    fun execute(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult =
        execute(configuration, container, "build", timeout)

    fun run(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult =
        execute(configuration, container, "run", timeout)

    fun test(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult =
        execute(configuration, container, "test", timeout)

    fun clean(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult =
        execute(configuration, container, "clean", timeout)

    private fun execute(configuration: RunConfiguration, container: Path, action: String, timeout: Duration): XcodeBuildResult =
        execute(XcodeBuildRequest(container, configuration.scheme, configuration.destination.xcodebuildSpecifier(), configuration.configuration, action, configuration.arguments, configuration.environment), timeout)

    fun execute(request: XcodeBuildRequest, timeout: Duration = Duration.ofMinutes(15), cancellation: BuildCancellation? = null, onOutput: (String) -> Unit = {}): XcodeBuildResult {
        require(!timeout.isNegative && !timeout.isZero) { "timeout must be positive" }
        val executable = toolchain.xcodebuildPath?.takeIf { java.nio.file.Files.isExecutable(it) }
            ?: return XcodeBuildResult(null, "xcodebuild is unavailable", false)
        if (!java.nio.file.Files.exists(request.container)) return XcodeBuildResult(null, "Xcode container not found: ${request.container}", false)
        val workingDirectory = request.container.parent
            ?: return XcodeBuildResult(null, "Xcode container has no working directory", false)
        if (!java.nio.file.Files.isDirectory(workingDirectory)) return XcodeBuildResult(null, "Working directory not found: $workingDirectory", false)
        val containerFlag = if (request.container.fileName.toString().endsWith(".xcworkspace")) "-workspace" else "-project"
        val command = listOf(
            executable.toString(),
            containerFlag, request.container.toString(),
            "-scheme", request.scheme,
            "-destination", request.destination,
            "-configuration", request.configuration,
            request.action,
        ) + request.arguments
        val process = runCatching {
            processFactoryWithEnvironment?.let { it(command, workingDirectory, request.environment) }
            ?: if (request.environment.isEmpty()) processFactory(command, workingDirectory)
            else ProcessBuilder(command).directory(workingDirectory.toFile()).apply {
                environment().putAll(request.environment)
                redirectErrorStream(true)
            }.start()
        }.getOrElse { return XcodeBuildResult(null, "Unable to start xcodebuild: ${it.message ?: "unknown error"}", false) }
        val outputBuffer = StringBuffer()
        val reader = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> outputBuffer.appendLine(line); runCatching { onOutput(line) } }
            }
        }
        reader.isDaemon = true
        reader.start()
        val timeoutNanos = runCatching { timeout.toNanos() }.getOrElse { Long.MAX_VALUE }
        val start = System.nanoTime()
        val deadline = if (timeoutNanos >= Long.MAX_VALUE - start) Long.MAX_VALUE else start + timeoutNanos
        var finished = false
        while (!finished && System.nanoTime() < deadline && cancellation?.isCancelled() != true) finished = process.waitFor(250, TimeUnit.MILLISECONDS)
        val cancelled = cancellation?.isCancelled() == true
        if (!finished || cancelled) {
            process.destroyForcibly()
            runCatching { process.waitFor(2, TimeUnit.SECONDS) }
        }
        reader.join(2_000)
        val timedOut = !finished && !cancelled
        return XcodeBuildResult(
            if (timedOut || cancelled || !process.isAlive) process.exitValueOrNull() else process.exitValue(),
            outputBuffer.toString(),
            timedOut,
            cancelled,
        )
    }

    private fun Process.exitValueOrNull(): Int? = runCatching { exitValue() }.getOrNull()
}
