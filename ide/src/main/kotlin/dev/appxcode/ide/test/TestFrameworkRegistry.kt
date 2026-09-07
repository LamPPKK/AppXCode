package dev.appxcode.ide.test

import java.nio.file.Files
import java.nio.file.Path

enum class TestFramework { XCTEST, QUICK, KIWI, CATCH }

data class DiscoveredTest(val name: String, val file: Path, val line: Int, val framework: TestFramework)
data class TestFrameworkCommand(val framework: TestFramework, val executable: String, val arguments: List<String>)

object TestFrameworkRegistry {
    fun command(framework: TestFramework, filter: String? = null): TestFrameworkCommand = when (framework) {
        TestFramework.XCTEST -> TestFrameworkCommand(framework, "xcodebuild", buildList { add("test"); filter?.let { add("-only-testing:$it") } })
        TestFramework.QUICK, TestFramework.KIWI -> TestFrameworkCommand(framework, "xcodebuild", buildList { add("test"); filter?.let { add("-only-testing:$it") } })
        TestFramework.CATCH -> TestFrameworkCommand(framework, "ctest", buildList { filter?.let { add("-R"); add(it) } })
    }

    fun discover(root: Path): List<DiscoveredTest> =
        discoverXCTest(root) + discoverQuick(root) + discoverKiwi(root) + discoverCatch(root)

    fun discoverXCTest(root: Path): List<DiscoveredTest> {
        if (!Files.isDirectory(root)) return emptyList()
        val result = mutableListOf<DiscoveredTest>()
        Files.walk(root).use { files -> files.filter { it.toString().endsWith(".swift") || it.toString().endsWith(".m") || it.toString().endsWith(".mm") }.forEach { file ->
            Files.readAllLines(file).forEachIndexed { index, line ->
                Regex("\\bfunc\\s+(test[A-Za-z0-9_]*)\\s*\\(").find(line)?.let { result += DiscoveredTest(it.groupValues[1], file, index + 1, TestFramework.XCTEST) }
                Regex("[-+]\\s*\\(void\\)\\s*(test[A-Za-z0-9_]*)\\s*\\{").find(line)?.let { result += DiscoveredTest(it.groupValues[1], file, index + 1, TestFramework.XCTEST) }
            }
        } }
        return result
    }

    fun discoverQuick(root: Path): List<DiscoveredTest> = discoverLines(root, TestFramework.QUICK) {
        Regex("\\b(?:it|fit|context|describe)\\s*\\(\\s*[\\\"']([^\\\"']+)")
    }

    fun discoverKiwi(root: Path): List<DiscoveredTest> = discoverLines(root, TestFramework.KIWI) {
        Regex("\\b(?:it|specify|context|describe)\\s*\\(\\s*[\\\"']([^\\\"']+)")
    }

    fun discoverCatch(root: Path): List<DiscoveredTest> = discoverLines(root, TestFramework.CATCH) {
        Regex("\\bTEST_CASE\\s*\\(\\s*[\\\"']([^\\\"']+)")
    }

    private fun discoverLines(root: Path, framework: TestFramework, pattern: () -> Regex): List<DiscoveredTest> {
        if (!Files.isDirectory(root)) return emptyList()
        val regex = pattern()
        return buildList {
            Files.walk(root).use { files -> files.filter(Files::isRegularFile).forEach { file ->
                if (file.toString().endsWith(".swift") || file.toString().endsWith(".m") || file.toString().endsWith(".mm") || file.toString().endsWith(".cpp"))
                    runCatching { Files.readAllLines(file) }.getOrDefault(emptyList()).forEachIndexed { index, line ->
                        regex.find(line)?.let { add(DiscoveredTest(it.groupValues[1], file, index + 1, framework)) }
                    }
            } }
        }
    }

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
