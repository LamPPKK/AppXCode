package dev.appxcode.ide.test

import java.nio.file.Files
import java.nio.file.Path

enum class TestFramework { XCTEST, QUICK, KIWI, CATCH }

object TestFrameworkRegistry {
    fun detect(root: Path): Set<TestFramework> {
        val files = if (!Files.isDirectory(root)) emptySequence() else Files.walk(root).use { it.filter(Files::isRegularFile).toList().asSequence() }
        val text = files.filter { it.toString().endsWith(".swift") || it.toString().endsWith(".m") || it.toString().endsWith(".mm") }
            .map { runCatching { Files.readString(it) }.getOrDefault("") }.joinToString("\n")
        return buildSet {
            if (text.contains("import XCTest") || text.contains("XCTestCase")) add(TestFramework.XCTEST)
            if (text.contains("import Quick") || text.contains("QuickSpec")) add(TestFramework.QUICK)
            if (text.contains("import Kiwi") || text.contains("describe(")) add(TestFramework.KIWI)
            if (text.contains("Catch2") || text.contains("CATCH_CONFIG_MAIN")) add(TestFramework.CATCH)
        }
    }
}
