plugins {
    id("org.jetbrains.intellij.platform")
    kotlin("jvm")
}
group = "dev.appxcode"
version = "0.1.0-SNAPSHOT"
kotlin { jvmToolchain(21) }
repositories { mavenCentral(); intellijPlatform { defaultRepositories() } }
dependencies { intellijPlatform { intellijIdeaCommunity("2025.1") } }
intellijPlatform { pluginConfiguration { ideaVersion { sinceBuild = "251" }; name = "AppXCode" } }
