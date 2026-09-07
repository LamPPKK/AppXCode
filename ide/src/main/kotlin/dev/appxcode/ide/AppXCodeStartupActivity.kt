package dev.appxcode.ide
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.appxcode.ide.device.DeviceRegistryService
import dev.appxcode.ide.device.PhysicalDeviceProvider
import dev.appxcode.ide.device.SimulatorProvider
import dev.appxcode.ide.device.VPhoneProvider
class AppXCodeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.getService(AppXCodeProjectService::class.java).initialize()
        if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
            project.getService(DeviceRegistryService::class.java).register(SimulatorProvider())
            project.getService(DeviceRegistryService::class.java).register(PhysicalDeviceProvider())
            if (System.getProperty("appxcode.vphone.enabled", "false").toBoolean()) {
                val executable = System.getProperty("appxcode.vphone.path", "vphone-cli")
                project.getService(DeviceRegistryService::class.java).register(VPhoneProvider(executable, enabled = true))
            }
        }
    }
}
