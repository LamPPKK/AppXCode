package dev.appxcode.ide.project

import dev.appxcode.ide.dependency.DependencyPin
import dev.appxcode.ide.dependency.DependencyModel
import java.nio.file.Path

data class ProjectSnapshot(
    val root: Path,
    val containers: List<XcodeContainer>,
    val targets: List<XcodeTarget>,
    val dependencies: List<DependencyPin>,
)

object ProjectSnapshotLoader {
    fun load(root: Path): ProjectSnapshot {
        val containers = XcodeProjectModel.discover(root)
        val project = containers.firstOrNull { it.kind == XcodeContainerKind.PROJECT }
        return ProjectSnapshot(root, containers, project?.let(XcodeProjectModel::readTargets) ?: emptyList(), DependencyModel.read(root))
    }
}
