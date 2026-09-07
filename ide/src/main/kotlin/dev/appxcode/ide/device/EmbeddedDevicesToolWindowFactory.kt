package dev.appxcode.ide.device

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.util.Disposer
import javax.swing.SwingUtilities
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class EmbeddedDevicesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val registry = project.getService(DeviceRegistryService::class.java)
        val list = JBList<String>()
        fun render(devices: List<AppleDevice>) { list.setListData(devices.map { "${it.name} · ${it.platform} · ${it.kind} · ${it.state}" }.toTypedArray()) }
        fun refresh() { render(registry.discover()) }
        val panel = JPanel(BorderLayout())
        val actions = JPanel(BorderLayout())
        actions.add(JButton("Refresh").also { it.addActionListener { refresh() } }, BorderLayout.WEST)
        panel.add(actions, BorderLayout.NORTH)
        panel.add(list, BorderLayout.CENTER)
        refresh()
        val content = ContentFactory.getInstance().createContent(panel, "Devices", false)
        val subscription = registry.onDevicesChanged { devices -> SwingUtilities.invokeLater { render(devices) } }
        Disposer.register(content) { subscription.close() }
        toolWindow.contentManager.addContent(content)
    }
}

@com.intellij.openapi.components.Service(com.intellij.openapi.components.Service.Level.PROJECT)
class DeviceRegistryService {
    private val registry = DeviceRegistry()
    fun register(provider: DeviceProvider) = registry.register(provider)
    fun unregister(providerId: String) = registry.unregister(providerId)
    fun discover(): List<AppleDevice> = registry.discover()
    fun refresh() = registry.refresh()
    fun onDevicesChanged(listener: (List<AppleDevice>) -> Unit): AutoCloseable = registry.onDevicesChanged(listener)
    fun clear() = registry.clear()
}
