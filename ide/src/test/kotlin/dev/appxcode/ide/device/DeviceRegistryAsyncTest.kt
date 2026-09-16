package dev.appxcode.ide.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DeviceRegistryAsyncTest {
    @Test
    fun refreshAsyncPublishesSnapshot() {
        val registry = DeviceRegistry()
        registry.register(object : DeviceProvider {
            override val id = "test-provider"
            override fun list() = listOf(AppleDevice("device-1", "Test Phone", "iOS", DeviceKind.SIMULATOR, DeviceState.AVAILABLE))
        })
        val latch = CountDownLatch(1)
        var count = 0
        registry.refreshAsync {
            count = it.totalCount
            latch.countDown()
        }
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, count)
        registry.close()
    }
}
