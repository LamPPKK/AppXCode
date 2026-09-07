package dev.appxcode.ide.test

import dev.appxcode.ide.build.XcodeBuildRequest
import dev.appxcode.ide.build.XcodeBuildService
import java.nio.file.Path
import java.time.Duration

enum class TestStatus { PASSED, FAILED, SKIPPED, UNKNOWN }

data class TestCaseResult(val identifier: String, val status: TestStatus, val durationSeconds: Double? = null)

data class XcodeTestResult(val cases: List<TestCaseResult>, val rawOutput: String) {
    val passed: Boolean get() = cases.isNotEmpty() && cases.all { it.status == TestStatus.PASSED }
    val isComplete: Boolean get() = cases.isNotEmpty() && cases.none { it.status == TestStatus.UNKNOWN }
    val failedCases: List<TestCaseResult> get() = cases.filter { it.status == TestStatus.FAILED }
    val skippedCases: List<TestCaseResult> get() = cases.filter { it.status == TestStatus.SKIPPED }
    val tree: TestResultTree get() = TestResultTree.from(cases)
}

class XcodeTestService(private val builder: XcodeBuildService) {
    fun run(
        container: Path,
        scheme: String,
        destination: String,
        configuration: String = "Debug",
        timeout: Duration = Duration.ofMinutes(20),
    ): XcodeTestResult {
        val result = builder.execute(XcodeBuildRequest(container, scheme, destination, configuration, action = "test"), timeout)
        return XcodeTestResult(parseCases(result.output), result.output)
    }

    fun rerunFailed(
        container: Path,
        scheme: String,
        destination: String,
        previous: XcodeTestResult,
        configuration: String = "Debug",
        timeout: Duration = Duration.ofMinutes(20),
    ): XcodeTestResult {
        val tests = previous.tree.failedCases().map { it.identifier }.distinct()
        if (tests.isEmpty()) return XcodeTestResult(emptyList(), "No failed tests to rerun")
        val args = tests.flatMap { listOf("-only-testing:$it") }
        val result = builder.execute(XcodeBuildRequest(container, scheme, destination, configuration, action = "test", arguments = args), timeout)
        return XcodeTestResult(parseCases(result.output), result.output)
    }

    private fun parseCases(output: String): List<TestCaseResult> = output.lineSequence().mapNotNull { line ->
        val passed = PASSED.matchEntire(line)
        val failed = FAILED.matchEntire(line)
        val skipped = SKIPPED.matchEntire(line)
        val match = passed ?: failed ?: skipped ?: return@mapNotNull null
        TestCaseResult(match.groupValues[1], when { passed != null -> TestStatus.PASSED; failed != null -> TestStatus.FAILED; else -> TestStatus.SKIPPED }, match.groupValues[2].toDoubleOrNull())
    }.toList()

    private companion object {
        val PASSED = Regex("^Test Case '-\\[(.+)\\]' passed \\(([^ ]+) (?:seconds|s)\\)$")
        val FAILED = Regex("^Test Case '-\\[(.+)\\]' failed \\(([^ ]+) (?:seconds|s)\\)$")
        val SKIPPED = Regex("^Test Case '-\\[(.+)\\]' skipped \\(([^ ]+) (?:seconds|s)\\)$")
    }
}
