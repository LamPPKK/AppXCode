package dev.appxcode.ide.language

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class LspServerConfig(val executable: Path, val workspace: Path, val arguments: List<String> = emptyList())
enum class LspState { STOPPED, STARTING, RUNNING, FAILED }

class LspProcessManager(private val config: LspServerConfig) : AutoCloseable {
    @Volatile var state: LspState = LspState.STOPPED
        private set
    private var process: Process? = null
    private var input: BufferedInputStream? = null
    private var output: BufferedOutputStream? = null
    private var readerThread: Thread? = null
    private val nextId = AtomicInteger(1)
    private val responses = ConcurrentHashMap<Int, String>()

    @Synchronized
    fun start(): Boolean {
        if (state == LspState.RUNNING && process?.isAlive == true) return true
        state = LspState.STARTING
        return runCatching {
            val started = ProcessBuilder(listOf(config.executable.toString()) + config.arguments)
                .directory(config.workspace.toFile())
                .start()
            process = started
            input = BufferedInputStream(started.inputStream)
            output = BufferedOutputStream(started.outputStream)
            started.errorStream.bufferedReader().let { reader ->
                Thread({ reader.use { it.forEachLine { } } }, "appxcode-sourcekit-stderr").apply {
                    isDaemon = true
                    start()
                }
            }
            readerThread = Thread(::readLoop, "appxcode-sourcekit-lsp").apply {
                isDaemon = true
                start()
            }
            if (!started.isAlive) {
                state = LspState.FAILED
                false
            } else {
                state = LspState.RUNNING
                true
            }
        }.getOrElse {
            state = LspState.FAILED
            false
        }
    }

    fun isAlive(): Boolean = process?.isAlive == true

    @Synchronized
    fun request(method: String, paramsJson: String = "{}", timeoutMillis: Long = 3_000): String? {
        if (!start()) return null
        val id = nextId.getAndIncrement()
        val payload = "{\"jsonrpc\":\"2.0\",\"id\":$id,\"method\":${jsonString(method)},\"params\":$paramsJson}"
        if (!writeMessage(payload)) return null
        val deadline = System.nanoTime() + timeoutMillis.coerceAtLeast(1) * 1_000_000
        while (System.nanoTime() < deadline) {
            responses.remove(id)?.let { return it }
            if (!isAlive()) break
            Thread.sleep(5)
        }
        return responses.remove(id)
    }

    @Synchronized
    fun notify(method: String, paramsJson: String = "{}"): Boolean {
        if (!start()) return false
        return writeMessage("{\"jsonrpc\":\"2.0\",\"method\":${jsonString(method)},\"params\":$paramsJson}")
    }

    fun restart(): Boolean {
        close()
        return start()
    }

    fun initialize(rootUri: String, timeoutMillis: Long = 5_000): Boolean {
        val response = request(
            "initialize",
            "{\"processId\":null,\"rootUri\":${jsonString(rootUri)},\"capabilities\":{},\"clientInfo\":{\"name\":\"AppXCode\"}}",
            timeoutMillis,
        ) ?: return false
        if (!response.contains("\"result\"")) return false
        return notify("initialized", "{}")
    }

    @Synchronized
    override fun close() {
        runCatching { output?.let { writeMessage("{\"jsonrpc\":\"2.0\",\"method\":\"exit\",\"params\":{}}") } }
        runCatching { output?.close() }
        runCatching { input?.close() }
        process?.destroy()
        process = null
        input = null
        output = null
        readerThread = null
        responses.clear()
        state = LspState.STOPPED
    }

    private fun writeMessage(payload: String): Boolean = runCatching {
        val bytes = payload.toByteArray(StandardCharsets.UTF_8)
        val stream = output ?: return false
        stream.write("Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
        stream.write(bytes)
        stream.flush()
        true
    }.getOrDefault(false)

    private fun readLoop() {
        val stream = input ?: return
        try {
            while (process?.isAlive == true) {
                val message = readMessage(stream) ?: break
                val id = ID.find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (id != null) responses[id] = message
            }
        } catch (_: Exception) {
            if (state != LspState.STOPPED) state = LspState.FAILED
        }
    }

    private fun readMessage(stream: BufferedInputStream): String? {
        var contentLength: Int? = null
        while (true) {
            val line = readHeaderLine(stream) ?: return null
            if (line.isEmpty()) break
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                contentLength = line.substringAfter(':').trim().toIntOrNull()
            }
        }
        val length = contentLength ?: return null
        val body = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = stream.read(body, offset, length - offset)
            if (read < 0) return null
            offset += read
        }
        return String(body, StandardCharsets.UTF_8)
    }

    private fun readHeaderLine(stream: BufferedInputStream): String? {
        val buffer = StringBuilder()
        while (true) {
            val value = stream.read()
            if (value < 0) return if (buffer.isEmpty()) null else buffer.toString()
            if (value == '\n'.code) return buffer.toString().trimEnd('\r')
            buffer.append(value.toChar())
        }
    }

    internal companion object {
        val ID = Regex("\\\"id\\\"\\s*:\\s*(\\d+)")
        fun jsonString(value: String): String = buildString {
            append('"')
            value.forEach { character ->
                when (character) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
            append('"')
        }
    }
}
