# Phase 1 — Platform Foundation

## Objective

Establish a standalone, testable IntelliJ application whose Apple-specific work is
performed through local or remote service contracts.

## Workstreams

- Bootstrap the Gradle/IntelliJ build, standalone product metadata, bundled plugins,
  runtime, packaging, update channel, and CI verification.
- Establish client, protocol, build-agent, and device-gateway module boundaries.
- Implement capability discovery, task lifecycle, progress, cancellation, structured
  diagnostics, logging, and version compatibility semantics.
- Define source identity and local/remote path mapping before editor integrations
  persist any platform-specific paths.
- Add secure local-agent startup and design remote authentication and trust setup.
- Introduce telemetry that is opt-in, privacy-preserving, and suitable for measuring
  performance and failures without collecting source code or secrets.
- Build hermetic test fixtures and CI jobs for macOS, Linux, and Windows client code.

## Validation

- Package and launch the empty product on every target desktop OS.
- Run the same mock task through loopback and remote protocol implementations.
- Verify cancellation, reconnect, version mismatch, corrupted artifact, and stale
  workspace behavior.
- Verify platform-neutral modules contain no accidental macOS-only dependencies.

## Exit gate

The product shell is installable, protocol contracts are versioned and tested, and
all later Apple operations have an approved service boundary instead of direct IDE
process invocation.
