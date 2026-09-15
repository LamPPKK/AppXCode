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
            var inBlockComment = false
            runCatching { Files.readAllLines(file) }.getOrDefault(emptyList()).forEachIndexed { index, line ->
                val code = stripCommentsAndStrings(line, inBlockComment)
                inBlockComment = code.second
                DECLARATION.find(code.first)?.let {
                    val nameGroup = it.groups[2]
                    symbols += SwiftSymbol(it.groupValues[2], it.groupValues[1], file, index + 1, (nameGroup?.range?.first ?: 0) + 1)
                }
                IDENTIFIER.findAll(code.first).forEach { match ->
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

        fun stripCommentsAndStrings(line: String, initiallyInBlockComment: Boolean): Pair<String, Boolean> {
            val result = line.toCharArray()
            var inBlockComment = initiallyInBlockComment
            var inString = false
            var escaped = false
            var index = 0
            while (index < line.length) {
                if (inBlockComment) {
                    result[index] = ' '
                    if (index + 1 < line.length && line[index] == '*' && line[index + 1] == '/') {
                        result[index + 1] = ' '
                        inBlockComment = false
                        index += 2
                    } else index++
                    continue
                }
                if (inString) {
                    result[index] = ' '
                    if (escaped) escaped = false
                    else if (line[index] == '\\') escaped = true
                    else if (line[index] == '"') inString = false
                    index++
                    continue
                }
                if (index + 1 < line.length && line[index] == '/' && line[index + 1] == '/') {
                    for (tail in index until line.length) result[tail] = ' '
                    break
                }
                if (index + 1 < line.length && line[index] == '/' && line[index + 1] == '*') {
                    result[index] = ' '
                    result[index + 1] = ' '
                    inBlockComment = true
                    index += 2
                    continue
                }
                if (line[index] == '"') {
                    result[index] = ' '
                    inString = true
                }
                index++
            }
            return String(result) to inBlockComment
        }
    }
}
