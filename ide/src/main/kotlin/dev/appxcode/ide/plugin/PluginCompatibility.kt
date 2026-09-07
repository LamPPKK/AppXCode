package dev.appxcode.ide.plugin

data class PluginDescriptor(val id: String, val sinceBuild: String?, val untilBuild: String?, val requiredModules: Set<String> = emptySet())
data class CompatibilityResult(val compatible: Boolean, val reasons: List<String>)

object PluginCompatibility {
    fun check(plugin: PluginDescriptor, appxcodeBuild: String, availableModules: Set<String>): CompatibilityResult {
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
