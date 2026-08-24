# AppXCode

AppXCode is a planned IntelliJ Platform-based IDE for Apple-platform development.
The macOS application is the primary product and will receive the complete,
production-grade Apple development experience first. Its long-term goal is to
restore the productive parts of AppCode and later extend the editor experience to
Linux and Windows.

The primary macOS experience will follow the familiar pre-2022.3 AppCode Classic
interaction model: a code-centered, information-dense, keyboard-first workspace
with configurable Project, Build, Run, Test, Debug, Git, and inspection tool
windows. AppXCode will create its own branding and distributable visual assets.

On macOS, AppXCode uses the locally installed Xcode toolchain through an internal
build-agent boundary. Future Linux and Windows editions will connect to a macOS
build agent, while a device gateway on the developer's workstation can install,
launch, test, and debug signed builds on locally connected devices.

## Project status

The project is currently in architecture and roadmap definition. No production
code has been implemented yet.

The current development plan is:

- [AppXCode development roadmap](plans/20260825012548-appxcode-development-roadmap/plan.md)
- [AppCode Classic experience workstream](plans/20260825012548-appxcode-development-roadmap/appcode-classic-experience.md)

Implementation should not begin until the Phase 0 decisions and acceptance gates
in the roadmap have been reviewed.
