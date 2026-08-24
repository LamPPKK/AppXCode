# AppXCode

AppXCode is a planned IntelliJ Platform-based IDE for Apple-platform development.
The macOS application is the primary product and will receive the complete,
production-grade Apple development experience first. Its long-term goal is to
restore the productive parts of AppCode and later extend the editor experience to
Linux and Windows.

The primary macOS experience will follow the latest stable IntelliJ Platform UI
and current macOS conventions. AppCode is the feature and productivity reference,
not a visual template. AppXCode will provide a modern, code-centered workspace with
configurable Project, Build, Run, Test, Debug, Git, and inspection tool windows,
plus its own branding and distributable visual assets.

Plugin compatibility is a core product requirement. AppXCode will preserve the
standard IntelliJ plugin model and target broad compatibility with plugins whose
declared IntelliJ build range and required public modules are present. Plugins tied
to proprietary or product-specific JetBrains modules cannot be promised compatible
unless AppXCode legitimately provides and verifies those dependencies.

Flutter is a named, optimized compatibility profile rather than an incidental
Marketplace check. The macOS product will target certified official Dart/Flutter
plugin combinations and integrate Flutter's iOS/macOS workflow with AppXCode's
Xcode, CocoaPods, signing, simulator, device, test, and debugger systems.

AppXCode also plans an `Embedded Devices` tool window that keeps Apple Simulator,
eligible physical-device screens, and an optional experimental `vphone-cli`
destination inside the IDE, with docking and detached-window modes. vPhone runs as
an isolated external provider, is disabled by default, and never authorizes
AppXCode to relax SIP/AMFI or modify firmware implicitly.

On macOS, AppXCode uses the locally installed Xcode toolchain through an internal
build-agent boundary. Future Linux and Windows editions will connect to a macOS
build agent, while a device gateway on the developer's workstation can install,
launch, test, and debug signed builds on locally connected devices.

## Project status

The project is currently in architecture and roadmap definition. No production
code has been implemented yet.

The current development plan is:

- [AppXCode development roadmap](plans/20260825012548-appxcode-development-roadmap/plan.md)
- [Modern AppXCode experience workstream](plans/20260825012548-appxcode-development-roadmap/modern-appxcode-experience.md)
- [IntelliJ plugin compatibility workstream](plans/20260825012548-appxcode-development-roadmap/intellij-plugin-compatibility.md)
- [Flutter optimized profile](plans/20260825012548-appxcode-development-roadmap/flutter-optimized-profile.md)
- [Embedded Devices and vPhone integration](plans/20260825012548-appxcode-development-roadmap/embedded-device-hub.md)

Implementation should not begin until the Phase 0 decisions and acceptance gates
in the roadmap have been reviewed.
