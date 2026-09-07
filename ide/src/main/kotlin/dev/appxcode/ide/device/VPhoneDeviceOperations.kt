package dev.appxcode.ide.device

import java.nio.file.Path

class VPhoneDeviceOperations(
    private val executable: String = "vphone-cli",
    private val runner: (List<String>) -> DeviceOperationResult = { args ->
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        DeviceOperationResult(process.waitFor() == 0, "vphone-cli completed", output)
    },
) : DeviceOperations {
    override fun install(deviceId: String, app: Path) =
        when {
            deviceId.isBlank() -> DeviceOperationResult(false, "Device id is required")
            !java.nio.file.Files.isRegularFile(app) && !java.nio.file.Files.isDirectory(app) -> DeviceOperationResult(false, "App bundle not found: $app")
            else -> execute("install", deviceId, app.toString())
        }
    override fun launch(deviceId: String, bundleId: String) = if (deviceId.isBlank() || bundleId.isBlank()) DeviceOperationResult(false, "Device id and bundle id are required") else execute("launch", deviceId, bundleId)
    override fun logs(deviceId: String, bundleId: String?): Sequence<String> {
        val result = if (deviceId.isBlank()) DeviceOperationResult(false, "Device id is required") else execute("logs", deviceId)
        return result.output.lineSequence().filter { bundleId == null || it.contains(bundleId) }
    }
    override fun screenshot(deviceId: String, destination: Path) = if (deviceId.isBlank() || destination.toString().isBlank()) DeviceOperationResult(false, "Device id and destination are required") else execute("screenshot", deviceId, destination.toString())
    private fun execute(vararg args: String): DeviceOperationResult = runCatching { runner(listOf(executable, *args)) }.getOrElse { DeviceOperationResult(false, "vphone-cli failed: ${it.message ?: "unknown error"}") }
}
