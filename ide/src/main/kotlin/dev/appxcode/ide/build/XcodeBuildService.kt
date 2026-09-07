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
)

data class XcodeBuildResult(
    val exitCode: Int?,
    val output: String,
    val timedOut: Boolean,
) {
    val succeeded: Boolean get() = exitCode == 0 && !timedOut
}

class XcodeBuildService(
    private val toolchain: AppleToolchain,
    private val processFactory: (List<String>, Path) -> Process = { command, directory ->
        ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start()
    },
) {
    fun execute(configuration: RunConfiguration, container: Path, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult =
        execute(XcodeBuildRequest(container, configuration.scheme, configuration.destination.xcodebuildSpecifier(), configuration.configuration, "build"), timeout)

    fun execute(request: XcodeBuildRequest, timeout: Duration = Duration.ofMinutes(15)): XcodeBuildResult {
        val executable = toolchain.xcodebuildPath ?: return XcodeBuildResult(null, "xcodebuild is unavailable", false)
        val containerFlag = if (request.container.fileName.toString().endsWith(".xcworkspace")) "-workspace" else "-project"
        val command = listOf(executable.toString(), "-scheme", request.scheme, "-destination", request.destination, "-configuration", request.configuration, request.action, containerFlag, request.container.toString()) + request.arguments
        val process = processFactory(command, request.container.parent)
        val outputBuffer = StringBuffer()
        val reader = Thread { process.inputStream.bufferedReader().use { outputBuffer.append(it.readText()) } }
        reader.start()
        val finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        reader.join(2_000)
        val timedOut = !finished
        return XcodeBuildResult(if (timedOut) null else process.exitValue(), outputBuffer.toString(), timedOut)
    }
}
