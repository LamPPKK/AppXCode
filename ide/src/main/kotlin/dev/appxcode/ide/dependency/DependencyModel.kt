package dev.appxcode.ide.dependency

import java.nio.file.Files
import java.nio.file.Path

enum class DependencyManager { SWIFT_PACKAGE_MANAGER, COCOAPODS }
data class DependencyPin(val manager: DependencyManager, val name: String, val version: String?, val revision: String? = null)

object DependencyModel {
    fun read(root: Path): List<DependencyPin> = buildList {
        if (!Files.isDirectory(root)) return@buildList
        val resolved = root.resolve("Package.resolved")
        if (Files.isRegularFile(resolved)) addAll(readSwiftPins(resolved))
        val lock = root.resolve("Podfile.lock")
        if (Files.isRegularFile(lock)) addAll(readPodPins(lock))
    }.distinctBy { it.manager to it.name }

    private fun readSwiftPins(path: Path): List<DependencyPin> {
        val text = Files.readString(path)
        val pattern = Regex("\\\"identity\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[\\s\\S]*?\\\"version\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"(?:[\\s\\S]*?\\\"revision\\\"\\s*:\\s*\\\"([^\\\"]+)\\\")?")
        return pattern.findAll(text).map { match -> DependencyPin(DependencyManager.SWIFT_PACKAGE_MANAGER, match.groupValues[1], match.groupValues[2], match.groupValues.getOrNull(3)?.ifBlank { null }) }.toList()
    }

    private fun readPodPins(path: Path): List<DependencyPin> {
        val lines = Files.readAllLines(path)
        val start = lines.indexOfFirst { it.trim() == "PODS:" }
        if (start < 0) return emptyList()
        return lines.drop(start + 1).takeWhile { it.startsWith("  - ") || it.startsWith("    - ") }
            .mapNotNull { line ->
                val value = line.trim().removePrefix("- ")
                if (value.isBlank() || line.trimStart().startsWith("- ") && line.startsWith("    ")) return@mapNotNull null
                val match = Regex("^([^ (]+) \\(([^)]+)\\)").find(value) ?: return@mapNotNull null
                DependencyPin(DependencyManager.COCOAPODS, match.groupValues[1], match.groupValues[2])
            }
    }
}
