package dev.appxcode.ide.language

import dev.appxcode.ide.toolchain.AppleToolchain
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

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
    private data class OpenDocument(var text: String, var version: Int)
    private val openedDocuments = mutableMapOf<Path, OpenDocument>()
    private val diagnosticsByFile = ConcurrentHashMap<Path, List<SwiftDiagnostic>>()
    private val notificationSubscription: AutoCloseable

    init {
        notificationSubscription = processManager.onNotification { method, message ->
            if (method == "textDocument/publishDiagnostics") handleDiagnostics(message)
        }
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
    override fun diagnostics(files: List<Path>): List<SwiftDiagnostic> {
        val regularFiles = files.filter(Files::isRegularFile)
        regularFiles.forEach(::syncDocument)
        val fallback = UnavailableSwiftLanguageService(toolchain)
        return regularFiles.flatMap { file -> diagnosticsByFile[file.toAbsolutePath().normalize()].orEmpty() }
            .takeIf { it.isNotEmpty() }
            ?: fallback.diagnostics(files)
    }
    override fun definition(file: Path, line: Int, column: Int): List<SwiftDocumentPosition> =
        locations("textDocument/definition", file, line, column, null)
    override fun references(file: Path, line: Int, column: Int, includeDeclaration: Boolean): List<SwiftDocumentPosition> =
        locations("textDocument/references", file, line, column, "\"context\":{\"includeDeclaration\":$includeDeclaration}")
    fun closeDocument(file: Path) {
        val normalized = file.toAbsolutePath().normalize()
        openedDocuments.remove(normalized) ?: return
        processManager.notify(
            "textDocument/didClose",
            "{\"textDocument\":{\"uri\":${json(normalized.toUri().toASCIIString())}}}"
        )
        diagnosticsByFile.remove(normalized)
    }

    override fun close() {
        openedDocuments.keys.toList().forEach(::closeDocument)
        runCatching { notificationSubscription.close() }
        processManager.close()
    }

    private fun syncDocument(file: Path) {
        val normalized = file.toAbsolutePath().normalize()
        val text = runCatching { Files.readString(normalized) }.getOrNull() ?: return
        val previous = openedDocuments[normalized]
        val uri = json(normalized.toUri().toASCIIString())
        val encodedText = json(text)
        if (previous == null) {
            processManager.notify("textDocument/didOpen", "{\"textDocument\":{\"uri\":$uri,\"languageId\":\"swift\",\"version\":1,\"text\":$encodedText}}")
            openedDocuments[normalized] = OpenDocument(text, 1)
        } else if (previous.text != text) {
            val version = previous.version + 1
            processManager.notify("textDocument/didChange", "{\"textDocument\":{\"uri\":$uri,\"version\":$version},\"contentChanges\":[{\"text\":$encodedText}]}")
            previous.text = text
            previous.version = version
        }
    }

    private fun handleDiagnostics(message: String) {
        val uri = DIAGNOSTIC_URI.find(message)?.groupValues?.getOrNull(1) ?: return
        val file = runCatching { Path.of(URI(unescape(uri))).toAbsolutePath().normalize() }.getOrNull() ?: return
        val diagnostics = DIAGNOSTIC.findAll(message).mapNotNull { match ->
            val severity = when (match.groupValues[3].toIntOrNull()) {
                1 -> Severity.ERROR
                2 -> Severity.WARNING
                else -> Severity.INFO
            }
            val line = match.groupValues[1].toIntOrNull()?.plus(1) ?: return@mapNotNull null
            val column = match.groupValues[2].toIntOrNull()?.plus(1) ?: return@mapNotNull null
            SwiftDiagnostic(file, line, column, unescape(match.groupValues[4]), severity)
        }.toList()
        diagnosticsByFile[file] = diagnostics
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
        val DIAGNOSTIC_URI = Regex("\\\"uri\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"")
        val DIAGNOSTIC = Regex("\\\"range\\\"\\s*:\\s*\\{\\s*\\\"start\\\"\\s*:\\s*\\{\\s*\\\"line\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"character\\\"\\s*:\\s*(\\d+)[\\s\\S]*?\\\"severity\\\"\\s*:\\s*(\\d+)[\\s\\S]*?\\\"message\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"")
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
