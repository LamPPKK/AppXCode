package dev.appxcode.ide.flutter

import dev.appxcode.ide.project.XcodeContainer
import dev.appxcode.ide.project.XcodeContainerKind
import dev.appxcode.ide.project.XcodeProjectModel
import java.nio.file.Files

enum class FlutterApplePlatform { IOS, MACOS }

data class FlutterAppleProject(
    val platform: FlutterApplePlatform,
    val container: XcodeContainer,
    val scheme: String?,
)

object FlutterAppleProjectResolver {
    fun resolve(project: FlutterProject): List<FlutterAppleProject> = buildList {
        resolvePlatform(project, FlutterApplePlatform.IOS, roots(project, "ios"))?.let(::add)
        resolvePlatform(project, FlutterApplePlatform.MACOS, roots(project, "macos"))?.let(::add)
    }

    private fun resolvePlatform(project: FlutterProject, platform: FlutterApplePlatform, directories: List<String>): FlutterAppleProject? {
        return directories.asSequence().map(project.root::resolve).filter(Files::isDirectory)
            .mapNotNull { resolveRoot(it, platform) }.firstOrNull()
    }

    private fun resolveRoot(platformRoot: java.nio.file.Path, platform: FlutterApplePlatform): FlutterAppleProject? {
        val containers = XcodeProjectModel.discover(platformRoot)
            .filterNot { it.path.fileName.toString() == "Pods.xcodeproj" }
            .filter { it.path.parent == platformRoot }
        val selected = containers.sortedWith(
            compareBy<XcodeContainer>(
                { if (it.path.fileName.toString() == "Runner.xcworkspace") 0 else if (it.kind == XcodeContainerKind.WORKSPACE) 1 else 2 },
                { it.displayName.lowercase() },
            ),
        ).firstOrNull() ?: return null
        val scheme = when {
            "Runner" in selected.schemes -> "Runner"
            selected.schemes.size == 1 -> selected.schemes.single()
            else -> null
        }
        return FlutterAppleProject(platform, selected, scheme)
    }

    private fun roots(project: FlutterProject, platform: String): List<String> =
        if (project.kind == FlutterProjectKind.MODULE) listOf(".$platform", platform) else listOf(platform, ".$platform")
}
