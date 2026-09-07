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
    override fun install(deviceId: String, app: Path) = execute("install", deviceId, app.toString())
    override fun launch(deviceId: String, bundleId: String) = execute("launch", deviceId, bundleId)
    override fun logs(deviceId: String, bundleId: String?) = execute("logs", deviceId).output.lineSequence()
    override fun screenshot(deviceId: String, destination: Path) = execute("screenshot", deviceId, destination.toString())
    private fun execute(vararg args: String): DeviceOperationResult = runCatching { runner(listOf(executable, *args)) }.getOrElse { DeviceOperationResult(false, it.message ?: "vphone-cli failed") }
}
