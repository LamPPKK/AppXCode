package dev.appxcode.ide.test

data class TestSuiteResult(val name: String, val cases: List<TestCaseResult>) {
    val passed: Int get() = cases.count { it.status == TestStatus.PASSED }
    val failed: Int get() = cases.count { it.status == TestStatus.FAILED }
    val skipped: Int get() = cases.count { it.status == TestStatus.SKIPPED }
    val hasSkipped: Boolean get() = skipped > 0
    val hasUnknown: Boolean get() = cases.any { it.status == TestStatus.UNKNOWN }
    val durationSeconds: Double get() = cases.mapNotNull { it.durationSeconds }.sum()
    val isSuccessful: Boolean get() = cases.isNotEmpty() && failed == 0 && cases.none { it.status == TestStatus.UNKNOWN }
}

data class TestResultTree(val suites: List<TestSuiteResult>) {
    val total: Int get() = suites.sumOf { it.cases.size }
    val passed: Int get() = suites.sumOf(TestSuiteResult::passed)
    val failed: Int get() = suites.sumOf(TestSuiteResult::failed)
    val skipped: Int get() = suites.sumOf(TestSuiteResult::skipped)
    val hasSkipped: Boolean get() = skipped > 0
    val hasUnknown: Boolean get() = suites.any { suite -> suite.hasUnknown }
    val hasFailures: Boolean get() = failed > 0
    val isSuccessful: Boolean get() = total > 0 && failed == 0 && suites.flatMap { it.cases }.none { it.status == TestStatus.UNKNOWN }
    fun failedCases(): List<TestCaseResult> = suites.flatMap { suite -> suite.cases.filter { it.status == TestStatus.FAILED } }

    companion object {
        fun from(cases: List<TestCaseResult>): TestResultTree = TestResultTree(
            cases.asReversed().distinctBy(TestCaseResult::identifier).asReversed()
                .groupBy { it.identifier.substringBefore("/").ifBlank { "Tests" } }
                .map { (suite, entries) -> TestSuiteResult(suite, entries) }
                .sortedBy(TestSuiteResult::name)
        )
    }
}
