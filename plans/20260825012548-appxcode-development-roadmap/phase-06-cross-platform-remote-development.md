# Phase 6 — Cross-Platform Remote Development

## Objective

After the primary macOS application is stable, extend AppXCode to Linux and Windows
while Apple-specific semantics, builds, signing, tests, and debugging execute on a
trusted macOS build node.

## Workstreams

- Productize macOS build-agent installation, enrollment, updates, diagnostics, and
  compatibility reporting.
- Implement authenticated pairing, encrypted transport, access policy, audit logs,
  and revocation for local-network and remote-network agents.
- Build content-addressed source synchronization with ignore rules, generated-file
  handling, atomic revisions, conflict reporting, and resumable artifact transfer.
- Map client, build, result-bundle, generated-source, and debugger paths without
  leaking remote filesystem assumptions into project data.
- Route Apple SDK language requests to remote SourceKit while preserving responsive
  local syntax behavior during latency or disconnection.
- Support remote dependency resolution, build cache policy, log streaming, test
  attachments, debug symbols, and task cancellation.
- Package and continuously test native Linux and Windows clients.
- Define frontend/backend plugin placement and compatibility independently for
  remote sessions, including split plugins, client-only UI plugins, backend-only
  analysis plugins, version skew, and missing-side diagnostics.
- Extend the Flutter profile so host-native Android/web/desktop workflows can run
  locally where supported while iOS/macOS work is routed to the macOS build node;
  preserve hot reload, debug, DevTools, artifact, and source-path semantics.
- Preserve macOS as the reference behavior and avoid delaying macOS maintenance or
  feature delivery to force lowest-common-denominator client behavior.
- Define user-owned Mac and future hosted-Mac operating models without coupling the
  client protocol to either deployment model.

## Validation

- Run the same project revision locally on Mac and remotely from Linux/Windows and
  compare diagnostics, artifacts, test results, and debugger source locations.
- Test high latency, limited bandwidth, reconnect, agent restart, client restart,
  version skew, workspace eviction, and interrupted artifact transfers.
- Verify signing assets and Apple credentials never leave the build node.
- Verify no Apple SDK content is copied into the client workspace or distribution.

## Exit gate

Linux and Windows users can edit an Apple project, receive authoritative semantics,
build and sign on a Mac node, inspect tests and artifacts, and debug through the
remote session with documented behavior under network failure.
