package dev.appxcode.ide
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
class AppXCodeStartupActivity : ProjectActivity { override suspend fun execute(project: Project) { project.getService(AppXCodeProjectService::class.java).initialize() } }
