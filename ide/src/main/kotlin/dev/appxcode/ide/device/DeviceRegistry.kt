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
    private val listeners = CopyOnWriteArrayList<(List<AppleDevice>) -> Unit>()
    @Volatile private var providerErrors: Map<String, String> = emptyMap()

    fun register(provider: DeviceProvider) {
        require(provider.id.isNotBlank()) { "Device provider id must not be blank" }
        val existing = providers.indexOfFirst { it.id == provider.id }
        if (existing >= 0 && providers[existing] === provider) return
        if (existing < 0) providers += provider else providers[existing] = provider
        notifyListeners()
    }
    fun unregister(providerId: String) { if (providers.removeIf { it.id == providerId }) { providerErrors = providerErrors - providerId; notifyListeners() } }
    fun clear() { if (providers.isNotEmpty()) { providers.clear(); providerErrors = emptyMap(); notifyListeners() } }
    fun refresh() { notifyListeners() }
    fun refresh(providerId: String): Boolean {
        require(providerId.isNotBlank()) { "Device provider id must not be blank" }
        if (providers.none { it.id == providerId }) return false
        notifyListeners()
        return true
    }
    fun onDevicesChanged(listener: (List<AppleDevice>) -> Unit): AutoCloseable {
        listeners += listener
        runCatching { listener(discover()) }
        return AutoCloseable { listeners.remove(listener) }
    }
    fun discover(): List<AppleDevice> {
        val errors = linkedMapOf<String, String>()
        val devices = providers.flatMap { provider -> runCatching { provider.list() }.getOrElse { error -> errors[provider.id] = error.message ?: error.javaClass.simpleName; emptyList() } }
        providerErrors = errors.toMap()
        return devices.distinctBy(AppleDevice::id).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppleDevice::platform, AppleDevice::name))
    }
    fun providerErrors(): Map<String, String> = providerErrors
    fun snapshot(): DeviceRegistrySnapshot = DeviceRegistrySnapshot(discover(), providerErrors())
    fun providerIds(): List<String> = providers.map(DeviceProvider::id).sorted()
    fun hasProvider(providerId: String): Boolean = providers.any { it.id == providerId }
    private fun notifyListeners() { val devices = discover(); listeners.forEach { runCatching { it(devices) } } }
}

data class DeviceRegistrySnapshot(val devices: List<AppleDevice>, val providerErrors: Map<String, String>) {
    val totalCount: Int get() = devices.size
    val availableCount: Int get() = devices.count { it.state == DeviceState.AVAILABLE }
    val hasAvailable: Boolean get() = availableCount > 0
    val availableDevices: List<AppleDevice> get() = devices.filter { it.state == DeviceState.AVAILABLE }
    val offlineCount: Int get() = devices.count { it.state == DeviceState.OFFLINE }
    val hasOffline: Boolean get() = offlineCount > 0
    val hasUnknown: Boolean get() = devices.any { it.state == DeviceState.UNKNOWN }
    val unknownCount: Int get() = devices.count { it.state == DeviceState.UNKNOWN }
    val vphoneCount: Int get() = devices.count { it.kind == DeviceKind.VPHONE }
    val physicalCount: Int get() = devices.count { it.kind == DeviceKind.PHYSICAL }
    val simulatorCount: Int get() = devices.count { it.kind == DeviceKind.SIMULATOR }
    val countsByKind: Map<DeviceKind, Int> get() = devices.groupingBy(AppleDevice::kind).eachCount()
    val availableByKind: Map<DeviceKind, Int> get() = devices.filter { it.state == DeviceState.AVAILABLE }.groupingBy(AppleDevice::kind).eachCount()
    val physicalAvailableCount: Int get() = availableByKind[DeviceKind.PHYSICAL] ?: 0
    val availablePhysicalDevices: List<AppleDevice> get() = availableDevices.filter { it.kind == DeviceKind.PHYSICAL }
    val physicalDevices: List<AppleDevice> get() = devices.filter { it.kind == DeviceKind.PHYSICAL }
    val availableSimulators: List<AppleDevice> get() = availableDevices.filter { it.kind == DeviceKind.SIMULATOR }
    val simulatorDevices: List<AppleDevice> get() = devices.filter { it.kind == DeviceKind.SIMULATOR }
    val availableVPhones: List<AppleDevice> get() = availableDevices.filter { it.kind == DeviceKind.VPHONE }
    val vphoneDevices: List<AppleDevice> get() = devices.filter { it.kind == DeviceKind.VPHONE }
    val availableDevicesByKind: Map<DeviceKind, List<AppleDevice>> get() = availableDevices.groupBy(AppleDevice::kind)
    val offlineDevices: List<AppleDevice> get() = devices.filter { it.state == DeviceState.OFFLINE }
    val unknownDevices: List<AppleDevice> get() = devices.filter { it.state == DeviceState.UNKNOWN }
    val offlineDevicesByKind: Map<DeviceKind, List<AppleDevice>> get() = offlineDevices.groupBy(AppleDevice::kind)
    val simulatorAvailableCount: Int get() = availableByKind[DeviceKind.SIMULATOR] ?: 0
    val physicalOfflineCount: Int get() = devices.count { it.kind == DeviceKind.PHYSICAL && it.state == DeviceState.OFFLINE }
    val physicalUnknownCount: Int get() = devices.count { it.kind == DeviceKind.PHYSICAL && it.state == DeviceState.UNKNOWN }
    val hasProviderErrors: Boolean get() = providerErrors.isNotEmpty()
    val errorCount: Int get() = providerErrors.size
}
