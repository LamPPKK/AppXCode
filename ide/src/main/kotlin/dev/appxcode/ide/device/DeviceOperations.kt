package dev.appxcode.ide.device

import java.nio.file.Path

data class DeviceOperationResult(val success: Boolean, val message: String, val output: String = "") {
    val status: String get() = if (success) "succeeded" else "failed"
}

interface DeviceOperations {
    fun install(deviceId: String, app: Path): DeviceOperationResult
    fun launch(deviceId: String, bundleId: String): DeviceOperationResult
    fun logs(deviceId: String, bundleId: String? = null): Sequence<String>
    fun screenshot(deviceId: String, destination: Path): DeviceOperationResult
}

class UnsupportedDeviceOperations : DeviceOperations {
    override fun install(deviceId: String, app: Path) = unavailable()
    override fun launch(deviceId: String, bundleId: String) = unavailable()
    override fun logs(deviceId: String, bundleId: String?) = emptySequence()
    override fun screenshot(deviceId: String, destination: Path) = unavailable()
    private fun unavailable() = DeviceOperationResult(false, "Device operations are not configured")
}
