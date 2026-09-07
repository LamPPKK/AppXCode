package dev.appxcode.ide.test

import dev.appxcode.ide.build.XcodeBuildRequest
import dev.appxcode.ide.build.XcodeBuildService
import java.nio.file.Path
import java.time.Duration

enum class TestStatus { PASSED, FAILED, SKIPPED, UNKNOWN }

data class TestCaseResult(val identifier: String, val status: TestStatus, val durationSeconds: Double? = null)

data class XcodeTestResult(val cases: List<TestCaseResult>, val rawOutput: String) {
    val passed: Boolean get() = cases.none { it.status == TestStatus.FAILED }
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

    private fun parseCases(output: String): List<TestCaseResult> = output.lineSequence().mapNotNull { line ->
        val passed = PASSED.matchEntire(line)
        val failed = FAILED.matchEntire(line)
        val match = passed ?: failed ?: return@mapNotNull null
        TestCaseResult(match.groupValues[1], if (passed != null) TestStatus.PASSED else TestStatus.FAILED, match.groupValues[2].toDoubleOrNull())
    }.toList()

    private companion object {
        val PASSED = Regex("^Test Case '-\\[(.+)\\]' passed \\(([^ ]+) seconds\\)$")
        val FAILED = Regex("^Test Case '-\\[(.+)\\]' failed \\(([^ ]+) seconds\\)$")
    }
}
