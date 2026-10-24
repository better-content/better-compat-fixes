# Better Compat Fixes

Forge 1.20.1 / Java 17; mod ID `better_compat_fixes`.

## Local verification

- Deterministic CI-equivalent lane: `./gradlew verifyFast`.
- Runtime/GameTest changes: `./gradlew verifyFull`.
- Stage: `./gradlew stageRuntimeJar`, `build/libs/better-compat-fixes-<version>.jar`.
- Registrate construction changes additionally require the native-present SOURCE profile:
  `./gradlew clean verifyFull stageRuntimeJar -PwithRegistrateFixture=true --no-daemon`, then
  `./gradlew clean verifyFull stageRuntimeJar --no-daemon` for fixture-disabled/default staging.
  The existing default graph already includes Registrate/Create; this is NOT target-library absence.
  Only the resource-only JDK control currently proves the genuinely absent plugin branch.
  Present selects exact pinned Registrate 1.3.11 (the full pack's Fruits Delight JarJar winner)
  and two actual upstream Base tests (87 with the four additive fuel cases); other profiles retain 1.3.3.
  Both native witnesses require the exact 1.3.11 mapped resource pair from the active loader.
  No Create/Ponder/Flywheel fixture or new library family is added. Ordinary
  default/no-Heat85, no-Bees83 and no-Curios82 populations include pending-read ownership,
  whole-bone placement and four real consumption/emission cases. These are required populations,
  not a claim that newly integrated fuel cases have passed; every previous identity remains required.
  This does not replace existing required optional profiles. Standing native authorization and
  coordination follow the shared workspace testing policy.
  Raw/mapped paired class pins and compiled site/forwarding checks run in `verifyFast`;
  WrapMethod/Inject composition and actual native behavior require the separately admitted gate.
- Active `qualifyUpstreamRuntimeOverridesV2` under `verifyFast` requires 19 pinned-original/pure
  controls, including deterministic repeat builds, unchanged 3,144 recursive classes/licenses/scripts,
  exact current replacement/plugin bytes, and immutable legacy builder/spec identities. The successor
  removes four exact obsolete registrations; only AdPother changes its output suffix to `bc-runtime.2`.
  Stable support is under `build/upstream-runtime-overrides-v2/`; originals/attempts remain under
  `build/upstream-runtime-v2-evidence/`. Retain v1 files, receipts, artifacts and failed candidates
  unchanged. Checksum pins are internal reviewed identities, not an external permission barrier.
  No cache mutation, log exemptions or public corrected-binary publication. See the legacy contract
  `src/upstreamRuntime/README.md` and current Root `docs/upstream-runtime-overrides.md`.
- Independent `qualifyUpstreamStartupOverrides` under `verifyFast` binds exact ATL annotation-only
  applicability for the reviewed Citadel 2.6.3 ABI, before issue initialization. Separate two-file
  support is under `build/upstream-startup-overrides/`. Of 880 classes, only the exact annotation
  UTF8 operand changes; all Code/method/field/resource/notices stay unchanged. Actual Forge scan and
  Maven range controls are not cold-start acceptance. Do not substitute an early IssueManager Mixin,
  reflection fallback, no-op constructor or global issue disable.
- Four nonshipping native fuel cases use actual Hearth/Boiler/steam-vehicle consumption and completed
  original AdPother integer-plus-fraction emission state against single/doubled native references.
  Keep all original operations intact and preserve zero/manual/cold/remaining-burn controls. The
  fixture-only corrected SDK uses its own immutable local Maven coordinate, not overwritten originals
  or cached mappings. `compileNativeFixtureJava`, actual `verifyFull` populations and same-candidate
  Dist are distinct requirements. Epic's required server-only startup ABI hook likewise requires
  native cold Rhino binding proof, not merely its eight structural controls.
- Separate `qualifyUpstreamInterfaceOverrides` under `verifyFast` runs 12 controls over two
  additional originals. It must not alter the preceding five-artifact recipe or its 3,144-class
  invariant. Only three exact PRIVATE|STATIC -> PUBLIC|STATIC method words in one pinned VS
  interface may change; all Code/annotations/descriptors and every other class byte stay exact.
  New side moves are limited to the two reviewed client-only Mixins. Separate three-file support
  is under `build/upstream-interface-overrides/`; retained attempts/originals stay separate.
  No global Mixin bootstrap replacement, server-handler removal or public binary publication.
  See `src/upstreamRuntime/interface-README.md`; actual native acceptance remains mandatory.
- Default full native gate requires canonical staged `better-industrial-heat-0.1.0.jar`
  via `BC_CUSTOM_MOD_JAR_DIR` or sibling `build/libs`; prepare that provider first.
  Spoilage changes also run `./gradlew clean verifyFull stageRuntimeJar -PspoilageWithoutHeat=true --no-daemon`,
  then default `./gradlew clean verifyFull stageRuntimeJar --no-daemon` for the actual-item candidate.
  The full gate rejects native ERROR/FATAL diagnostics and retains prior native worlds/logs/mods.
- Backpack admission changes also run `./gradlew clean verifyFull stageRuntimeJar -PbackpackWithoutCurios=true --no-daemon`.
  This native profile omits Curios and its mandatory-dependent Ars Nouveau, asserts actual mod absence,
  and runs the Curios-free armor regression. Then run default `./gradlew clean verifyFull stageRuntimeJar --no-daemon`
  to stage the paired-inventory candidate. Production-JAR checks include optional-class-linkage absence;
  this source/GameTest profile is not a standalone production-server or human-pointer verdict.
- Pending chunk-read changes require `verifyChunkIoOwnershipArtifact`, four deterministic JUnit
  controls, and the native two-barrier test-owned worker case. The dedicated required config and
  exact AP method/field mappings must survive staging; do not move ordinary fixture helpers into
  the reserved native mixin package. Production-default preparation uses the three-file immutable
  `gradle/native-default-configs-v2/`; retain the historical two-file snapshot unchanged.
- Inventory compatibility changes also read [native parity](../../better-content-modpack/docs/inventory-parity.md).

## Shared authority

Read [workspace policy](../../better-content-modpack/docs/policies/workspace.md),
[testing](../../better-content-modpack/docs/testing.md) and
[disposal](../../better-content-modpack/docs/policies/generated-data.md).
Docs-only changes use the shared documentation check and `git diff --check`.
