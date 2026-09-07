package dev.appxcode.ide.device

/** Optional isolated adapter for Lakr233/vphone-cli. Disabled unless explicitly enabled. */
class VPhoneProvider(
    private val executable: String,
    private val enabled: Boolean = false,
    private val runner: (List<String>) -> String = { args ->
        ProcessBuilder(args).redirectErrorStream(true).start().inputStream.bufferedReader().readText()
    },
) : DeviceProvider {
    override val id: String = "vphone-cli"

    override fun list(): List<AppleDevice> {
        if (!enabled) return emptyList()
        val output = runCatching { runner(listOf(executable, "list")) }.getOrNull() ?: return emptyList()
        return output.lineSequence().mapNotNull { line ->
            val fields = line.split('|').map(String::trim)
            if (fields.size < 2 || fields[0].isBlank()) return@mapNotNull null
            AppleDevice(fields[0], fields[1], "iOS", DeviceKind.VPHONE, DeviceState.UNKNOWN)
        }.distinctBy(AppleDevice::id)
    }
}
