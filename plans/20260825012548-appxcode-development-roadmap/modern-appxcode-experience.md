# Modern AppXCode Experience Workstream

## Objective

Create a current, production-grade macOS IDE experience based on the latest stable
IntelliJ Platform UI and contemporary macOS conventions. AppCode is the reference
for capabilities, workflow coverage, and developer productivity—not for visual
appearance.

AppXCode does not target pixel parity with any historical AppCode release. The UI
baseline advances with supported IntelliJ Platform versions through an explicit
upgrade and visual-regression policy.

## Design direction

AppXCode is a focused Apple development workstation:

- Modern and visually calm, with progressive disclosure of advanced controls.
- Editor-centered rather than dashboard-centered.
- Native-feeling on macOS while remaining consistent with IntelliJ conventions.
- Keyboard-first, with complete mouse, trackpad, and accessibility operation.
- Spacious by default, with Compact Mode for higher information density.
- Context-aware: Apple-specific controls appear only when the active project,
  scheme, destination, device, build, test, or debug session needs them.
- Customizable without presenting every possible action at once.

The latest stable IntelliJ components, Action System, tool windows, editor APIs,
Kotlin UI DSL, popups, banners, notifications, trees, lists, and status widgets are
the default implementation choices. Custom UI requires a documented Apple-specific
need and an accessibility, theme, scale, and maintenance plan.

## Main workspace

### Main frame

- Native macOS menu integration and correct window, full-screen, focus, keyboard,
  and appearance behavior.
- Simplified, customizable toolbar containing the active scheme, destination,
  Run, Debug, Stop, and global search controls.
- Central editor with tabs, splits, navigation, previews, and restoration.
- Current IntelliJ tool-window stripes on the left and right; vertical and bottom
  placement follows each tool's task and user customization.
- Status/navigation area for Git branch, indexing, diagnostics, toolchain, build
  agent, encoding, caret position, and background tasks.
- Compact Mode reduces chrome, spacing, and icon size without removing capability.

### Welcome and project opening

- Recent projects, clone/open, create project/package, remote Mac pairing, and Xcode
  toolchain readiness are presented without turning the welcome screen into a
  marketing dashboard.
- Missing Xcode, SDK, signing, dependency, or build-agent requirements use actionable
  guidance with direct diagnostics and recovery.
- Opening an existing Xcode project never requires format conversion.

### Project and Apple context

- Project view represents workspaces, projects, groups, targets, packages,
  generated sources, and dependencies.
- Files view represents the real filesystem and clearly distinguishes membership in
  Xcode targets.
- Scheme, configuration, platform, destination, architecture, signing, and build
  agent context remain visible when relevant without consuming permanent editor
  space.
- Quick search and context actions provide direct navigation to files, symbols,
  targets, schemes, settings, devices, tests, and actions.

### Editor

- Gutter actions cover Run, Test, breakpoints, coverage, Git changes, folding, and
  diagnostics without creating icon noise.
- Completion, quick documentation, parameter information, intentions, inspections,
  inlay hints, usages, refactoring previews, and generated interfaces follow current
  IntelliJ presentation patterns.
- SwiftUI previews and other Apple-specific auxiliary views use editor splits or
  tool windows according to task persistence and available space.
- Light and dark editor schemes, increased contrast, font scaling, presentation,
  distraction-free, and screen-reader modes remain first-class.

### Build and run

- Build and Run use current IntelliJ tool-window conventions and stream progress
  without modal blocking.
- Task trees and diagnostics support filtering, grouping, expand/collapse, source
  navigation, copy, rerun, cancellation, and retained history.
- Scheme and destination controls expose recent and compatible choices first, with
  detailed settings progressively disclosed.
- Local and remote builds use the same visible task model; connection and path
  mapping state are clear when remote execution is involved.

### Test runner

- XCTest, Quick, Kiwi, and Catch/Catch2 share one modern test tree and result model.
- Passing, failed, skipped, running, flaky, and unavailable states use icon, shape,
  label, and color rather than color alone.
- Rerun failed, repeat, filter, sort, duration, failure diff, attachments, history,
  coverage, and jump-to-source actions follow current IntelliJ patterns.
- Horizontal, vertical, compact, and detached layouts remain usable.

### Debugger

- Debug sessions use the current IntelliJ debugger layout with source, threads,
  frames, variables, watches, console, breakpoints, memory, and stepping controls.
- Local, simulator, physical-device, and remote build sessions share one interaction
  model while exposing transport-specific limitations and recovery actions.
- Source, stack, variable, and inline-value state remain synchronized without
  unexpected focus or panel replacement.

### Git, problems, and inspections

- Changes, commit, log, branches, diff, shelf/stash, conflicts, TODO, Problems, and
  inspection results use standard current IntelliJ workflows.
- Generated Xcode/CocoaPods artifacts and target membership are clearly represented
  in change and inspection contexts.
- Findings provide direct source navigation, context-preserving return navigation,
  suppression, quick fixes, and scope controls.

## Visual system

- Start from current IntelliJ light and dark themes; customize only what establishes
  AppXCode identity or clarifies Apple-specific semantics.
- Reuse supported platform icons where possible. New icons follow the current
  IntelliJ grid, palette, theme, compact-mode, and HiDPI requirements.
- AppXCode branding is original and avoids JetBrains/AppCode logos or unlicensed
  assets.
- Motion is restrained and functional: progress, transitions, focus, and state
  changes should communicate behavior without delaying expert workflows.
- Text remains concise, localized-ready, and consistent with current IntelliJ UI
  language conventions.

## Keyboard, customization, and accessibility

- The current macOS IntelliJ keymap is the default.
- Offer optional AppCode and Xcode migration presets only where their shortcuts can
  be implemented accurately and maintained.
- Preserve action search, customizable toolbar, rearrangeable tool windows, recent
  locations, full screen, presentation, distraction-free, and zen workflows.
- Every primary action has a meaningful accessible name, role, state, focus order,
  keyboard path, visible focus treatment, and non-color status cue.
- Respect macOS appearance, increased contrast, reduced motion, font scaling, and
  assistive-technology behavior where the platform exposes them.

## Delivery integration

### Phase 0

- Record the current stable IntelliJ UI baseline and upgrade policy.
- Build the Apple-development task inventory and UX quality ledger.
- Decide the branding, icon, theme, density, typography, accessibility, and custom
  component policies.

### Phase 1

- Deliver the macOS application shell, current IntelliJ layout, Action System,
  themes, Compact Mode, keymap, settings, scaling, and accessibility foundation.

### Phases 2-5

- Add language, project, dependency, Build, Run, Test, Debug, Git, inspection, and
  refactoring experiences using current platform patterns.

### Phase 8A

- Close the macOS UX quality ledger and pass visual, accessibility, performance,
  upgrade, and task-based usability gates before the primary release.

### Future Linux and Windows editions

- Use the same information architecture and action model while adapting host OS
  conventions. Cross-platform limitations must not redefine the macOS experience.

## Validation

### Canonical tasks

Users must be able to efficiently:

1. Open an Xcode workspace and locate a source symbol.
2. Inspect target membership and switch scheme/destination.
3. Navigate diagnostics and apply or preview a safe fix.
4. Build and trace an error from task tree to source.
5. Run a test from the gutter, inspect failure output, and rerun failed tests.
6. Stop at a breakpoint, move through frames, inspect variables, and evaluate an
   expression.
7. Review and commit a focused Git change without losing editor context.
8. Diagnose and recover from an unavailable toolchain, agent, simulator, or device.

### Acceptance gates

- Approved golden screenshots for current light/dark themes, default/compact density,
  common window sizes, supported display scales, and increased-contrast mode.
- Automated focus-order, keyboard-action, accessibility-role, layout-overflow,
  icon/theme, and screenshot-regression checks.
- Task-based usability testing measures completion, error rate, navigation count,
  focus surprises, discoverability, and expert efficiency.
- IntelliJ Platform upgrades include before/after visual and interaction review;
  AppXCode does not silently retain obsolete custom UI when the platform provides a
  better supported component.
- The IDE remains responsive and readable under indexing, build, test, debug, Git,
  remote-agent, and device activity.

## Risks and mitigations

### Chasing every platform UI change

Support deliberate IntelliJ release bands. Upgrade on a planned cadence, review
notable UI/API changes, and update golden baselines only after human approval.

### Excessive custom Swing UI

Prefer stable IntelliJ Platform components and extension points. Isolate unavoidable
custom components and test every supported theme, scale, density, and accessibility
mode.

### New features fragmenting the workspace

Require every feature to declare its editor, tool-window, popup, banner, settings,
status, and keyboard placement in the UX quality ledger before implementation.

### A clean UI hiding essential expert controls

Use progressive disclosure, action search, contextual toolbars, customizable
surfaces, shortcuts, and Compact Mode. Validate both discoverability for newcomers
and operation count for experienced developers.

## Living references

- IntelliJ IDEA New UI:
  https://www.jetbrains.com/help/idea/new-ui.html
- IntelliJ Platform UI overview:
  https://plugins.jetbrains.com/docs/intellij/ui-overview.html
- IntelliJ Platform UI components:
  https://plugins.jetbrains.com/docs/intellij/user-interface-components.html
- IntelliJ Platform icon requirements:
  https://plugins.jetbrains.com/docs/intellij/icons.html

These references are living inputs. The supported IntelliJ Platform version and
repository test baselines determine the exact UI contract for each AppXCode release.
