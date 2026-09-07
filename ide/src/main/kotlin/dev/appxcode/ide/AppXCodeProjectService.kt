package dev.appxcode.ide
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicBoolean
@Service(Service.Level.PROJECT)
class AppXCodeProjectService(private val project: Project) {
    private val initialized = AtomicBoolean(false)
    fun initialize() { initialized.compareAndSet(false, true) }
    fun isInitialized(): Boolean = initialized.get()
    fun projectName(): String = project.name
}
