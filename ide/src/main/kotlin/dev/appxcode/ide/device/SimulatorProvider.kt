package dev.appxcode.ide.device

import java.nio.file.Files
import java.nio.file.Path

class SimulatorProvider(
    private val xcrun: Path = Path.of("/usr/bin/xcrun"),
    private val runner: (List<String>) -> String = { args -> ProcessBuilder(args).redirectErrorStream(true).start().inputStream.bufferedReader().readText() },
    private val deviceOperations: DeviceOperations = SimctlDeviceOperations(xcrun),
) : DeviceProvider {
    override val id: String = "apple-simctl"

    override fun list(): List<AppleDevice> {
        if (!Files.isExecutable(xcrun)) return emptyList()
        val output = runCatching { runner(listOf(xcrun.toString(), "simctl", "list", "devices")) }.getOrNull() ?: return emptyList()
        return output.lineSequence().mapNotNull { line ->
            val match = DEVICE_LINE.find(line) ?: return@mapNotNull null
            val state = when (match.groupValues[3].uppercase()) { "BOOTED" -> DeviceState.AVAILABLE; "SHUTDOWN" -> DeviceState.OFFLINE; else -> DeviceState.UNKNOWN }
            AppleDevice(match.groupValues[2], match.groupValues[1], "iOS Simulator", DeviceKind.SIMULATOR, state)
        }.distinctBy(AppleDevice::id).sortedBy { it.name.lowercase() }.toList()
    }

    override fun capabilities(device: AppleDevice): Set<DeviceCapability> =
        if (device.state != DeviceState.AVAILABLE) emptySet() else setOf(
            DeviceCapability.INSTALL_APP,
            DeviceCapability.LAUNCH_APP,
            DeviceCapability.LOGS,
            DeviceCapability.SCREENSHOT,
        )

    override fun operations(device: AppleDevice): DeviceOperations = deviceOperations

    private companion object { val DEVICE_LINE = Regex("^\\s+(.+?) \\(([0-9A-Fa-f-]{20,})\\) \\(([^)]+)\\)") }
}
