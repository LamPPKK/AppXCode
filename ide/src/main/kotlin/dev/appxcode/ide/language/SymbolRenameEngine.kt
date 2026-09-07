package dev.appxcode.ide.language

import java.nio.file.Files
import java.nio.file.Path

data class TextEdit(val file: Path, val startOffset: Int, val endOffset: Int, val replacement: String)
data class RenamePreview(val symbol: String, val replacement: String, val edits: List<TextEdit>)

/** Produces previewable identifier edits; callers decide when and how to write them. */
object SymbolRenameEngine {
    fun preview(files: Iterable<Path>, symbol: String, replacement: String): RenamePreview {
        require(symbol.matches(IDENTIFIER)) { "symbol must be an identifier" }
        require(replacement.matches(IDENTIFIER)) { "replacement must be an identifier" }
        val edits = files.flatMap { file ->
            if (!Files.isRegularFile(file)) return@flatMap emptyList()
            val text = Files.readString(file)
            IDENTIFIER.findAll(text).filter { it.value == symbol }.map { TextEdit(file, it.range.first, it.range.last + 1, replacement) }.toList()
        }
        return RenamePreview(symbol, replacement, edits)
    }

    fun apply(preview: RenamePreview): Set<Path> {
        preview.edits.groupBy { it.file }.forEach { (file, edits) ->
            var text = Files.readString(file)
            edits.sortedByDescending { it.startOffset }.forEach { edit ->
                require(edit.endOffset <= text.length && edit.startOffset >= 0) { "stale rename preview for $file" }
                text = text.substring(0, edit.startOffset) + edit.replacement + text.substring(edit.endOffset)
            }
            Files.writeString(file, text)
        }
        return preview.edits.map { it.file }.toSet()
    }

    private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
