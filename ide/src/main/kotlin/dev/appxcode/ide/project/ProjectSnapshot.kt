package dev.appxcode.ide.project

import dev.appxcode.ide.dependency.DependencyPin
import dev.appxcode.ide.dependency.DependencyModel
import java.nio.file.Path

data class XcodeContainerSnapshot(
    val container: XcodeContainer,
    val schemes: List<XcodeScheme>,
    val targets: List<XcodeTarget>,
    val buildConfigurations: List<XcodeBuildConfiguration>,
)

data class ProjectSnapshot(
    val root: Path,
    val containers: List<XcodeContainer>,
    val schemes: List<XcodeScheme>,
    val targets: List<XcodeTarget>,
    val dependencies: List<DependencyPin>,
    val buildConfigurations: List<XcodeBuildConfiguration> = emptyList(),
    val containerModels: List<XcodeContainerSnapshot> = emptyList(),
)

object ProjectSnapshotLoader {
    fun load(root: Path): ProjectSnapshot {
        if (!java.nio.file.Files.isDirectory(root)) return ProjectSnapshot(root, emptyList(), emptyList(), emptyList(), emptyList())
        val containers = XcodeProjectModel.discover(root)
        val containerModels = containers.map { container ->
            val projects = when (container.kind) {
                XcodeContainerKind.PROJECT -> listOf(container.path)
                XcodeContainerKind.WORKSPACE -> XcodeProjectModel.readWorkspaceProjects(container.path)
            }
            XcodeContainerSnapshot(
                container,
                XcodeProjectModel.readSchemes(container),
                projects.flatMap(XcodeProjectModel::readTargets),
                projects.flatMap(XcodeProjectModel::readBuildConfigurations),
            )
        }
        val schemes = containerModels.flatMap(XcodeContainerSnapshot::schemes)
            .distinctBy(XcodeScheme::name)
            .sortedBy { it.name.lowercase() }
        // Legacy flattened views use project containers only; containerModels is authoritative for workspace-qualified selection.
        val projectModels = containerModels.filter { it.container.kind == XcodeContainerKind.PROJECT }
        val targets = projectModels.flatMap(XcodeContainerSnapshot::targets)
        val buildConfigurations = projectModels.flatMap(XcodeContainerSnapshot::buildConfigurations)
        val dependencies = runCatching { DependencyModel.read(root) }.getOrDefault(emptyList())
        return ProjectSnapshot(root, containers, schemes, targets, dependencies, buildConfigurations, containerModels)
    }
}
