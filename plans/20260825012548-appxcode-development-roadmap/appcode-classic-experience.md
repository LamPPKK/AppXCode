# AppCode Classic Experience Workstream

## Objective

Make the primary macOS application feel familiar and efficient to former AppCode
users without producing a brittle pixel clone or copying JetBrains branding and
proprietary visual assets.

The default reference is the classic AppCode interface before the optional New UI
introduced in AppCode 2022.3. Phase 0 may select a narrower reference release after
the public reference pack and user workflows have been reviewed.

## Design direction

The product is a precision development workstation:

- Editor-centered rather than dashboard-centered.
- Compact and information-dense without sacrificing legibility.
- Keyboard-first, with complete mouse and trackpad operation.
- Calm, utilitarian, and native to macOS rather than decorative or web-like.
- Configurable: expert users can hide, move, resize, pin, and restore tool windows.
- Progressive: complexity appears when a build, test, debug, inspection, or Git task
  needs it and gets out of the way afterward.

AppXCode should inherit proven IntelliJ interaction conventions where the platform
already provides them. Custom UI is justified only for Apple-specific concepts or
when stable platform components cannot satisfy the approved experience.

## Canonical workspace

### Main frame

- macOS application menu and document/window behavior.
- Compact toolbar with scheme, target/destination, Run, Debug, Stop, and search.
- Optional navigation bar showing project-to-symbol context.
- Central tabbed editor with split groups.
- Tool-window bars on the left, right, and bottom edges.
- Compact status bar for Git branch, indexing, build-agent/toolchain state,
  diagnostics, encoding, line/column, and background tasks.

### Project and navigation

- Project view reflects Xcode projects, workspaces, groups, targets, and generated
  sources.
- Files view reflects the filesystem without pretending every file belongs to an
  Xcode target.
- Project Source, Non-Source, Changed Files, and changelist scopes are directly
  reachable.
- Single-keystroke Project focus, select-open-file, quick file/symbol/action search,
  recent locations, back/forward navigation, and structure navigation.
- Target membership and platform/configuration context are visible without opening
  a separate management application for routine work.

### Editor

- Gutter run/test actions, breakpoints, coverage, change markers, line numbers, and
  folding indicators.
- Error stripe, inspection severity, quick fixes, intentions, inlay hints, parameter
  information, documentation, usages, and refactoring previews.
- Predictable tab behavior, editor splits, breadcrumbs/navigation context, semantic
  selection, distraction-free mode, and restore-on-relaunch.
- Light and dark editor schemes that remain distinct from application chrome and
  meet accessibility contrast requirements.

### Build and run

- Build and Run appear as bottom tool windows instead of modal progress flows.
- Hierarchical tasks and diagnostics support expand/collapse, filtering, source
  navigation, copy, rerun, cancellation, and retained history.
- Console output preserves ANSI meaning safely, supports search/filter, and keeps
  build/run selection stable while editing.
- Scheme and destination changes are fast, visible, and available from keyboard.

### Test runner

- Hierarchical XCTest, Quick, Kiwi, and Catch/Catch2 results share one tree.
- Passing, failed, skipped, running, and unavailable states are distinguishable by
  icon, label, and shape rather than color alone.
- Rerun failed, repeat, filter, sort by duration, failure diff, attachments, history,
  and jump-to-source are first-class actions.
- Horizontal and vertical tool-window placements remain usable.

### Debugger

- Debug opens in the bottom tool window and navigates the editor to the suspended
  source location.
- Threads/frames, variables, watches, console, breakpoints, memory, and stepping are
  organized for rapid keyboard movement and predictable focus.
- Values expand inline in a tree; source, stack, and variable selections stay
  synchronized without disruptive panel replacement.
- Local, simulator, device, and remote build sessions present the same debugger UI
  while exposing capability limitations clearly.

### Git and inspections

- Changes, commit, log, branches, diff, shelf/stash, conflicts, TODO, Problems, and
  inspection results use normal IntelliJ tool-window and editor-diff conventions.
- Generated Xcode/CocoaPods files and source membership are clear in change views.
- Navigation from any finding to source and back preserves context.

## Keyboard and customization

- Ship an AppCode Classic keymap as the default on macOS.
- Offer an Xcode-compatible preset for migrating users without weakening AppCode
  shortcuts.
- Preserve standard action search, customizable menus/toolbars, tool-window number
  shortcuts, hide-all-windows, full screen, presentation, and distraction-free
  modes.
- Every primary workflow must be possible without a pointing device and must expose
  meaningful accessibility names, roles, and focus order.

## Branding and asset policy

- Do not reuse the AppCode name as product branding, JetBrains/AppCode logos,
  proprietary illustrations, or assets that are not licensed for redistribution.
- Create an AppXCode identity and icon family that belongs to this project.
- Reuse IntelliJ Platform components and icons only where their applicable license
  and redistribution terms permit it.
- Reproduce workflow and information architecture, not protected brand identity.
- Record source, license, modification, and attribution requirements for every
  external asset admitted to the repository.

## Delivery integration

### Phase 0

- Capture approved public references and version them by source/release.
- Build the UX parity ledger and canonical task list.
- Decide theme, density, icon, typography, branding, and asset policies.

### Phase 1

- Deliver the main frame, editor shell, tool-window behavior, keymap, themes,
  settings, scaling, and accessibility foundation.

### Phases 2-5

- Add language, project, Build, Run, Test, Debug, Git, inspection, and refactoring
  experiences against the same interaction model.

### Phase 8A

- Close the macOS UX parity ledger and pass visual, accessibility, performance, and
  former-user task testing before the primary release.

### Future Linux and Windows editions

- Reuse the information architecture and action model while adapting operating
  system conventions. Cross-platform limitations must not redefine the macOS UI.

## Validation

### Canonical tasks

Measure whether users can efficiently:

1. Open an Xcode workspace and locate a source symbol.
2. Inspect target membership and switch scheme/destination.
3. Navigate diagnostics and apply or preview a safe fix.
4. Build and trace an error from task tree to source.
5. Run a test from the gutter, inspect failure output, and rerun failed tests.
6. Stop at a breakpoint, move through frames, inspect variables, and evaluate an
   expression.
7. Review and commit a focused Git change without losing editor context.
8. Hide and restore tool windows entirely from the keyboard.

### Acceptance gates

- Approved golden screenshots for light/dark appearance, compact/default density,
  common window sizes, supported display scales, and increased-contrast mode.
- Automated focus-order, keyboard-action, accessibility-role, layout-overflow, and
  screenshot-regression checks.
- Task-based comparison by former AppCode users, measuring completion, error rate,
  navigation count, focus surprises, and qualitative familiarity.
- No critical workflow requires copying a proprietary AppCode asset or using an
  unstable/private IntelliJ UI implementation.
- The IDE remains responsive and readable under indexing, build, test, debug, Git,
  and background-agent activity.

## Risks and mitigations

### Nostalgia overriding usability

Keep the classic workflow baseline, but validate it with real tasks and modern
accessibility expectations. Preserve optional classic density rather than freezing
historical defects.

### Excessive custom Swing UI

Prefer stable IntelliJ Platform components and extension points. Isolate unavoidable
custom components, test every theme/scale, and avoid undocumented implementation
details.

### Visual similarity creating branding confusion

Use an original product identity and asset set. Document references and licenses,
and review public-facing screens before release.

### New features fragmenting the layout

Require every feature to declare its tool-window, editor, popup, settings, and
keyboard placement in the UX parity ledger before implementation.

## Historical public references

- JetBrains, "Customizing the AppCode User Interface" (2020):
  https://blog.jetbrains.com/appcode/2020/09/customizing-the-appcode-user-interface/
- JetBrains, "Navigation in AppCode" (2021):
  https://blog.jetbrains.com/appcode/2021/04/navigation-in-appcode/
- JetBrains, "Tutorial: Debugging in AppCode" (2020):
  https://blog.jetbrains.com/appcode/2020/11/tutorial-debugging/
- JetBrains, "AppCode 2022.3 EAP: A New UI" (2022):
  https://blog.jetbrains.com/appcode/2022/11/appcode-2022-3-eap-fixes-for-swift-completion-and-spm-a-new-ui/

These links identify public reference behavior. Their images and other assets are
not automatically licensed for redistribution in AppXCode.
