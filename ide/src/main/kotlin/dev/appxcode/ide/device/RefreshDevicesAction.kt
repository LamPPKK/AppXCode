package dev.appxcode.ide.device

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RefreshDevicesAction : AnAction("Refresh Embedded Devices") {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.getService(DeviceRegistryService::class.java)?.refresh()
    }
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }
}
