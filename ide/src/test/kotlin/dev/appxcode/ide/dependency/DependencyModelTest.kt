package dev.appxcode.ide.dependency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class DependencyModelTest {
    @Test
    fun readsXcodeWorkspaceResolvedFileRegardlessOfStateFieldOrder() {
        val root = Files.createTempDirectory("appxcode-deps")
        val swiftpm = root.resolve("App.xcworkspace/xcshareddata/swiftpm")
        Files.createDirectories(swiftpm)
        Files.writeString(swiftpm.resolve("Package.resolved"), """
            { "version": 2, "pins": [
              { "identity": "swift-collections", "state": { "revision": "abc123", "version": "1.1.0" } },
              { "identity": "local-package", "state": { "branch": "main", "revision": "def456" } }
            ] }
        """.trimIndent())

        val pins = DependencyModel.read(root).filter { it.manager == DependencyManager.SWIFT_PACKAGE_MANAGER }
        assertEquals(2, pins.size)
        assertEquals("1.1.0", pins.first { it.name == "swift-collections" }.version)
        assertEquals("abc123", pins.first { it.name == "swift-collections" }.revision)
        assertNull(pins.first { it.name == "local-package" }.version)
    }

    @Test
    fun readsLegacyPackageResolvedSchema() {
        val root = Files.createTempDirectory("appxcode-legacy-deps")
        Files.writeString(root.resolve("Package.resolved"), """
            { "object": { "pins": [
              { "package": "Nimble", "state": { "version": "12.0.0", "revision": "123abc" } }
            ] }, "version": 1 }
        """.trimIndent())
        val pin = DependencyModel.read(root).single()
        assertEquals("Nimble", pin.name)
        assertEquals("12.0.0", pin.version)
        assertEquals("123abc", pin.revision)
    }
}
