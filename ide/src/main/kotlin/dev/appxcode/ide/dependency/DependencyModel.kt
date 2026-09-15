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
    }.distinctBy { listOf(it.manager.name, it.name.lowercase(), it.version.orEmpty(), it.revision.orEmpty()) }

    private fun swiftResolvedFiles(root: Path): List<Path> = buildList {
        root.resolve("Package.resolved").takeIf(Files::isRegularFile)?.let(::add)
        runCatching {
            Files.walk(root).use { paths ->
                paths.filter(Files::isRegularFile)
                    .filter { it.fileName.toString() == "Package.resolved" }
                    .filter { it.parent?.fileName?.toString() == "swiftpm" && it.parent?.parent?.fileName?.toString() == "xcshareddata" }
                    .forEach(::add)
            }
        }
    }.distinct().sortedBy(Path::toString)

    private fun readSwiftPins(path: Path): List<DependencyPin> {
        val text = Files.readString(path)
        return jsonObjects(text).mapNotNull { pinText ->
            if (PINS_FIELD.containsMatchIn(pinText)) return@mapNotNull null
            val match = PIN_START.find(pinText) ?: return@mapNotNull null
            if (field(pinText, "revision") == null && field(pinText, "version") == null) return@mapNotNull null
            DependencyPin(
                DependencyManager.SWIFT_PACKAGE_MANAGER,
                match.groupValues[2],
                field(pinText, "version"),
                field(pinText, "revision"),
            )
        }.toList()
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
    private val PINS_FIELD = Regex("\\\"pins\\\"\\s*:")

    private fun jsonObjects(text: String): Sequence<String> = sequence {
        val starts = ArrayDeque<Int>()
        var inString = false
        var escaped = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (inString) {
                if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '\"') inString = false
            } else when (char) {
                '\"' -> inString = true
                '{' -> starts.addLast(index)
                '}' -> if (starts.isNotEmpty()) yield(text.substring(starts.removeLast(), index + 1))
            }
            index++
        }
    }
}
