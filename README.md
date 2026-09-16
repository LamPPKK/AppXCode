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

The project now contains the first executable IntelliJ Platform base: a Gradle
plugin project, AppXCode plugin descriptor, and project-scoped startup service.
Feature implementation follows the staged roadmap below.

Implemented slices currently include native Xcode container/scheme discovery,
Swift symbol indexing and fallback diagnostics, SwiftPM/CocoaPods parsing and
resolve actions, Xcode build/test/archive/signing services, Git status/diff,
Apple Simulator and physical-device adapters, opt-in vphone support, Flutter
project/run detection, Embedded Devices UI, and versioned remote build-agent
contracts. Each slice is committed independently on `main`.

The current development plan is [AppXCode feature development plan](plans/20260907202352-feature-development-plan.md).
It is the source of truth for the phased implementation, acceptance gates, and
known platform constraints. Completed slices are committed independently on
`main`; the implementation is progressing through the plan incrementally.
