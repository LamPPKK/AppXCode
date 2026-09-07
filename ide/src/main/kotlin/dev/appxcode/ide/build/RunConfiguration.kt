package dev.appxcode.ide.build

data class RunConfiguration(
    val name: String,
    val scheme: String,
    val destination: AppleDestination,
    val configuration: String = "Debug",
    val environment: Map<String, String> = emptyMap(),
    val arguments: List<String> = emptyList(),
)

class RunConfigurationRegistry {
    private val configurations = linkedMapOf<String, RunConfiguration>()
    fun put(configuration: RunConfiguration) { configurations[configuration.name] = configuration }
    fun remove(name: String) { configurations.remove(name) }
    fun get(name: String): RunConfiguration? = configurations[name]
    fun all(): List<RunConfiguration> = configurations.values.toList()
}
