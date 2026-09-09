package dev.appxcode.ide.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class DeviceRegistryTest {
    @Test
    fun `snapshot listeners retain provider capabilities`() {
        val device = AppleDevice("sim-1", "iPhone Test", "iOS", DeviceKind.SIMULATOR, DeviceState.AVAILABLE)
        val provider = FakeProvider(
            devices = listOf(device),
            capabilities = setOf(DeviceCapability.SCREENSHOT, DeviceCapability.LAUNCH_APP),
        )
        val registry = DeviceRegistry()
        registry.register(provider)

        var observed: DeviceRegistrySnapshot? = null
        val subscription = registry.onSnapshotChanged { observed = it }
        registry.refresh()

        val snapshot = requireNotNull(observed)
        assertTrue(snapshot.supports(device.id, DeviceCapability.SCREENSHOT))
        assertTrue(snapshot.supports(device.id, DeviceCapability.LAUNCH_APP))
        assertEquals(provider.capabilities, snapshot.capabilities(device.id))

        subscription.close()
        registry.close()
    }

    @Test
    fun `supported screenshot routes to owning provider`() {
        val device = AppleDevice("sim-2", "iPhone Route", "iOS", DeviceKind.SIMULATOR, DeviceState.AVAILABLE)
        val operations = RecordingOperations()
        val registry = DeviceRegistry()
        registry.register(
            FakeProvider(
                devices = listOf(device),
                capabilities = setOf(DeviceCapability.SCREENSHOT),
                operations = operations,
            ),
        )
        val destination = Path.of("build", "screenshots", "sim-2.png")

        val result = registry.screenshot(device.id, destination)

        assertTrue(result.success)
        assertEquals(device.id, operations.screenshotDeviceId)
        assertEquals(destination, operations.screenshotDestination)
        registry.close()
    }

    @Test
    fun `unsupported operation is rejected before provider invocation`() {
        val device = AppleDevice("physical-1", "iPhone Cable", "iOS", DeviceKind.PHYSICAL, DeviceState.AVAILABLE)
        val operations = RecordingOperations()
        val registry = DeviceRegistry()
        registry.register(FakeProvider(devices = listOf(device), capabilities = emptySet(), operations = operations))

        val result = registry.screenshot(device.id, Path.of("build", "blocked.png"))

        assertFalse(result.success)
        assertTrue(result.message.contains("screenshot is unavailable"))
        assertEquals(0, operations.screenshotCalls)
        registry.close()
    }

    private class FakeProvider(
        private val devices: List<AppleDevice>,
        val capabilities: Set<DeviceCapability>,
        private val operations: DeviceOperations = RecordingOperations(),
    ) : DeviceProvider {
        override val id: String = "fake-provider"
        override fun list(): List<AppleDevice> = devices
        override fun capabilities(device: AppleDevice): Set<DeviceCapability> = capabilities
        override fun operations(device: AppleDevice): DeviceOperations = operations
    }

    private class RecordingOperations : DeviceOperations {
        var screenshotCalls: Int = 0
        var screenshotDeviceId: String? = null
        var screenshotDestination: Path? = null

        override fun install(deviceId: String, app: Path): DeviceOperationResult =
            DeviceOperationResult(true, "install")

        override fun launch(deviceId: String, bundleId: String): DeviceOperationResult =
            DeviceOperationResult(true, "launch")

        override fun logs(deviceId: String, bundleId: String?): Sequence<String> = emptySequence()

        override fun screenshot(deviceId: String, destination: Path): DeviceOperationResult {
            screenshotCalls += 1
            screenshotDeviceId = deviceId
            screenshotDestination = destination
            return DeviceOperationResult(true, "screenshot")
        }
    }
}
