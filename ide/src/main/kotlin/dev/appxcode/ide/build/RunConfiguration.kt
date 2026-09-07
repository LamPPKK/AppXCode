package dev.appxcode.ide.build

data class RunConfiguration(
    val name: String,
    val scheme: String,
    val destination: AppleDestination,
    val configuration: String = "Debug",
    val environment: Map<String, String> = emptyMap(),
    val arguments: List<String> = emptyList(),
) {
    init {
        require(name.isNotBlank()) { "Run configuration name must not be blank" }
        require(scheme.isNotBlank()) { "Run configuration scheme must not be blank" }
        require(configuration.isNotBlank()) { "Run configuration build configuration must not be blank" }
    }
}

class RunConfigurationRegistry {
    private val configurations = linkedMapOf<String, RunConfiguration>()
    @Synchronized
    fun put(configuration: RunConfiguration) { configurations[configuration.name] = configuration }

    @Synchronized
    fun remove(name: String) { configurations.remove(name) }

    @Synchronized
    fun get(name: String): RunConfiguration? = configurations[name]

    @Synchronized
    fun all(): List<RunConfiguration> = configurations.values.toList()
}
