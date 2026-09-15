package dev.appxcode.ide.project

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class XcodeProjectWatcherTest {
    @Test
    fun recognizesEveryFileThatCanChangeTheProjectModel() {
        listOf(
            "App.xcodeproj",
            "App.xcworkspace",
            "App.xcscheme",
            "Debug.xcconfig",
            "project.pbxproj",
            "contents.xcworkspacedata",
            "Package.swift",
            "Package.resolved",
            "Podfile",
            "Podfile.lock",
        ).forEach { assertTrue(it, XcodeProjectWatcher.isProjectModelPath(Path.of(it))) }
        assertFalse(XcodeProjectWatcher.isProjectModelPath(Path.of("README.md")))
    }

    @Test
    fun observesWorkspaceMetadataAndProjectsCreatedAfterStartup() {
        val root = Files.createTempDirectory("appxcode-watcher")
        val workspace = root.resolve("App.xcworkspace")
        Files.createDirectories(workspace)
        val workspaceData = workspace.resolve("contents.xcworkspacedata")
        Files.writeString(workspaceData, "<Workspace/>")

        XcodeProjectWatcher(root).use { watcher ->
            val workspaceChanged = CountDownLatch(1)
            val projectCreated = CountDownLatch(1)
            watcher.onChange { path ->
                if (path == workspaceData) workspaceChanged.countDown()
                if (path.fileName?.toString() == "project.pbxproj") projectCreated.countDown()
            }

            Files.writeString(workspaceData, "<Workspace version=\"1.0\"/>")
            assertTrue("workspace metadata change was not observed", workspaceChanged.await(5, TimeUnit.SECONDS))

            val newProject = root.resolve("Generated/App.xcodeproj")
            Files.createDirectories(newProject)
            Files.writeString(newProject.resolve("project.pbxproj"), "// generated")
            assertTrue("new nested project change was not observed", projectCreated.await(5, TimeUnit.SECONDS))
        }
    }
}
