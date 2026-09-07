package dev.appxcode.ide.project

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class XcodeProjectWatcher(private val root: Path) : AutoCloseable {
    private val listeners = CopyOnWriteArrayList<(Path) -> Unit>()
    private val service = FileSystems.getDefault().newWatchService()
    @Volatile private var running = true
    private val worker = thread(isDaemon = true, name = "appxcode-project-watcher") { loop() }

    init {
        java.nio.file.Files.walk(root).use { paths -> paths.filter(java.nio.file.Files::isDirectory).forEach { it.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY) } }
    }
    fun onChange(listener: (Path) -> Unit) { listeners += listener }

    private fun loop() {
        while (running) {
            val key = runCatching { service.take() }.getOrNull() ?: break
            key.pollEvents().forEach { event ->
                val watched = key.watchable() as Path
                val path = watched.resolve(event.context() as Path)
                if (event.kind() == ENTRY_CREATE && java.nio.file.Files.isDirectory(path)) {
                    runCatching {
                        java.nio.file.Files.walk(path).use { paths ->
                            paths.filter(java.nio.file.Files::isDirectory).forEach { registerDirectory(it) }
                        }
                    }
                }
                if (path.fileName.toString().let { it.endsWith(".xcodeproj") || it.endsWith(".xcworkspace") || it == "Package.resolved" || it == "Podfile.lock" }) listeners.forEach { it(path) }
            }
            if (!key.reset()) break
        }
    }

    private fun registerDirectory(directory: Path) {
        directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    }

    override fun close() { running = false; service.close(); worker.interrupt(); listeners.clear() }
}
