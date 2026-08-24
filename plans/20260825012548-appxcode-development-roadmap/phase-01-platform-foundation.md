# Phase 1 — Platform Foundation

## Objective

Establish the primary standalone macOS IntelliJ application whose Apple-specific
work is performed through service contracts that support local execution first and
remote execution later.

## Workstreams

- Bootstrap the Gradle/IntelliJ build, standalone product metadata, bundled plugins,
  runtime, packaging, update channel, and CI verification.
- Implement the current IntelliJ New UI shell with native macOS menu integration,
  simplified customizable toolbar, scheme/destination controls, editor-centered
  layout, current tool-window stripes, status/navigation area, light/dark themes,
  optional Compact Mode, and keyboard-first focus behavior.
- Establish owned design tokens and branding while using stable IntelliJ Platform
  components for accessibility, scaling, themes, and customization.
- Establish client, protocol, build-agent, and device-gateway module boundaries.
- Implement capability discovery, task lifecycle, progress, cancellation, structured
  diagnostics, logging, and version compatibility semantics.
- Define source identity and local/remote path mapping before editor integrations
  persist any platform-specific paths.
- Add secure local-agent startup and design remote authentication and trust setup.
- Introduce telemetry that is opt-in, privacy-preserving, and suitable for measuring
  performance and failures without collecting source code or secrets.
- Build the macOS packaging and release pipeline. Run platform-neutral contract
  tests on Linux and Windows without treating those clients as Phase 1 products.

## Validation

- Package and launch the primary product on supported macOS versions.
- Run the same mock task through loopback and remote protocol implementations.
- Verify cancellation, reconnect, version mismatch, corrupted artifact, and stale
  workspace behavior.
- Verify platform-neutral modules contain no accidental macOS-only dependencies.
- Compare the shell with the current platform guidelines and approved product states
  at supported display scales,
  window sizes, light/dark appearance, keyboard navigation, and increased contrast.

## Exit gate

The product shell is installable, protocol contracts are versioned and tested, and
all later Apple operations have an approved service boundary instead of direct IDE
process invocation. No Linux or Windows product milestone blocks this gate.
