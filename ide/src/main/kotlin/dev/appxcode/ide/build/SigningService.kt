package dev.appxcode.ide.build

import java.nio.file.Path

data class SigningConfiguration(val teamId: String?, val bundleId: String?, val provisioningProfile: Path? = null)
data class SigningCheck(val ready: Boolean, val issues: List<String>)

class SigningService(private val runner: (List<String>) -> String = { args ->
    ProcessBuilder(args).redirectErrorStream(true).start().inputStream.bufferedReader().readText()
}) {
    fun check(configuration: SigningConfiguration): SigningCheck {
        val issues = buildList {
            if (configuration.teamId.isNullOrBlank()) add("Apple development team is not configured")
            if (configuration.bundleId.isNullOrBlank()) add("Bundle identifier is not configured")
            configuration.provisioningProfile?.let { if (!it.toFile().isFile) add("Provisioning profile not found: $it") }
            val identities = runCatching { runner(listOf("security", "find-identity", "-v", "-p", "codesigning")) }
                .getOrElse { add("Unable to inspect code-signing identities: ${it.message ?: "security unavailable"}"); "" }
            if (identities.contains("0 valid identities") || identities.contains("valid identities found", ignoreCase = true) && identities.startsWith("0")) add("No valid code-signing identity found")
        }
        return SigningCheck(issues.isEmpty(), issues)
    }
}
