package dev.appxcode.ide.device

import java.util.concurrent.CopyOnWriteArrayList

enum class DeviceKind { SIMULATOR, PHYSICAL, VPHONE }
enum class DeviceState { AVAILABLE, BOOTING, OFFLINE, UNKNOWN }

data class AppleDevice(val id: String, val name: String, val platform: String, val kind: DeviceKind, val state: DeviceState)

interface DeviceProvider {
    val id: String
    fun list(): List<AppleDevice>
}

class DeviceRegistry {
    private val providers = CopyOnWriteArrayList<DeviceProvider>()

    fun register(provider: DeviceProvider) { if (providers.none { it.id == provider.id }) providers += provider }
    fun unregister(providerId: String) { providers.removeIf { it.id == providerId } }
    fun discover(): List<AppleDevice> = providers.flatMap { runCatching { it.list() }.getOrDefault(emptyList()) }
        .distinctBy(AppleDevice::id)
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppleDevice::platform, AppleDevice::name))
}
