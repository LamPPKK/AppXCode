package dev.appxcode.ide.device

import java.nio.file.Files
import java.nio.file.Path

class SimctlDeviceOperations(
    private val xcrun: Path = Path.of("/usr/bin/xcrun"),
    private val runner: (List<String>) -> DeviceOperationResult = { args ->
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        DeviceOperationResult(process.waitFor() == 0, "simctl completed", output)
    },
    private val logProcess: ((List<String>) -> Process)? = null,
) : DeviceOperations {
    fun boot(deviceId: String): DeviceOperationResult = execute("boot", deviceId)
    fun shutdown(deviceId: String): DeviceOperationResult = execute("shutdown", deviceId)
    fun erase(deviceId: String): DeviceOperationResult = execute("erase", deviceId)

    override fun install(deviceId: String, app: Path): DeviceOperationResult =
        if (!Files.exists(app)) DeviceOperationResult(false, "App bundle not found: $app") else execute("install", deviceId, app.toString())

    override fun launch(deviceId: String, bundleId: String): DeviceOperationResult = execute("launch", deviceId, bundleId)

    override fun logs(deviceId: String, bundleId: String?): Sequence<String> =
        if (!Files.isExecutable(xcrun)) emptySequence() else sequence {
            val process = logProcess?.invoke(listOf(xcrun.toString(), "simctl", "spawn", deviceId, "log", "stream", "--style", "compact"))
                ?: ProcessBuilder(xcrun.toString(), "simctl", "spawn", deviceId, "log", "stream", "--style", "compact").redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (bundleId == null || line.contains(bundleId)) yield(line)
                }
            }
        }

    override fun screenshot(deviceId: String, destination: Path): DeviceOperationResult = execute("io", deviceId, "screenshot", destination.toString())

    private fun execute(vararg args: String): DeviceOperationResult {
        if (!Files.isExecutable(xcrun)) return DeviceOperationResult(false, "xcrun is unavailable")
        return runCatching { runner(listOf(xcrun.toString(), "simctl", *args)) }.getOrElse { DeviceOperationResult(false, it.message ?: "simctl failed") }
    }
}
