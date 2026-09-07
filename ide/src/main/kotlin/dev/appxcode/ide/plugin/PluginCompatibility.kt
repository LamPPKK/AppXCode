package dev.appxcode.ide.plugin

data class PluginDescriptor(val id: String, val sinceBuild: String?, val untilBuild: String?, val requiredModules: Set<String> = emptySet()) {
    init {
        require(id.isNotBlank() && id == id.trim() && !id.any(Char::isWhitespace)) { "Plugin id must be a non-blank identifier" }
        require(sinceBuild == null || sinceBuild.isNotBlank()) { "sinceBuild must not be blank" }
        require(untilBuild == null || untilBuild.isNotBlank()) { "untilBuild must not be blank" }
        require(requiredModules.none { it.isBlank() || it != it.trim() }) { "Required module ids must not be blank or padded" }
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
        val number = parseBuild(build) ?: return false
        val min = since?.let(::parseBuild)
        val max = until?.let(::parseBuild)
        return (min == null || compareBuild(number, min) >= 0) && (max == null || compareBuild(number, max) <= 0)
    }

    private fun compareBuild(left: List<Int>, right: List<Int>): Int {
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val result = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
            if (result != 0) return result
        }
        return 0
    }

    private fun parseBuild(value: String): List<Int>? = value.trim().split('.').takeIf { it.isNotEmpty() }
        ?.map { it.toIntOrNull() ?: return null }
        ?.dropLastWhile { it == 0 }
}
