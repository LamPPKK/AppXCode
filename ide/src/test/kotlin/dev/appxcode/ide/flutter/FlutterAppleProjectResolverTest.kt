package dev.appxcode.ide.flutter

import dev.appxcode.ide.project.XcodeContainerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class FlutterAppleProjectResolverTest {
    @Test
    fun prefersRunnerWorkspaceAndUsesEvidenceBackedScheme() {
        val root = Files.createTempDirectory("appxcode-flutter-apple")
        Files.createDirectories(root.resolve("ios/Runner.xcodeproj"))
        val workspace = root.resolve("ios/Runner.xcworkspace")
        val schemes = workspace.resolve("xcshareddata/xcschemes")
        Files.createDirectories(schemes)
        Files.writeString(schemes.resolve("Runner.xcscheme"), "<Scheme/>")
        val project = FlutterProject(root, "sample", true, false, emptyList())

        val resolved = FlutterAppleProjectResolver.resolve(project).single()
        assertEquals(FlutterApplePlatform.IOS, resolved.platform)
        assertEquals(XcodeContainerKind.WORKSPACE, resolved.container.kind)
        assertEquals("Runner", resolved.scheme)
    }

    @Test
    fun resolvesGeneratedModuleRootWithoutGuessingAmbiguousScheme() {
        val root = Files.createTempDirectory("appxcode-flutter-module")
        Files.createDirectories(root.resolve("ios"))
        val projectDir = root.resolve(".ios/Runner.xcodeproj")
        val schemes = projectDir.resolve("xcshareddata/xcschemes")
        Files.createDirectories(schemes)
        Files.writeString(schemes.resolve("Debug.xcscheme"), "<Scheme/>")
        Files.writeString(schemes.resolve("Release.xcscheme"), "<Scheme/>")
        val project = FlutterProject(root, "module", false, false, emptyList(), kind = FlutterProjectKind.MODULE)

        val resolved = FlutterAppleProjectResolver.resolve(project).single()
        assertEquals(XcodeContainerKind.PROJECT, resolved.container.kind)
        assertNull(resolved.scheme)
    }

    @Test
    fun ignoresNestedVendorContainersAndResolvesMacosHost() {
        val root = Files.createTempDirectory("appxcode-flutter-macos")
        Files.createDirectories(root.resolve("macos/Pods/Vendor/Runner.xcworkspace"))
        Files.createDirectories(root.resolve("macos/Runner.xcodeproj"))
        val project = FlutterProject(root, "desktop", false, true, emptyList())
        val resolved = FlutterAppleProjectResolver.resolve(project).single()
        assertEquals(FlutterApplePlatform.MACOS, resolved.platform)
        assertEquals("Runner.xcodeproj", resolved.container.path.fileName.toString())
    }
}
