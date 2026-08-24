# IntelliJ Plugin Compatibility Workstream

## Objective

Make AppXCode a well-behaved IntelliJ Platform product that can install and run the
widest practical set of existing plugins without lying about unavailable APIs,
ignoring plugin metadata, or destabilizing the primary IDE.

Compatibility means a plugin's declared IntelliJ build range is valid, every
required module and plugin is present, declared incompatibilities are respected,
referenced public APIs exist with compatible behavior, and installation/runtime
smoke tests pass. No custom IntelliJ-based product can guarantee every plugin built
for every JetBrains IDE because products ship different module sets and some
plugins use commercial, product-specific, internal, or obsolete APIs.

## Compatibility contract

### Tier 1 — Core-compatible

Target full compatibility for plugins that:

- Declare a supported AppXCode/IntelliJ build range.
- Depend only on public module families AppXCode ships and documents, initially
  centered on `com.intellij.modules.platform`, `com.intellij.modules.lang`,
  `com.intellij.modules.xml`, `com.intellij.modules.vcs`, and
  `com.intellij.modules.xdebugger` where the selected upstream distribution exposes
  them.
- Avoid internal, experimental-without-policy, obsolete, and scheduled-for-removal
  APIs outside the accepted compatibility baseline.
- Pass descriptor validation, Plugin Verifier, installation, startup, enable/disable,
  and representative runtime smoke tests.

### Tier 2 — Bundled-feature compatible

Support plugins that depend on optional open-source modules or plugins AppXCode
deliberately bundles, versions, tests, and redistributes. Candidate examples include
Git, terminal, Markdown, JSON, YAML, Kotlin, and other dependencies selected during
Phase 0. Inclusion is not assumed until license, footprint, security, maintenance,
and interaction checks pass.

### Tier 3 — Product-specific

Do not claim compatibility by default for plugins requiring modules or plugins that
belong to another product, such as:

- IntelliJ IDEA Ultimate licensing or commercial feature modules.
- Java, Python, PHP, Rider/ReSharper, CLion/CIDR, or other language/product APIs not
  present in the AppXCode distribution.
- Legacy AppCode module IDs and Swift/CIDR APIs whose original implementation is not
  part of AppXCode.
- Third-party dependency plugins that are unavailable or outside their own supported
  version range.

A Tier 3 plugin can move to Tier 2 only when AppXCode lawfully provides all required
dependencies and verifies their behavior. AppXCode must not publish fake module
aliases merely to force a plugin to load.

### Tier 4 — Rejected or quarantined

Reject or isolate plugins with invalid descriptors, incompatible build ranges,
declared AppXCode/module incompatibility, missing required dependencies, failed
signature/security policy, verifier-critical API failures, or repeated startup
crashes. The product must explain the exact reason and offer safe recovery.

## Platform invariants

- Track an upstream IntelliJ Platform release band and preserve its build-number
  semantics for plugin `since-build`, `until-build`, and `strict-until-build` checks.
- Do not modify the semantics of public platform modules or extension points merely
  to make AppXCode internals easier.
- Keep AppXCode APIs in explicit AppXCode modules/plugin IDs with documented status,
  availability, and compatibility policy.
- Preserve normal `plugin.xml` dependency, optional dependency, additional descriptor,
  and `incompatible-with` behavior.
- Preserve standard plugin classloader isolation and avoid loading plugin libraries
  into the core/application classloader.
- Use the upstream JetBrains Runtime required by the selected IntelliJ Platform
  release unless an evidence-backed compatibility decision says otherwise.
- Avoid AppXCode core code depending on arbitrary third-party plugins.

## Product module inventory

Every release publishes a machine-readable and human-readable inventory containing:

- IntelliJ platform build and JetBrains Runtime versions.
- Product code and AppXCode version.
- Built-in module IDs, plugin IDs, aliases, versions, and API-status policy.
- Optional bundled plugins and whether users may disable them.
- AppXCode-specific modules and extension points.
- Removed, replaced, incompatible, experimental, and scheduled-for-removal APIs.
- Remote frontend/backend availability for every module and plugin.

The inventory is generated from the built distribution and verified against the
runtime. It is not maintained as an unaudited hand-written list.

## Plugin acquisition and management

- Investigate formal JetBrains Marketplace recognition for the AppXCode product code
  and compatibility filtering.
- If the Marketplace cannot represent AppXCode directly, provide a signed AppXCode
  compatibility repository containing approved plugin versions and metadata; do not
  silently mirror or redistribute plugins without permission.
- Continue supporting installation from disk subject to the same compatibility and
  security checks.
- Display compatibility tier, source, signature, permissions/capabilities where
  available, required dependencies, restart behavior, and verification result.
- Support update, rollback where feasible, disable, uninstall, repair, safe mode,
  and startup disable-list recovery.
- Never override a plugin author's declared build range or incompatibility without a
  user-visible, unsupported developer mode separated from the normal product.

## Compatibility pipeline

### Descriptor classification

For every cataloged plugin version:

1. Read ID, version, build range, required/optional dependencies, additional
   descriptors, and incompatibilities.
2. Compare requirements with the exact AppXCode distribution inventory.
3. Classify the plugin into a support tier with machine-readable reasons.
4. Reject impossible candidates before downloading or executing them.

### Binary verification

- Run JetBrains Plugin Verifier against the exact AppXCode build where supported by
  the tool and prove this path during Phase 0.
- Treat missing classes/methods, forbidden internal API use, invalid extension
  points, and binary incompatibilities according to release policy.
- Record verifier version, IntelliJ/AppXCode build, plugin checksum, result, and
  accepted waivers.
- Re-run the catalog whenever the upstream platform, bundled module set, JDK/runtime,
  or plugin version changes.

### Runtime verification

- Install and enable in a disposable profile.
- Start the IDE, open a fixture project, invoke representative actions/extensions,
  persist settings, restart, disable/unload where supported, update, and uninstall.
- Attribute startup, UI thread, indexing, memory, classloader, and shutdown failures
  to the plugin when evidence permits.
- Test safe mode and automated recovery from startup loops.
- Run untrusted compatibility tests in isolated CI workers with no developer
  credentials, signing keys, personal repositories, or unrestricted network access.

## Catalog and service-level policy

Maintain a versioned catalog rather than the untestable claim “all plugins”:

- **Certified:** metadata, verifier, runtime, upgrade, and recovery tests pass for an
  exact plugin/AppXCode version pair.
- **Expected compatible:** metadata and verifier pass, but representative runtime
  coverage is incomplete.
- **Community reported:** user evidence exists but AppXCode CI has not certified it.
- **Incompatible:** a specific missing dependency, build range, API, crash, license,
  or security reason is recorded.
- **Unknown:** not evaluated; the UI must not present it as supported.

Prioritize widely used platform-level developer productivity, themes, keymaps,
editor, Git, documentation, AI-assistance, and collaboration plugins that do not
require unrelated product-specific language modules. Catalog size and certification
targets are set only after Phase 0 measures CI cost and Marketplace access.

## AppXCode plugin SDK

- Publish a targetable AppXCode IDE artifact or documented local-product setup for
  the IntelliJ Platform Gradle Plugin 2.x.
- Publish product/module metadata, AppXCode extension-point documentation, examples,
  and migration guidance.
- Provide a development instance, test fixtures, verifier configuration, and a
  compatibility report format plugin authors can reproduce.
- Keep Swift/Xcode/device/build APIs separated into explicit modules so plugins can
  depend only on the capability they use.
- Use API annotations and an availability/deprecation policy consistent with the
  selected IntelliJ release band.

## Remote development

Future remote mode evaluates compatibility separately for:

- macOS build/backend plugins that access projects, indexes, toolchains, builds,
  tests, devices, or debuggers;
- client/UI plugins that contribute actions, editor UI, tool windows, settings, or
  presentation; and
- split plugins with a versioned frontend/backend protocol.

The plugin manager must show where each component runs, detect side/version skew,
and avoid transferring arbitrary plugin code or secrets between machines without
explicit policy.

## Validation and release gates

- The generated module inventory matches the packaged and running product.
- Every Certified plugin passes against the exact release candidate and checksum.
- Required Tier 1 representative plugins install, start, operate, persist settings,
  update, disable/unload, uninstall, and recover without corrupting the IDE profile.
- Missing, product-specific, incompatible, or out-of-range plugins fail before
  execution with an actionable explanation.
- A broken plugin cannot permanently trap the user in a startup crash loop.
- Platform upgrades cannot ship until catalog impact, binary verification, runtime
  smoke results, and approved waivers are reviewed.
- AppXCode public compatibility claims match the published catalog and contain no
  unqualified “all IntelliJ plugins” promise.

## Risks and mitigations

### Plugin requires another JetBrains product

Report the exact missing module/plugin and classify it as product-specific. Do not
fake product IDs or APIs.

### Marketplace does not recognize AppXCode

Pursue formal product integration; otherwise operate a permission-respecting,
signed compatibility repository and support verified installation from disk.

### Platform upgrades break a large catalog

Use planned IntelliJ release bands, pre-release verification, dual test lanes,
catalog impact reports, and rollback-ready AppXCode releases.

### Plugin destabilizes or compromises the IDE

Use descriptor/signature policy, classloader isolation, disposable compatibility
workers, startup attribution, disable lists, safe mode, and recovery UI. Record that
the IntelliJ plugin model is in-process and therefore cannot provide a complete
security sandbox by metadata alone.

### Compatibility work consumes the product roadmap

Commit strongly to Tier 1, choose Tier 2 deliberately, and keep Tier 3 explicit.
Do not delay core Swift/Xcode functionality to emulate unrelated product modules.

## Official references

- Plugin compatibility with IntelliJ Platform products:
  https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html
- Plugin dependencies:
  https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
- Plugin configuration and build ranges:
  https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
- Verifying plugin compatibility:
  https://plugins.jetbrains.com/docs/intellij/verifying-plugin-compatibility.html
- Targeting custom IntelliJ Platform IDEs:
  https://plugins.jetbrains.com/docs/intellij/dev-alternate-products.html
- Custom plugin repositories:
  https://plugins.jetbrains.com/docs/intellij/custom-plugin-repository.html

These are living references. Each AppXCode release records the exact upstream
IntelliJ Platform branch, documentation baseline, module inventory, and verifier
tool version used for its compatibility claims.
