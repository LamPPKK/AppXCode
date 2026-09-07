package dev.appxcode.ide.flutter

import java.nio.file.Path

data class FlutterCommandResult(val success: Boolean, val output: String, val exitCode: Int?)

class FlutterToolService(
    private val flutter: String = "flutter",
    private val runner: (List<String>, Path) -> FlutterCommandResult = { args, root ->
        val process = ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        FlutterCommandResult(code == 0, output, code)
    },
) {
    private var session: Process? = null
    private val sessionOutput = StringBuffer()

    @Synchronized fun startSession(root: Path, deviceId: String? = null): Boolean {
        if (!java.nio.file.Files.isDirectory(root)) return false
        if (session?.isAlive == true) return true
        session = null
        sessionOutput.setLength(0)
        val args = buildList { add(flutter); add("run"); if (deviceId != null) { add("-d"); add(deviceId) } }
        return runCatching {
            session = ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start()
            if (session?.isAlive != true) { session = null; return@runCatching false }
            Thread {
                session?.inputStream?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        synchronized(sessionOutput) {
                            sessionOutput.append(line).append('\n')
                            if (sessionOutput.length > 1_000_000) sessionOutput.delete(0, sessionOutput.length - 1_000_000)
                        }
                    }
                }
            }.apply { isDaemon = true; start() }
            true
        }.getOrDefault(false)
    }
    @Synchronized fun stopSession() {
        session?.let { process ->
            process.destroy()
            runCatching { if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly() }
        }
        session = null
    }
    fun sessionOutput(): String = synchronized(sessionOutput) { sessionOutput.toString() }
    fun clearSessionOutput() = synchronized(sessionOutput) { sessionOutput.setLength(0) }
    fun sessionAlive(): Boolean = session?.isAlive == true
    fun run(root: Path, deviceId: String? = null): FlutterCommandResult = execute(root, "run", deviceId)
    fun test(root: Path): FlutterCommandResult = execute(root, "test", null)
    fun pubGet(root: Path): FlutterCommandResult = when {
        !java.nio.file.Files.isDirectory(root) -> FlutterCommandResult(false, "Flutter project root does not exist", null)
        !java.nio.file.Files.isRegularFile(root.resolve("pubspec.yaml")) -> FlutterCommandResult(false, "pubspec.yaml not found", null)
        else -> runner(listOf(flutter, "pub", "get"), root)
    }
    fun doctor(root: Path): FlutterCommandResult = if (!java.nio.file.Files.isDirectory(root)) FlutterCommandResult(false, "Flutter project root does not exist", null) else runner(listOf(flutter, "doctor"), root)
    fun hotReload(root: Path): FlutterCommandResult = sendSignal(root, "r")
    fun hotRestart(root: Path): FlutterCommandResult = sendSignal(root, "R")

    private fun execute(root: Path, action: String, deviceId: String?): FlutterCommandResult =
        if (!java.nio.file.Files.isDirectory(root)) FlutterCommandResult(false, "Flutter project root does not exist", null)
        else runner(buildList { add(flutter); add(action); if (deviceId != null) { add("-d"); add(deviceId) } }, root)

    private fun sendSignal(root: Path, signal: String): FlutterCommandResult {
        val process = session ?: return FlutterCommandResult(false, "No active Flutter session", null)
        return runCatching { val writer = process.outputStream.bufferedWriter(); writer.write(signal); writer.flush(); FlutterCommandResult(true, "Sent $signal", null) }
            .getOrElse { FlutterCommandResult(false, it.message ?: "Unable to send Flutter command", null) }
    }
}
