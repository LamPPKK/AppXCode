# Embedded Devices and vPhone Integration

Status: Proposed cross-cutting workstream

## Goal

AppXCode will display and control Apple simulators, physical-device screens, and
supported virtual iPhones inside an `Embedded Devices` tool window. The target
experience is the integrated Running Devices workflow in Android Studio: a run
destination opens next to the editor, remains interactive during build/debug, can
be detached when more space is needed, and does not force users to manage a
separate window.

Apple's current Xcode Device Hub is the product-behavior reference for simulator
and physical-device interactions. AppXCode must not copy Xcode assets or depend on
undocumented Xcode UI internals. The UI belongs to AppXCode and consumes versioned
device-provider capabilities.

The initial provider set is:

1. **Apple Simulator:** the production-default provider on macOS, implemented with
   supported Apple command-line tools and public APIs.
2. **Mac-connected physical device:** screen viewing and interaction only where a
   supported Apple interface or an approved gateway capability exists.
3. **vPhone:** an explicit experimental provider for
   [`Lakr233/vphone-cli`](https://github.com/Lakr233/vphone-cli), isolated from the
   IDE process and unavailable unless the user configures a compatible host.
4. **Remote Mac:** transports the selected Mac-side provider's video, input, and
   lifecycle capabilities to a future Linux or Windows AppXCode client.

The provider label and security state must always remain visible. A vPhone is not
presented as an Apple Simulator or a physical device.

## Why vPhone is a separate experimental provider

`vphone-cli` is MIT-licensed and exposes useful integration surfaces: JSON-based VM
inventory, a per-VM control socket for screenshots and input, and VNC for viewing a
running guest. Its current host prerequisites are materially different from normal
AppXCode use: Apple Silicon, macOS 15 or later, Xcode and the iOS SDK, private PV=3
entitlements, and SIP/AMFI relaxation. Firmware preparation can apply security
bypasses, including jailbreak and experimental variants.

Therefore:

- AppXCode never changes SIP, AMFI, NVRAM, Recovery settings, or host security
  policy automatically.
- The provider is disabled by default and requires an explicit, informed opt-in.
- The user installs and maintains `vphone-cli`; AppXCode does not silently download
  it or firmware.
- AppXCode does not redistribute IPSWs, CloudOS images, patched firmware, Apple SDK
  content, or vPhone-generated VM bundles.
- Bundling or redistributing `vphone-cli` and its dependencies requires a separate
  legal, security, provenance, and update-policy review even though its repository
  uses the MIT license.
- `less`, `regular`, `dev`, `jb`, and `exp` variants are reported exactly. AppXCode
  never upgrades a VM to a less secure variant as a side effect of Run or Debug.
- A security warning remains visible for hosts with relaxed protection; telemetry
  must not collect firmware, guest credentials, VM content, or control traffic.

The provider is a research/development convenience, not a supported path for App
Store release validation. Final behavior, performance, security, and signing must
still be verified on Apple-supported simulators and physical devices.

## User experience

### Embedded Devices tool window

The standard tool window supports:

- destination picker grouped by provider, device family, OS version, and state;
- one tab per active device, with clear simulator, physical, vPhone, and remote
  badges;
- fit, actual-size, zoom, rotate, bezel visibility, screenshot, record, mute, and
  reconnect controls where the provider advertises them;
- pointer/touch, drag, keyboard, clipboard, Home/Side/Action keys, and provider-
  specific gestures through a normalized input model;
- a compact run-session view and an expanded device-management view;
- docking beside or below the editor, Move to Editor, detach to a standalone
  window, full screen, and restoration of the previous layout;
- correlated Run, Debug, Test, Logs, Inspector, and device-status navigation;
- clear capture, microphone, clipboard, trust, developer-mode, and network-stream
  indicators;
- accessibility names, complete keyboard navigation, scalable controls, light/dark
  themes, Compact Mode, and HiDPI rendering.

Unavailable actions are hidden or disabled with an exact capability explanation.
For example, a provider that can install and launch an app but cannot expose a
debug server must not display a working-looking Debug action.

### Destination management

The expanded view can create, clone, import, export, configure, boot, stop, and
delete a destination only when its provider offers the operation. Destructive
operations require target-specific confirmation and must never run implicitly from
a project build.

For vPhone, the first AppXCode milestone manages already-created VMs. VM creation,
firmware download/preparation, restore, patch selection, import/export, and deletion
remain delegated to `vphone-cli` until their security, progress, cancellation,
storage, and recovery semantics pass dedicated acceptance gates.

## Architecture

### Provider contract

Add `platform/device-api` as the UI-facing model and keep provider implementations
outside it. The contract includes:

- stable provider, host, destination, runtime, and session identities;
- inventory and lifecycle state streams;
- capability negotiation for display, input, clipboard, screenshot, recording,
  application install/launch/stop, logs, tests, debugging, and file transfer;
- frame metadata, pixel size, scale, orientation, color space, timestamps, and
  back-pressure;
- normalized pointer/touch/key events and provider-specific actions;
- cancellation, progress, reconnect, health, diagnostics, and failure categories;
- local/remote routing and protocol-version compatibility.

Provider capabilities are session-scoped, not inferred solely from a provider name,
host OS, firmware version, or VM variant. They can change after boot, reconnect,
lock, trust, or toolchain updates.

### IDE and helper-process boundary

No Apple private framework, vPhone binary, firmware patcher, VNC native library, or
device protocol implementation loads into the IntelliJ process. AppXCode talks to
an owned helper through the versioned local agent protocol. The helper launches
only user-configured, version-checked executables with explicit arguments, bounded
resources, redacted logs, and a controlled environment.

This boundary protects IDE stability, makes the future remote Mac path natural, and
allows each provider to be disabled independently after a compatibility or security
incident.

### vPhone adapter

The experimental adapter will:

1. discover the configured `vphone-cli` binary and verify its version, executable
   identity, host prerequisites, VM data root, and protocol compatibility;
2. inventory VMs through machine-readable CLI output and map them to AppXCode
   destinations without parsing human-formatted terminal text;
3. launch and monitor a selected VM as a supervised external process without
   modifying firmware or host protection settings;
4. connect only to the selected VM's local control socket and local VNC endpoint;
5. use the control socket for supported screenshots, touch, swipe, hardware-key,
   and clipboard operations, with VNC providing the initial continuous display
   transport;
6. expose install, launch, logs, tests, and debug only after an executable spike
   proves each operation for the exact vPhone, firmware, variant, Xcode, and AppXCode
   version tuple;
7. close sessions and release input/capture resources without deleting or mutating
   the VM.

The adapter must prefer documented, versioned vPhone interfaces. If a required
interface is not stable enough, AppXCode reports the capability as unsupported
instead of scraping windows, injecting into the vPhone process, or loading its
private APIs.

### Rendering and input

The video canvas decodes off the IntelliJ UI thread, retains at most a bounded
number of frames, preserves aspect ratio, and maps input using the source frame's
size, orientation, scale, and visible viewport. Resize and rotation updates are
atomic with input transforms so a click cannot target stale coordinates.

VNC is the vPhone bootstrap transport, not the universal AppXCode protocol. The
helper terminates the provider-specific connection and sends a versioned,
authenticated frame/input stream to the IDE. Phase 0 will compare raw-frame,
hardware-assisted video, and damage-region transports before fixing local and
remote formats.

Multi-touch emulation, pressure, Pencil, biometric events, GPS, camera, microphone,
push notifications, and sensor simulation are separate capabilities. UI controls
must not imply support before the active provider proves them.

### Run, test, and debug routing

`Embedded Devices` never owns the build graph. It binds a device session to the
existing destination and run-session models:

1. the build agent resolves the scheme/configuration and builds for the selected
   destination class;
2. signing and artifact compatibility are checked before transfer;
3. the selected provider installs and launches only if it advertises the exact
   capability;
4. test and debugger transports remain owned by the Apple Test and Apple Debug
   services, correlated through the same session identity;
5. the tool window shows the app while the standard Run/Test/Debug windows keep
   authoritative logs, test results, breakpoints, and diagnostics.

vPhone is its own destination class. AppXCode must not assume that an artifact built
for Apple Simulator or a physical iPhone is installable or debuggable on every
vPhone firmware/variant. A Phase 0 spike defines the supported build and signing
contract before Run, Test, or Debug is promised.

## Remote Linux and Windows path

vPhone continues to execute only on a compatible Mac. A Linux or Windows client
can later display and control it through the Mac build agent; it does not run
`vphone-cli` locally. The remote device stream must provide:

- authenticated authorization separate from build permission;
- adaptive resolution/frame rate and explicit latency/quality state;
- bounded bandwidth, back-pressure, reconnect, key-frame recovery, and session
  revocation;
- end-to-end mapping between client input, Mac-side device session, logs, tests,
  debugger, and source revision;
- an unmistakable streaming/capture indicator and one-click disconnect.

The same remote contract should carry Apple Simulator and approved physical-device
screen providers so the cross-platform client does not contain vPhone-specific UI.

## Delivery plan

### Phase 0 — feasibility and policy

- Verify public Apple Simulator/Device Hub integration boundaries; do not treat
  Xcode's embedded UI as a reusable public API without evidence.
- Pin a `vphone-cli` release and exercise VM inventory, launch, VNC, control socket,
  screenshots, touch, keys, clipboard, shutdown, failure, and recovery.
- Measure frame rate, end-to-end input latency, CPU/GPU, memory, HiDPI behavior,
  rotation, resizing, focus, and reconnect.
- Prove or reject app build/install/launch, logs, XCTest-family execution, LLDB
  attach, and Flutter hot reload/debug for each candidate vPhone variant.
- Complete the host-security, firmware, Apple-license, MIT dependency, supply-chain,
  update, crash-isolation, and support review.
- Write ADRs for the provider contract, display transport, remote stream, vPhone
  distribution model, and experimental-support policy.

### Phase 1 — tool-window foundation

- Build the provider-neutral destination/session model and Embedded Devices shell
  against a fake provider with deterministic frames and input acknowledgements.
- Establish focus, docking, detach, accessibility, scaling, theme, state restoration,
  privacy indicators, and failure UX.

### Phase 4 — local macOS providers

- Ship the production Apple Simulator path first.
- Enable local physical-device viewing only for verified capabilities.
- Release the vPhone adapter as an optional experimental feature only if all Phase
  0 safety gates pass; otherwise keep it in an external preview channel.
- Integrate eligible Flutter Run/Debug/Profile and integration-test sessions with
  the same device canvas and hot-reload lifecycle.

### Phases 6-7 — remote transport and gateways

- Stream Mac-hosted Simulator/vPhone sessions into Linux and Windows clients.
- Add physical-device screen transport to the cross-platform gateway only after
  its separate compatibility and security gates pass.

### Phase 8 — release hardening

- Publish exact provider/version/capability matrices and known limitations.
- Pass interaction, performance, accessibility, crash/reconnect, security, and
  release-channel gates before any provider loses its experimental label.

## Validation matrix

At minimum, test:

- supported macOS, Mac model, display scale, AppXCode, Xcode, provider, provider
  version, firmware/device OS, destination type, and security-state combinations;
- idle, booting, running, locked, disconnected, crashed, incompatible, upgrading,
  and low-disk states;
- portrait/landscape, bezel on/off, resize during input, multiple windows, multiple
  destinations, and IDE restart/session restoration;
- tap, hold, drag, swipe, scroll, hardware keys, keyboard/IME, clipboard, screenshot,
  recording, and every advertised extended capability;
- build/install/launch/stop/log/test/debug/Flutter actions separately, with negative
  tests for unsupported combinations;
- local, high-latency, low-bandwidth, packet-loss, reconnect, revoked-session, and
  client/agent version-skew paths;
- malicious or malformed frames, dimensions, input acknowledgements, CLI output,
  control-socket messages, and VM metadata.

Performance budgets are set from Phase 0 evidence. The initial release must define
and enforce measurable maximum input latency, dropped-frame rate, sustained helper
CPU/GPU/memory, startup-to-first-frame time, and reconnect time for each supported
transport rather than claiming “real time” without a testable threshold.

## Exit criteria

The workstream is complete only when:

- the Apple Simulator embedded path meets production quality on the supported
  macOS/Xcode matrix;
- each additional provider exposes only capabilities backed by passing evidence;
- vPhone cannot weaken host security, modify firmware, or mutate VMs without an
  explicit user action outside normal Run/Debug;
- local and remote sessions recover cleanly without freezing or crashing the IDE;
- the tool window is accessible, responsive, and consistent with current IntelliJ
  Platform behavior;
- security, licensing, redistribution, firmware handling, and provider support
  policies are published; and
- users can always choose a supported separate-window or Mac-connected fallback
  when embedding is unavailable.

## Primary references

- Apple Device Hub documentation:
  https://developer.apple.com/documentation/xcode/device-hub
- Apple Device Hub interaction documentation:
  https://developer.apple.com/documentation/xcode/interacting-with-your-app-in-the-ios-or-ipados-simulator
- Android Studio embedded emulator behavior:
  https://developer.android.com/studio/run/emulator
- `vphone-cli` repository and current integration surfaces:
  https://github.com/Lakr233/vphone-cli

These are living inputs. Phase 0 records exact versions and dates so later changes
do not silently alter AppXCode's security or compatibility promise.
