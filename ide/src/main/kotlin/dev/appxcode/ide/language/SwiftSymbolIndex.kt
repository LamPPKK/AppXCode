package dev.appxcode.ide.language

import java.nio.file.Files
import java.nio.file.Path

data class SwiftSymbol(val name: String, val kind: String, val file: Path, val line: Int)

class SwiftSymbolIndex {
    private val symbols = mutableListOf<SwiftSymbol>()

    @Synchronized fun index(files: Iterable<Path>) {
        symbols.clear()
        files.filter { Files.isRegularFile(it) && it.toString().endsWith(".swift") }.forEach { file ->
            runCatching { Files.readAllLines(file) }.getOrDefault(emptyList()).forEachIndexed { index, line ->
                DECLARATION.find(line)?.let { symbols += SwiftSymbol(it.groupValues[2], it.groupValues[1], file, index + 1) }
            }
        }
        symbols.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.file.toString() }.thenBy { it.line })
    }

    @Synchronized fun find(name: String): List<SwiftSymbol> = symbols.filter { it.name == name }
    @Synchronized fun find(name: String, kind: String): List<SwiftSymbol> = symbols.filter { it.name == name && it.kind == kind }
    @Synchronized fun complete(prefix: String): List<SwiftSymbol> = symbols
        .filter { it.name.startsWith(prefix, ignoreCase = true) }
        .distinctBy { it.name to it.kind }

    private companion object { val DECLARATION = Regex("\\b(class|struct|enum|func|var|let|protocol)\\s+([A-Za-z_][A-Za-z0-9_]*)") }
}
