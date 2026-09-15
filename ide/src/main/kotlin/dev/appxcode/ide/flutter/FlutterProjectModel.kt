package dev.appxcode.ide.flutter

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.FileVisitResult
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

enum class FlutterProjectKind { APPLICATION, PACKAGE, PLUGIN, MODULE }

data class FlutterProject(
    val root: Path,
    val name: String,
    val hasIosRunner: Boolean,
    val hasMacosRunner: Boolean,
    val entrypoints: List<Path>,
    val hasAndroidRunner: Boolean = false,
    val kind: FlutterProjectKind = FlutterProjectKind.APPLICATION,
)

object FlutterProjectDetector {
    fun discover(root: Path): List<FlutterProject> {
        if (!Files.isDirectory(root)) return emptyList()
        val projects = mutableListOf<FlutterProject>()
        runCatching {
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
                    if (dir != root && dir.fileName.toString() in IGNORED_DIRECTORIES) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (attrs.isRegularFile && file.fileName.toString() == "pubspec.yaml") detect(file.parent)?.let(projects::add)
                    return FileVisitResult.CONTINUE
                }
                override fun visitFileFailed(file: Path, exc: java.io.IOException): FileVisitResult = FileVisitResult.CONTINUE
            })
        }
        return projects.distinctBy { it.root.toAbsolutePath().normalize() }.sortedBy { it.root.toString() }
    }

    fun detect(root: Path): FlutterProject? {
        val pubspec = root.resolve("pubspec.yaml")
        if (!Files.isRegularFile(pubspec)) return null
        val text = runCatching { Files.readString(pubspec) }.getOrNull() ?: return null
        val name = Regex("(?m)^name:\\s*([^#\\r\\n]+)").find(text)?.groupValues?.get(1)
            ?.trim()?.trim('"', '\'')?.takeIf { it.isNotBlank() } ?: root.fileName.toString()
        val metadataFile = root.resolve(".metadata")
        val metadata = metadataFile.takeIf(Files::isRegularFile)?.let { runCatching { Files.readString(it) }.getOrDefault("") }.orEmpty()
        val flutterBlock = topLevelBlock(text, "flutter")
        val hasIos = Files.isDirectory(root.resolve("ios"))
        val hasMacos = Files.isDirectory(root.resolve("macos"))
        val hasAndroid = Files.isDirectory(root.resolve("android"))
        val flutterSdkDependency = Regex("(?m)^\\s+flutter:\\s*$[\\r\\n]+\\s+sdk:\\s*flutter\\s*$").containsMatchIn(text)
        if (metadata.isEmpty() && flutterBlock == null && !flutterSdkDependency && !hasIos && !hasMacos && !hasAndroid) return null
        val entrypoints = buildList {
            val lib = root.resolve("lib")
            if (Files.isDirectory(lib)) runCatching { Files.walk(lib).use { stream -> stream.filter { it.fileName.toString().endsWith(".dart") }.forEach(::add) } }
            val test = root.resolve("test")
            if (Files.isDirectory(test)) runCatching { Files.walk(test).use { stream -> stream.filter { it.fileName.toString().endsWith(".dart") }.forEach(::add) } }
        }.distinct().sortedBy { it.toString() }
        return FlutterProject(root, name, hasIos, hasMacos, entrypoints, hasAndroid, detectKind(metadata, flutterBlock, hasIos || hasMacos || hasAndroid))
    }

    private fun detectKind(metadata: String, flutterBlock: String?, hasRunner: Boolean): FlutterProjectKind {
        val declared = Regex("(?m)^project_type:\\s*(\\w+)").find(metadata)?.groupValues?.get(1)?.lowercase()
        return when {
            declared == "module" -> FlutterProjectKind.MODULE
            declared == "plugin" || flutterBlock?.let { Regex("(?m)^\\s+plugin:\\s*$").containsMatchIn(it) } == true -> FlutterProjectKind.PLUGIN
            declared == "package" -> FlutterProjectKind.PACKAGE
            declared == "app" || hasRunner -> FlutterProjectKind.APPLICATION
            else -> FlutterProjectKind.PACKAGE
        }
    }

    private fun topLevelBlock(text: String, name: String): String? {
        val lines = text.lines()
        val start = lines.indexOfFirst { it.matches(Regex("^${Regex.escape(name)}:\\s*(?:#.*)?$")) }
        if (start < 0) return null
        return lines.drop(start + 1).takeWhile { it.isBlank() || it.firstOrNull()?.isWhitespace() == true }.joinToString("\n")
    }

    private val IGNORED_DIRECTORIES = setOf(".git", ".dart_tool", ".gradle", ".build", "build", "Pods", "DerivedData")
}
