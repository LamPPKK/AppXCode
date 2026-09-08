package dev.appxcode.ide.device

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.Disposable
import javax.swing.SwingUtilities
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JLabel

class EmbeddedDevicesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val registry = project.getService(DeviceRegistryService::class.java)
        val list = JBList<String>()
        list.name = "Embedded Devices"
        list.toolTipText = "Connected Apple devices and simulators"
        val status = JLabel()
        fun render(devices: List<AppleDevice>) {
            list.setListData(devices.map { "${it.name} · ${it.platform} · ${it.kind} · ${it.state} · ${it.id}" }.toTypedArray())
            val snapshot = registry.snapshot()
            status.text = "Available: ${snapshot.availableCount}/${snapshot.totalCount}" +
                if (snapshot.hasProviderErrors) " · Provider errors: ${snapshot.errorCount}" else ""
            status.toolTipText = snapshot.providerErrors.entries
                .joinToString("<br>", prefix = "<html>", postfix = "</html>") { "${it.key}: ${it.value}" }
        }
        fun refresh() { render(registry.discover()) }
        val panel = JPanel(BorderLayout())
        val actions = JPanel(BorderLayout())
        actions.add(JButton("Refresh").also { it.addActionListener { refresh() } }, BorderLayout.WEST)
        actions.add(status, BorderLayout.CENTER)
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
class DeviceRegistryService : Disposable {
    private val registry = DeviceRegistry()
    internal fun sharedRegistry(): DeviceRegistry = registry
    fun register(provider: DeviceProvider) = registry.register(provider)
    fun unregister(providerId: String) = registry.unregister(providerId)
    fun discover(): List<AppleDevice> = registry.discover()
    fun snapshot(): DeviceRegistrySnapshot = registry.snapshot()
    fun availableDevices(): List<AppleDevice> = registry.snapshot().availableDevices
    fun hasAvailableDevices(): Boolean = registry.snapshot().hasAvailable
    fun availableDevicesByKind(): Map<DeviceKind, List<AppleDevice>> = registry.snapshot().availableDevicesByKind
    fun offlineDevices(): List<AppleDevice> = registry.snapshot().offlineDevices
    fun unknownDevices(): List<AppleDevice> = registry.snapshot().unknownDevices
    fun find(deviceId: String): AppleDevice? = registry.find(deviceId)
    fun preferred(): AppleDevice? = registry.preferred()
    fun preferredDevice(): AppleDevice? = registry.snapshot().preferredDevice()
    fun select(deviceId: String? = null): AppleDevice? = registry.select(deviceId)
    fun providerIds(): List<String> = registry.providerIds()
    fun hasProvider(providerId: String): Boolean = registry.hasProvider(providerId)
    fun providerErrors(): Map<String, String> = registry.providerErrors()
    fun refresh() = registry.refresh()
    fun refresh(providerId: String): Boolean = registry.refresh(providerId)
    fun onDevicesChanged(listener: (List<AppleDevice>) -> Unit): AutoCloseable = registry.onDevicesChanged(listener)
    fun clear() = registry.clear()
    override fun dispose() = registry.clear()
}
