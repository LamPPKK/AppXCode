package dev.appxcode.ide.remote

data class RetryPolicy(val maxAttempts: Int = 3, val backoffMillis: Long = 500) {
    init { require(maxAttempts >= 1); require(backoffMillis >= 0) }
    fun delayFor(attempt: Int): Long = backoffMillis * attempt.coerceAtLeast(1)
}

enum class AgentFailure { NETWORK, TIMEOUT, AUTHENTICATION, VALIDATION, BUILD }

object RetryDecider {
    fun shouldRetry(failure: AgentFailure): Boolean = failure == AgentFailure.NETWORK || failure == AgentFailure.TIMEOUT
}
