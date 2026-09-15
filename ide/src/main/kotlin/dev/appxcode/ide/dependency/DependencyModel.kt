package dev.appxcode.ide.dependency

import java.nio.file.Files
import java.nio.file.Path

enum class DependencyManager { SWIFT_PACKAGE_MANAGER, COCOAPODS }
data class DependencyPin(val manager: DependencyManager, val name: String, val version: String?, val revision: String? = null)

object DependencyModel {
    fun read(root: Path): List<DependencyPin> = buildList {
        if (!Files.isDirectory(root)) return@buildList
        swiftResolvedFiles(root).forEach { resolved ->
            addAll(runCatching { readSwiftPins(resolved) }.getOrDefault(emptyList()))
        }
        val lock = root.resolve("Podfile.lock")
        if (Files.isRegularFile(lock)) addAll(runCatching { readPodPins(lock) }.getOrDefault(emptyList()))
    }.distinctBy { it.manager to it.name }

    private fun swiftResolvedFiles(root: Path): List<Path> = buildList {
        root.resolve("Package.resolved").takeIf(Files::isRegularFile)?.let(::add)
        runCatching {
            Files.walk(root, 5).use { paths ->
                paths.filter(Files::isRegularFile)
                    .filter { it.fileName.toString() == "Package.resolved" }
                    .filter { it.toString().contains("xcshareddata${java.io.File.separator}swiftpm") }
                    .forEach(::add)
            }
        }
    }.distinct().sortedBy(Path::toString)

    private fun readSwiftPins(path: Path): List<DependencyPin> {
        val text = Files.readString(path)
        val starts = PIN_START.findAll(text).toList()
        return starts.mapIndexed { index, match ->
            val end = starts.getOrNull(index + 1)?.range?.first ?: text.length
            val pinText = text.substring(match.range.first, end)
            DependencyPin(
                DependencyManager.SWIFT_PACKAGE_MANAGER,
                match.groupValues[2],
                field(pinText, "version"),
                field(pinText, "revision"),
            )
        }
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

    private fun field(text: String, name: String): String? =
        Regex("\\\"${Regex.escape(name)}\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1)

    private val PIN_START = Regex("\\\"(identity|package)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
}
