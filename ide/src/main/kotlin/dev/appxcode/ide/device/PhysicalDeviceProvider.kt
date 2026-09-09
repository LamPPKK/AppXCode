package dev.appxcode.ide.device

import java.nio.file.Files
import java.nio.file.Path

class PhysicalDeviceProvider(
    private val xcrun: Path = Path.of("/usr/bin/xcrun"),
    private val runner: (List<String>) -> String = { args -> ProcessBuilder(args).redirectErrorStream(true).start().inputStream.bufferedReader().readText() },
    private val deviceOperations: DeviceOperations = DevicectlDeviceOperations(xcrun),
) : DeviceProvider {
    override val id: String = "apple-devicectl"

    override fun list(): List<AppleDevice> {
        if (!Files.isExecutable(xcrun)) return emptyList()
        val output = runCatching { runner(listOf(xcrun.toString(), "devicectl", "list", "devices")) }.getOrNull() ?: return emptyList()
        return output.lineSequence().mapNotNull { line ->
            val match = DEVICE_LINE.find(line) ?: return@mapNotNull null
            AppleDevice(match.groupValues[2], match.groupValues[1], match.groupValues[3], DeviceKind.PHYSICAL, if (match.groupValues[4].contains("available", true)) DeviceState.AVAILABLE else DeviceState.UNKNOWN)
        }.distinctBy(AppleDevice::id).sortedBy { it.name.lowercase() }.toList()
    }

    override fun capabilities(device: AppleDevice): Set<DeviceCapability> =
        if (device.state != DeviceState.AVAILABLE) emptySet() else setOf(
            DeviceCapability.INSTALL_APP,
            DeviceCapability.LAUNCH_APP,
            DeviceCapability.LOGS,
        )

    override fun operations(device: AppleDevice): DeviceOperations = deviceOperations

    private companion object { val DEVICE_LINE = Regex("^\\s*(.+?)\\s+([0-9A-Fa-f-]{20,})\\s+(iOS|iPadOS|macOS|watchOS|tvOS)\\s+(.+)$") }
}
