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
            val identities = runCatching { runner(listOf("security", "find-identity", "-v", "-p", "codesigning")) }.getOrDefault("")
            if (identities.contains("0 valid identities")) add("No valid code-signing identity found")
        }
        return SigningCheck(issues.isEmpty(), issues)
    }
}
