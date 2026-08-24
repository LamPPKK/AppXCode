# Phase 7 — Cross-Platform Physical Devices

## Objective

Allow a supported physical Apple device connected to a Linux or Windows workstation
to participate in run, test, logging, and debugging sessions whose artifact was
built and signed on a macOS build node.

## Workstreams

- Implement the replaceable device-backend contract for discovery, pairing, trust,
  installation, launch, process control, logs, crash reports, tunnels, and debug
  services.
- Complete legal, security, and maintenance review before selecting or distributing
  any third-party device implementation.
- Transfer signed artifacts and required symbols safely from build agent to the
  correct client-side gateway.
- Coordinate build-agent, client, and gateway session state, cancellation, device
  removal, locking, Developer Mode, and trust prompts.
- Bridge LLDB/debug-server traffic and test-control traffic while preserving source
  and symbol mapping from the remote build.
- Define versioned capability probes rather than assuming features from OS version.
- Treat iPhone/iPad as the first production gateway targets; assess tvOS network
  devices and watchOS-through-paired-iPhone separately.
- Provide a supported fallback in which the physical device connects to the Mac
  build node when the local gateway cannot satisfy a capability.
- Route eligible physical-device screen frames and input capabilities through the
  provider-neutral Embedded Devices session. Do not reuse the vPhone adapter or
  imply that display access proves install, test, or debugger support.

## Validation

- Maintain a real-device compatibility lab across supported host and device OS
  bands.
- Prove install, launch, logs, crash collection, XCTest-family execution, breakpoint
  debugging, disconnect recovery, and cleanup from both Linux and Windows.
- Test multiple devices, device replacement mid-session, trust revocation, locked
  devices, Developer Mode disabled, network changes, and unsupported OS updates.
- Publish capability limitations instead of marking a platform fully supported from
  installation success alone.

## Exit gate

The declared Windows/Linux device matrix passes automated and manual compatibility
gates, failures degrade safely, and users can fall back to a Mac-connected device
without changing projects.
