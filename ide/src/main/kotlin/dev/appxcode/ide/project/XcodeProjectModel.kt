package dev.appxcode.ide.project

import java.nio.file.Files
import java.nio.file.Path

enum class XcodeContainerKind { PROJECT, WORKSPACE }

data class XcodeContainer(
    val path: Path,
    val kind: XcodeContainerKind,
    val schemes: List<String> = emptyList(),
) {
    val displayName: String get() = path.fileName.toString().substringBeforeLast('.')
}

data class XcodeScheme(val name: String, val buildables: List<String>, val testables: List<String>)

/** Discovers Xcode containers without converting or rewriting their native files. */
object XcodeProjectModel {
    fun readScheme(path: Path): XcodeScheme? {
        if (!Files.isRegularFile(path) || !path.fileName.toString().endsWith(".xcscheme")) return null
        val text = Files.readString(path)
        val buildables = Regex("BuildableName=\\\"([^\\\"]+)\\\"").findAll(text).map { it.groupValues[1] }.distinct().toList()
        val testables = Regex("BlueprintName=\\\"([^\\\"]+)\\\"").findAll(text).map { it.groupValues[1] }.distinct().toList()
        return XcodeScheme(path.fileName.toString().removeSuffix(".xcscheme"), buildables, testables)
    }
    fun discover(root: Path): List<XcodeContainer> {
        if (!Files.isDirectory(root)) return emptyList()
        return Files.list(root).use { stream ->
            stream.filter { Files.isDirectory(it) }
                .mapNotNull { path ->
                    when {
                        path.fileName.toString().endsWith(".xcworkspace") -> container(path, XcodeContainerKind.WORKSPACE)
                        path.fileName.toString().endsWith(".xcodeproj") -> container(path, XcodeContainerKind.PROJECT)
                        else -> null
                    }
                }
                .sorted(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
                .toList()
        }
    }

    private fun container(path: Path, kind: XcodeContainerKind): XcodeContainer {
        val shared = path.resolve("xcshareddata/xcschemes")
        val user = path.resolve("xcuserdata")
        val schemeRoots = listOf(shared, user).filter(Files::isDirectory)
        val schemes = schemeRoots.flatMap { root ->
            Files.walk(root).use { files -> files.filter { it.fileName.toString().endsWith(".xcscheme") }.map { it.fileName.toString().removeSuffix(".xcscheme") }.toList() }
        }.distinct().sorted(String.CASE_INSENSITIVE_ORDER)
        return XcodeContainer(path, kind, schemes)
    }
}
