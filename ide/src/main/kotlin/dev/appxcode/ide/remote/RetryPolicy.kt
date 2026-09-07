package dev.appxcode.ide.remote

data class RetryPolicy(val maxAttempts: Int = 3, val backoffMillis: Long = 500) {
    init { require(maxAttempts >= 1); require(backoffMillis >= 0) }
    fun delayFor(attempt: Int): Long {
        val multiplier = attempt.coerceAtLeast(1).toLong()
        return if (backoffMillis > Long.MAX_VALUE / multiplier) Long.MAX_VALUE else backoffMillis * multiplier
    }
}

enum class AgentFailure { NETWORK, TIMEOUT, AUTHENTICATION, VALIDATION, BUILD }

object RetryDecider {
    fun shouldRetry(failure: AgentFailure): Boolean = failure == AgentFailure.NETWORK || failure == AgentFailure.TIMEOUT
    fun shouldRetry(errorCode: String?): Boolean =
        errorCode == BuildAgentErrorCode.TRANSPORT_UNAVAILABLE || errorCode == BuildAgentErrorCode.TIMEOUT
}
