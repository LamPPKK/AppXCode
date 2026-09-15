package dev.appxcode.ide.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class XcodeProjectModelDiscoveryTest {
    @Test
    fun discoversDeepContainersWithoutExposingInternalWorkspace() {
        val root = Files.createTempDirectory("appxcode-xcode-monorepo")
        val project = root.resolve("products/mobile/apple/clients/consumer/App.xcodeproj")
        Files.createDirectories(project.resolve("project.xcworkspace"))
        val workspace = root.resolve("products/mobile/apple/App.xcworkspace")
        Files.createDirectories(workspace)
        Files.createDirectories(root.resolve("Pods/Pods.xcodeproj"))
        Files.createDirectories(root.resolve("DerivedData/Generated.xcworkspace"))

        val containers = XcodeProjectModel.discover(root)
        assertEquals(2, containers.size)
        assertTrue(containers.any { it.path == project && it.kind == XcodeContainerKind.PROJECT })
        assertTrue(containers.any { it.path == workspace && it.kind == XcodeContainerKind.WORKSPACE })
        assertFalse(containers.any { it.path.fileName.toString() == "project.xcworkspace" })
    }

    @Test
    fun acceptsContainerAsDiscoveryRoot() {
        val project = Files.createTempDirectory("appxcode-root-container").resolve("Root.xcodeproj")
        Files.createDirectories(project)
        val result = XcodeProjectModel.discover(project)
        assertEquals(project, result.single().path)
    }
}
