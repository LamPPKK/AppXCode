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
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class EmbeddedDevicesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val registry = project.getService(DeviceRegistryService::class.java)
        val list = JBList<String>()
        list.name = "Embedded Devices"
        list.toolTipText = "Connected Apple devices and simulators"
        list.visibleRowCount = 12
        val copyId = JButton("Copy ID")
        copyId.name = "Copy selected device ID"
        copyId.isEnabled = false
        list.addListSelectionListener { copyId.isEnabled = list.selectedIndex >= 0 }
        val preferred = JButton("Preferred")
        preferred.name = "Select preferred device"
        preferred.addActionListener {
            registry.preferred()?.let { device ->
                val index = registry.discover().indexOfFirst { it.id == device.id }
                if (index >= 0) list.selectedIndex = index
            }
        }
        val status = JLabel()
        fun render(snapshot: DeviceRegistrySnapshot) {
            list.setListData(snapshot.devices.map { "${it.name} · ${it.platform} · ${it.kind} · ${it.state} · ${it.id}" }.toTypedArray())
            status.text = if (snapshot.totalCount == 0) "No devices discovered" else "Available: ${snapshot.availableCount}/${snapshot.totalCount}" +
                " · Physical: ${snapshot.physicalCount} · Sim: ${snapshot.simulatorCount} · vPhone: ${snapshot.vphoneCount}" +
                if (snapshot.hasProviderErrors) " · Provider errors: ${snapshot.errorCount}" else ""
            status.toolTipText = snapshot.providerErrors.entries.takeIf { it.isNotEmpty() }
                ?.joinToString("<br>", prefix = "<html>", postfix = "</html>") { "${it.key}: ${it.value}" }
            preferred.isEnabled = snapshot.hasAvailable
        }
        fun refresh() { render(registry.snapshot()) }
        val panel = JPanel(BorderLayout())
        val actions = JPanel(BorderLayout())
        actions.add(JButton("Refresh").also {
            it.name = "Refresh embedded devices"
            it.addActionListener { refresh() }
        }, BorderLayout.WEST)
        actions.add(copyId.also { it.addActionListener {
            val selected = registry.discover().getOrNull(list.selectedIndex)
            selected?.id?.let { id -> Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(id), null) }
        } }, BorderLayout.EAST)
        actions.add(preferred, BorderLayout.SOUTH)
        actions.add(status, BorderLayout.CENTER)
        panel.add(actions, BorderLayout.NORTH)
        panel.add(list, BorderLayout.CENTER)
        refresh()
        val content = ContentFactory.getInstance().createContent(panel, "Devices", false)
        val subscription = registry.onSnapshotChanged { snapshot -> SwingUtilities.invokeLater { render(snapshot) } }
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
    fun isClosed(): Boolean = registry.isClosed
    fun hasProvider(providerId: String): Boolean = registry.hasProvider(providerId)
    fun providerErrors(): Map<String, String> = registry.providerErrors()
    fun providerError(providerId: String): String? {
        require(providerId.isNotBlank()) { "Device provider id must not be blank" }
        return registry.providerErrors()[providerId]
    }
    fun refresh() = registry.refresh()
    fun refresh(providerId: String): Boolean = registry.refresh(providerId)
    fun onDevicesChanged(listener: (List<AppleDevice>) -> Unit): AutoCloseable = registry.onDevicesChanged(listener)
    fun onSnapshotChanged(listener: (DeviceRegistrySnapshot) -> Unit): AutoCloseable =
        registry.onSnapshotChanged(listener)
    fun clear() = registry.clear()
    override fun dispose() = registry.close()
}
