package dev.appxcode.ide.test

data class TestSuiteResult(val name: String, val cases: List<TestCaseResult>) {
    val passed: Int get() = cases.count { it.status == TestStatus.PASSED }
    val failed: Int get() = cases.count { it.status == TestStatus.FAILED }
    val skipped: Int get() = cases.count { it.status == TestStatus.SKIPPED }
}

data class TestResultTree(val suites: List<TestSuiteResult>) {
    val total: Int get() = suites.sumOf { it.cases.size }
    val passed: Int get() = suites.sumOf(TestSuiteResult::passed)
    val failed: Int get() = suites.sumOf(TestSuiteResult::failed)
    val skipped: Int get() = suites.sumOf(TestSuiteResult::skipped)

    companion object {
        fun from(cases: List<TestCaseResult>): TestResultTree = TestResultTree(
            cases.groupBy { it.identifier.substringBefore("/").ifBlank { "Tests" } }
                .map { (suite, entries) -> TestSuiteResult(suite, entries) }
                .sortedBy(TestSuiteResult::name)
        )
    }
}
