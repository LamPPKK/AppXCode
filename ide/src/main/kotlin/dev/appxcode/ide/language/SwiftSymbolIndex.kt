package dev.appxcode.ide.language

import java.nio.file.Files
import java.nio.file.Path

data class SwiftSymbol(val name: String, val kind: String, val file: Path, val line: Int, val column: Int = 1)
data class SwiftReference(val name: String, val file: Path, val line: Int, val column: Int)
data class SwiftNavigationTarget(val name: String, val file: Path, val line: Int, val column: Int, val kind: String? = null)

class SwiftSymbolIndex {
    private val symbols = mutableListOf<SwiftSymbol>()
    private val references = mutableListOf<SwiftReference>()

    @Synchronized fun index(files: Iterable<Path>) {
        symbols.clear()
        references.clear()
        files.filter { Files.isRegularFile(it) && it.toString().endsWith(".swift") }.forEach { file ->
            runCatching { Files.readAllLines(file) }.getOrDefault(emptyList()).forEachIndexed { index, line ->
                DECLARATION.find(line)?.let {
                    val nameGroup = it.groups[2]
                    symbols += SwiftSymbol(it.groupValues[2], it.groupValues[1], file, index + 1, (nameGroup?.range?.first ?: 0) + 1)
                }
                IDENTIFIER.findAll(line).forEach { match ->
                    references += SwiftReference(match.value, file, index + 1, match.range.first + 1)
                }
            }
        }
        symbols.sortWith(
            compareBy<SwiftSymbol> { it.name.lowercase() }
                .thenBy { it.file.toString() }
                .thenBy { it.line },
        )
    }

    @Synchronized fun find(name: String): List<SwiftSymbol> = symbols.filter { it.name == name }
    @Synchronized fun find(name: String, kind: String): List<SwiftSymbol> = symbols.filter { it.name == name && it.kind == kind }
    @Synchronized fun references(name: String): List<SwiftReference> = references.filter { it.name == name }
    @Synchronized fun declarationTargets(name: String): List<SwiftNavigationTarget> = find(name).map {
        SwiftNavigationTarget(it.name, it.file, it.line, it.column, it.kind)
    }
    @Synchronized fun usageTargets(name: String, includeDeclarations: Boolean = false): List<SwiftNavigationTarget> {
        val declarationLocations = symbols.asSequence()
            .filter { it.name == name }
            .map { Triple(it.file, it.line, it.column) }
            .toSet()
        return references(name)
            .filter { includeDeclarations || Triple(it.file, it.line, it.column) !in declarationLocations }
            .map { SwiftNavigationTarget(it.name, it.file, it.line, it.column) }
    }
    @Synchronized fun complete(prefix: String): List<SwiftSymbol> = symbols
        .filter { it.name.startsWith(prefix, ignoreCase = true) }
        .distinctBy { it.name to it.kind }

    private companion object {
        val DECLARATION = Regex("\\b(class|struct|enum|func|var|let|protocol)\\s+([A-Za-z_][A-Za-z0-9_]*)")
        val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}
