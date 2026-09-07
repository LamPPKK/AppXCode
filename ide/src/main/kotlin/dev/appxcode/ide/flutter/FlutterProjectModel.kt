package dev.appxcode.ide.flutter

import java.nio.file.Files
import java.nio.file.Path

data class FlutterProject(
    val root: Path,
    val name: String,
    val hasIosRunner: Boolean,
    val hasMacosRunner: Boolean,
    val entrypoints: List<Path>,
    val hasAndroidRunner: Boolean = false,
)

object FlutterProjectDetector {
    fun detect(root: Path): FlutterProject? {
        val pubspec = root.resolve("pubspec.yaml")
        if (!Files.isRegularFile(pubspec)) return null
        val text = Files.readString(pubspec)
        val name = Regex("(?m)^name:\\s*([^#\\r\\n]+)").find(text)?.groupValues?.get(1)
            ?.trim()?.trim('"', '\'')?.takeIf { it.isNotBlank() } ?: root.fileName.toString()
        val entrypoints = buildList {
            val lib = root.resolve("lib")
            if (Files.isDirectory(lib)) Files.walk(lib).use { stream -> stream.filter { it.fileName.toString().endsWith(".dart") }.forEach(::add) }
            val test = root.resolve("test")
            if (Files.isDirectory(test)) Files.walk(test).use { stream -> stream.filter { it.fileName.toString().endsWith(".dart") }.forEach(::add) }
        }.distinct().sortedBy { it.toString() }
        return FlutterProject(root, name, Files.isDirectory(root.resolve("ios")), Files.isDirectory(root.resolve("macos")), entrypoints, Files.isDirectory(root.resolve("android")))
    }
}
