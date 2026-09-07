package dev.appxcode.ide.plugin

data class PluginDescriptor(val id: String, val sinceBuild: String?, val untilBuild: String?, val requiredModules: Set<String> = emptySet()) {
    init {
        require(id.isNotBlank()) { "Plugin id must not be blank" }
        require(sinceBuild == null || sinceBuild.isNotBlank()) { "sinceBuild must not be blank" }
        require(untilBuild == null || untilBuild.isNotBlank()) { "untilBuild must not be blank" }
        require(requiredModules.none { it.isBlank() }) { "Required module ids must not be blank" }
    }
}
data class CompatibilityResult(val compatible: Boolean, val reasons: List<String>)

object PluginCompatibility {
    fun check(plugin: PluginDescriptor, appxcodeBuild: String, availableModules: Set<String>): CompatibilityResult {
        if (appxcodeBuild.isBlank()) return CompatibilityResult(false, listOf("AppXCode build is missing"))
        val reasons = buildList {
            if (!inRange(plugin.sinceBuild, plugin.untilBuild, appxcodeBuild)) add("build $appxcodeBuild is outside plugin range")
            val missing = plugin.requiredModules - availableModules
            if (missing.isNotEmpty()) add("missing modules: ${missing.sorted().joinToString()}")
        }
        return CompatibilityResult(reasons.isEmpty(), reasons)
    }

    private fun inRange(since: String?, until: String?, build: String): Boolean {
        val number = build.takeWhile(Char::isDigit).toIntOrNull() ?: return false
        val min = since?.takeWhile(Char::isDigit)?.toIntOrNull()
        val max = until?.takeWhile(Char::isDigit)?.toIntOrNull()
        return (min == null || number >= min) && (max == null || number <= max)
    }
}
