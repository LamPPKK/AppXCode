package dev.appxcode.ide.project

import dev.appxcode.ide.dependency.DependencyPin
import dev.appxcode.ide.dependency.DependencyModel
import java.nio.file.Path

data class ProjectSnapshot(
    val root: Path,
    val containers: List<XcodeContainer>,
    val schemes: List<XcodeScheme>,
    val targets: List<XcodeTarget>,
    val dependencies: List<DependencyPin>,
)

object ProjectSnapshotLoader {
    fun load(root: Path): ProjectSnapshot {
        val containers = XcodeProjectModel.discover(root)
        val project = containers.firstOrNull { it.kind == XcodeContainerKind.PROJECT }
        val schemes = containers.flatMap(XcodeProjectModel::readSchemes).distinctBy(XcodeScheme::name)
        return ProjectSnapshot(root, containers, schemes, project?.let(XcodeProjectModel::readTargets) ?: emptyList(), DependencyModel.read(root))
    }
}
