package dev.appxcode.ide.language

import java.nio.file.Files
import java.nio.file.Path

data class ObjCSymbol(val name: String, val kind: String, val file: Path, val line: Int)

class ObjCSymbolIndex {
    private val symbols = mutableListOf<ObjCSymbol>()

    @Synchronized fun index(files: Iterable<Path>) {
        symbols.clear()
        files.filter { Files.isRegularFile(it) && (it.toString().endsWith(".h") || it.toString().endsWith(".m") || it.toString().endsWith(".mm")) }
            .forEach { file ->
                runCatching { Files.readAllLines(file) }.getOrDefault(emptyList()).forEachIndexed { index, line ->
                    Regex("\\b(?:@interface|@implementation|@protocol)\\s+([A-Za-z_][A-Za-z0-9_]*)").find(line)
                        ?.let { symbols += ObjCSymbol(it.groupValues[1], "type", file, index + 1) }
                    Regex("^[+-]\\s*\\([^)]*\\)\\s*([A-Za-z_][A-Za-z0-9_]*)").find(line)
                        ?.let { symbols += ObjCSymbol(it.groupValues[1], "method", file, index + 1) }
                }
            }
        symbols.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.kind }.thenBy { it.file.toString() }.thenBy { it.line })
    }

    @Synchronized fun find(name: String): List<ObjCSymbol> = symbols.filter { it.name == name }
    @Synchronized fun find(name: String, kind: String): List<ObjCSymbol> = symbols.filter { it.name == name && it.kind == kind }
    @Synchronized fun complete(prefix: String): List<ObjCSymbol> = symbols
        .filter { it.name.startsWith(prefix, ignoreCase = true) }
        .distinctBy { it.name to it.kind }
}
