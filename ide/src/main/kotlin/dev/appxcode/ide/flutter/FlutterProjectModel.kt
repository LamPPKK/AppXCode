package dev.appxcode.ide.flutter

import java.nio.file.Files
import java.nio.file.Path

data class FlutterProject(val root: Path, val name: String, val hasIosRunner: Boolean, val hasMacosRunner: Boolean, val entrypoints: List<Path>)

object FlutterProjectDetector {
    fun detect(root: Path): FlutterProject? {
        val pubspec = root.resolve("pubspec.yaml")
        if (!Files.isRegularFile(pubspec)) return null
        val text = Files.readString(pubspec)
        val name = Regex("(?m)^name:\\s*([^\\s#]+)").find(text)?.groupValues?.get(1) ?: root.fileName.toString()
        val entrypoints = listOf(root.resolve("lib/main.dart"), root.resolve("test")).filter(Files::exists)
        return FlutterProject(root, name, Files.isDirectory(root.resolve("ios")), Files.isDirectory(root.resolve("macos")), entrypoints)
    }
}
