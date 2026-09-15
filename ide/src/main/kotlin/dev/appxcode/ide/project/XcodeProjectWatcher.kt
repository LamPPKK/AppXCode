package dev.appxcode.ide.project

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class XcodeProjectWatcher(private val root: Path) : AutoCloseable {
    private val listeners = CopyOnWriteArrayList<(Path) -> Unit>()
    private val service = FileSystems.getDefault().newWatchService()
    @Volatile private var running = true
    private val worker: Thread

    init {
        if (!java.nio.file.Files.isDirectory(root)) {
            service.close()
            throw IllegalArgumentException("Project watcher root must be a directory")
        }
        runCatching {
            java.nio.file.Files.walk(root).use { paths ->
                paths.filter(java.nio.file.Files::isDirectory).forEach { directory ->
                    runCatching { registerDirectory(directory) }
                }
            }
        }.onFailure { service.close(); throw it }
        worker = thread(isDaemon = true, name = "appxcode-project-watcher") { loop() }
    }
    fun onChange(listener: (Path) -> Unit) {
        if (running) listeners += listener
    }

    fun removeListener(listener: (Path) -> Unit) { listeners -= listener }

    private fun loop() {
        while (running) {
            val key = runCatching { service.take() }.getOrNull() ?: break
            key.pollEvents().forEach { event ->
                val watched = key.watchable() as Path
                if (event.kind() == OVERFLOW) {
                    notifyListeners(root)
                    return@forEach
                }
                val path = watched.resolve(event.context() as Path)
                if (event.kind() == ENTRY_CREATE && java.nio.file.Files.isDirectory(path)) {
                    runCatching {
                        java.nio.file.Files.walk(path).use { paths ->
                            paths.forEach { candidate ->
                                if (java.nio.file.Files.isDirectory(candidate)) registerDirectory(candidate)
                                if (isProjectModelPath(candidate)) notifyListeners(candidate)
                            }
                        }
                    }
                }
                if (isProjectModelPath(path)) notifyListeners(path)
            }
            if (!key.reset()) break
        }
    }

    private fun registerDirectory(directory: Path) {
        if (java.nio.file.Files.isDirectory(directory) && java.nio.file.Files.isReadable(directory)) {
            directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        }
    }

    private fun notifyListeners(path: Path) {
        listeners.forEach { listener -> runCatching { listener(path) } }
    }

    override fun close() { running = false; service.close(); worker.interrupt(); listeners.clear() }

    companion object {
        internal fun isProjectModelPath(path: Path): Boolean = path.fileName?.toString().orEmpty().let { name ->
            name.endsWith(".xcodeproj") ||
                name.endsWith(".xcworkspace") ||
                name.endsWith(".xcscheme") ||
                name.endsWith(".xcconfig") ||
                name == "project.pbxproj" ||
                name == "contents.xcworkspacedata" ||
                name == "Package.swift" ||
                name == "Package.resolved" ||
                name == "Podfile" ||
                name == "Podfile.lock"
        }
    }
}
