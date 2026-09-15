package dev.appxcode.ide.language

import dev.appxcode.ide.toolchain.AppleToolchain
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

data class SwiftCompletion(val label: String, val detail: String? = null, val insertText: String = label)
data class SwiftDiagnostic(val file: Path, val line: Int, val column: Int, val message: String, val severity: Severity)
data class SwiftDocumentPosition(val file: Path, val line: Int, val column: Int)
enum class Severity { ERROR, WARNING, INFO }

interface SwiftLanguageService {
    fun complete(file: Path, line: Int, column: Int): List<SwiftCompletion>
    fun diagnostics(files: List<Path>): List<SwiftDiagnostic>
    fun definition(file: Path, line: Int, column: Int): List<SwiftDocumentPosition> = emptyList()
    fun references(file: Path, line: Int, column: Int, includeDeclaration: Boolean = true): List<SwiftDocumentPosition> = emptyList()
}

object SwiftLanguageServiceFactory {
    fun create(toolchain: AppleToolchain, workspace: Path): SwiftLanguageService {
        val executable = workspace.resolve(".appxcode/sourcekit-lsp")
            .takeIf { java.nio.file.Files.isExecutable(it) }
            ?: System.getenv("PATH").orEmpty().split(java.io.File.pathSeparator).asSequence()
                .map { Path.of(it, "sourcekit-lsp") }.firstOrNull { java.nio.file.Files.isExecutable(it) }
        return if (executable != null) {
            LspSwiftLanguageService(toolchain, workspace, LspProcessManager(LspServerConfig(executable, workspace)))
        } else UnavailableSwiftLanguageService(toolchain)
    }
}

class LspSwiftLanguageService(
    private val toolchain: AppleToolchain,
    private val workspace: Path,
    private val processManager: LspProcessManager
) : SwiftLanguageService, AutoCloseable {
    private val fallbackIndex = SwiftSymbolIndex()
    private val openedDocuments = mutableMapOf<Path, String>()

    init {
        if (processManager.start()) {
            processManager.initialize(workspace.toUri().toASCIIString())
        }
    }
    fun isAlive(): Boolean = processManager.isAlive()
    fun restart(): Boolean = processManager.restart() && processManager.initialize(workspace.toUri().toASCIIString())
    override fun complete(file: Path, line: Int, column: Int): List<SwiftCompletion> {
        if (!java.nio.file.Files.isRegularFile(file) || line < 1 || column < 0) return emptyList()
        syncDocument(file)
        val lsp = processManager.request(
            "textDocument/completion",
            "{\"textDocument\":{\"uri\":${json(file.toUri().toASCIIString())}},\"position\":{\"line\":${line - 1},\"character\":$column}}",
        )
        parseCompletions(lsp).takeIf { it.isNotEmpty() }?.let { return it }
        val sourceLine = runCatching { java.nio.file.Files.readAllLines(file).getOrNull(line - 1) }.getOrNull() ?: return emptyList()
        val safeColumn = column.coerceAtMost(sourceLine.length)
        val prefix = TOKEN.find(sourceLine.substring(0, safeColumn))?.value.orEmpty()
        if (prefix.isEmpty()) return emptyList()
        val files = runCatching {
            java.nio.file.Files.walk(workspace).use { stream -> stream.filter { java.nio.file.Files.isRegularFile(it) && it.toString().endsWith(".swift") }.toList() }
        }.getOrDefault(listOf(file))
        fallbackIndex.index(files)
        return fallbackIndex.complete(prefix).map { SwiftCompletion(it.name, it.kind, it.name) }
    }
    override fun diagnostics(files: List<Path>): List<SwiftDiagnostic> =
        UnavailableSwiftLanguageService(toolchain).diagnostics(files)
    override fun definition(file: Path, line: Int, column: Int): List<SwiftDocumentPosition> =
        locations("textDocument/definition", file, line, column, null)
    override fun references(file: Path, line: Int, column: Int, includeDeclaration: Boolean): List<SwiftDocumentPosition> =
        locations("textDocument/references", file, line, column, "\"context\":{\"includeDeclaration\":$includeDeclaration}")
    override fun close() = processManager.close()

    private fun syncDocument(file: Path) {
        val text = runCatching { Files.readString(file) }.getOrNull() ?: return
        val previous = openedDocuments[file]
        val uri = json(file.toUri().toASCIIString())
        val encodedText = json(text)
        if (previous == null) {
            processManager.notify("textDocument/didOpen", "{\"textDocument\":{\"uri\":$uri,\"languageId\":\"swift\",\"version\":1,\"text\":$encodedText}}")
        } else if (previous != text) {
            processManager.notify("textDocument/didChange", "{\"textDocument\":{\"uri\":$uri,\"version\":2},\"contentChanges\":[{\"text\":$encodedText}]}")
        }
        openedDocuments[file] = text
    }

    private fun locations(method: String, file: Path, line: Int, column: Int, extra: String?): List<SwiftDocumentPosition> {
        if (!Files.isRegularFile(file) || line < 1 || column < 0) return emptyList()
        syncDocument(file)
        val suffix = extra?.let { ",$it" }.orEmpty()
        val response = processManager.request(
            method,
            "{\"textDocument\":{\"uri\":${json(file.toUri().toASCIIString())}},\"position\":{\"line\":${line - 1},\"character\":$column}$suffix}",
        ) ?: return emptyList()
        return LOCATION.findAll(response).mapNotNull { match ->
            val path = runCatching { Path.of(URI(match.groupValues[1])) }.getOrNull() ?: return@mapNotNull null
            SwiftDocumentPosition(path, match.groupValues[2].toInt() + 1, match.groupValues[3].toInt())
        }.toList()
    }

    private fun parseCompletions(response: String?): List<SwiftCompletion> {
        if (response == null) return emptyList()
        return COMPLETION.findAll(response).map { match ->
            SwiftCompletion(unescape(match.groupValues[1]), match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let(::unescape))
        }.toList()
    }

    private companion object {
        val TOKEN = Regex("[A-Za-z_][A-Za-z0-9_]*$")
        val COMPLETION = Regex("\\{[^{}]*\\\"label\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"(?:[^{}]*\\\"detail\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\")?[^{}]*}")
        val LOCATION = Regex("\\\"uri\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^{}]*?\\\"start\\\"\\s*:\\s*\\{\\s*\\\"line\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"character\\\"\\s*:\\s*(\\d+)")
        fun json(value: String): String = LspProcessManager.jsonString(value)
        fun unescape(value: String): String = value.replace("\\\"", "\"").replace("\\\\", "\\")
    }
}

class UnavailableSwiftLanguageService(private val toolchain: AppleToolchain) : SwiftLanguageService {
    override fun complete(file: Path, line: Int, column: Int): List<SwiftCompletion> = emptyList()
    override fun diagnostics(files: List<Path>): List<SwiftDiagnostic> = files.flatMap { file ->
        if (!java.nio.file.Files.isRegularFile(file)) return@flatMap emptyList()
        val lines = runCatching { java.nio.file.Files.readAllLines(file) }.getOrNull() ?: return@flatMap emptyList()
        buildList {
            if (toolchain.swiftPath == null) add(SwiftDiagnostic(file, 1, 1, "Swift toolchain is unavailable", Severity.INFO))
            lines.forEachIndexed { index, line ->
                if (line.contains("TODO") || line.contains("FIXME")) add(SwiftDiagnostic(file, index + 1, 1, line.trim(), Severity.INFO))
            }
            val balance = lines.joinToString("").count { it == '{' } - lines.joinToString("").count { it == '}' }
            if (balance != 0) add(SwiftDiagnostic(file, lines.size, 1, "Unbalanced braces", Severity.ERROR))
        }
    }
}
