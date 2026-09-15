package dev.appxcode.ide.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ProjectSnapshotLoaderTest {
    @Test
    fun keepsTargetsAndConfigurationsForEveryProjectContainer() {
        val root = Files.createTempDirectory("appxcode-snapshot-monorepo")
        createProject(root.resolve("apps/First.xcodeproj"), "AAAAAAAAAAAAAAAAAAAAAAAA", "First")
        createProject(root.resolve("packages/Second.xcodeproj"), "BBBBBBBBBBBBBBBBBBBBBBBB", "Second")
        val workspace = root.resolve("All.xcworkspace")
        Files.createDirectories(workspace)
        Files.writeString(workspace.resolve("contents.xcworkspacedata"), """
            <Workspace version="1.0">
              <Group location="group:apps" name="Applications">
                <FileRef location="group:First.xcodeproj"/>
              </Group>
              <FileRef location="group:packages/Second.xcodeproj"/>
            </Workspace>
        """.trimIndent())

        val snapshot = ProjectSnapshotLoader.load(root)
        assertEquals(3, snapshot.containerModels.size)
        assertEquals(listOf("First", "Second"), snapshot.targets.map { it.name })
        assertEquals(2, snapshot.buildConfigurations.size)
        val workspaceModel = snapshot.containerModels.single { it.container.path == workspace }
        assertEquals(listOf("First", "Second"), workspaceModel.targets.map { it.name })
        assertEquals(2, workspaceModel.buildConfigurations.size)
        assertTrue(snapshot.containerModels.filter { it.container.kind == XcodeContainerKind.PROJECT }
            .all { it.targets.size == 1 && it.buildConfigurations.size == 1 })
    }

    private fun createProject(project: Path, id: String, name: String) {
        val configurationId = if (name == "First") "CCCCCCCCCCCCCCCCCCCCCCCC" else "DDDDDDDDDDDDDDDDDDDDDDDD"
        Files.createDirectories(project)
        Files.writeString(project.resolve("project.pbxproj"), """
            {
              objects = {
                $id = {
                  isa = PBXNativeTarget;
                  name = $name;
                  productName = $name;
                  productType = "com.apple.product-type.application";
                };
                $configurationId = {
                  isa = XCBuildConfiguration;
                  buildSettings = { PRODUCT_NAME = $name; };
                  name = Debug;
                };
              };
            }
        """.trimIndent())
    }
}
