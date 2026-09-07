package dev.appxcode.ide.device

import java.nio.file.Files
import java.nio.file.Path

class DevicectlDeviceOperations(
    private val xcrun: Path = Path.of("/usr/bin/xcrun"),
    private val runner: (List<String>) -> DeviceOperationResult = { args ->
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        DeviceOperationResult(process.waitFor() == 0, "devicectl completed", output)
    },
) : DeviceOperations {
    override fun install(deviceId: String, app: Path): DeviceOperationResult =
        if (!Files.exists(app)) DeviceOperationResult(false, "App bundle not found: $app") else execute("device", "install", "app", "--device", deviceId, app.toString())

    override fun launch(deviceId: String, bundleId: String): DeviceOperationResult = execute("device", "process", "launch", "--device", deviceId, bundleId)

    override fun logs(deviceId: String, bundleId: String?): Sequence<String> = execute("device", "log", "collect", "--device", deviceId).output.lineSequence()

    override fun screenshot(deviceId: String, destination: Path): DeviceOperationResult = execute("device", "screenshot", "--device", deviceId, destination.toString())

    private fun execute(vararg args: String): DeviceOperationResult {
        if (!Files.isExecutable(xcrun)) return DeviceOperationResult(false, "xcrun is unavailable")
        return runCatching { runner(listOf(xcrun.toString(), "devicectl", *args)) }.getOrElse { DeviceOperationResult(false, it.message ?: "devicectl failed") }
    }
}
