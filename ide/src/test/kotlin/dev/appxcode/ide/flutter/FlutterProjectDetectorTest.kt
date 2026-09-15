package dev.appxcode.ide.flutter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files

class FlutterProjectDetectorTest {
    @Test
    fun discoversAppPluginPackageAndModuleInMonorepo() {
        val root = Files.createTempDirectory("appxcode-flutter-monorepo")
        project(root.resolve("apps/mobile"), "mobile") { Files.createDirectories(it.resolve("ios")) }
        project(root.resolve("packages/core"), "core", "dependencies:\n  flutter:\n    sdk: flutter\n")
        project(root.resolve("packages/camera_plugin"), "camera_plugin", "flutter:\n  plugin:\n    platforms:\n      ios:\n")
        project(root.resolve("modules/payments"), "payments") { Files.writeString(it.resolve(".metadata"), "project_type: module\n") }
        project(root.resolve("build/generated"), "ignored")

        val projects = FlutterProjectDetector.discover(root)
        assertEquals(4, projects.size)
        assertEquals(FlutterProjectKind.APPLICATION, projects.first { it.name == "mobile" }.kind)
        assertEquals(FlutterProjectKind.PACKAGE, projects.first { it.name == "core" }.kind)
        assertEquals(FlutterProjectKind.PLUGIN, projects.first { it.name == "camera_plugin" }.kind)
        assertEquals(FlutterProjectKind.MODULE, projects.first { it.name == "payments" }.kind)
        assertFalse(projects.any { it.name == "ignored" })
    }

    @Test
    fun excludesPureDartPackageAndDoesNotTreatDependencyPluginAsFlutterPlugin() {
        val root = Files.createTempDirectory("appxcode-mixed-dart")
        project(root.resolve("pure_dart"), "pure_dart", "dependencies:\n  http: ^1.0.0\n")
        project(root.resolve("flutter_app"), "flutter_app", "dependencies:\n  flutter:\n    sdk: flutter\n  plugin: ^1.0.0\n")
        val projects = FlutterProjectDetector.discover(root)
        assertEquals(1, projects.size)
        assertEquals("flutter_app", projects.single().name)
        assertEquals(FlutterProjectKind.PACKAGE, projects.single().kind)
    }

    private fun project(root: java.nio.file.Path, name: String, extra: String = "", setup: (java.nio.file.Path) -> Unit = {}) {
        Files.createDirectories(root)
        Files.writeString(root.resolve("pubspec.yaml"), "name: $name\n$extra")
        setup(root)
    }
}
