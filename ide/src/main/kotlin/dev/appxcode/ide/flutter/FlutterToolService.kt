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
    fun run(root: Path, deviceId: String? = null): FlutterCommandResult = execute(root, "run", deviceId)
    fun test(root: Path): FlutterCommandResult = execute(root, "test", null)
    fun hotReload(root: Path): FlutterCommandResult = sendSignal(root, "r")
    fun hotRestart(root: Path): FlutterCommandResult = sendSignal(root, "R")

    private fun execute(root: Path, action: String, deviceId: String?): FlutterCommandResult =
        runner(buildList { add(flutter); add(action); if (deviceId != null) { add("-d"); add(deviceId) } }, root)

    private fun sendSignal(root: Path, signal: String): FlutterCommandResult =
        FlutterCommandResult(false, "Interactive Flutter session required to send '$signal'", null)
}
