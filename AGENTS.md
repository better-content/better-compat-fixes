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
  Present adds only pinned Registrate 1.3.3 and two actual upstream Base tests (83 with the
  default graph); no Create/Ponder/Flywheel fixture or runtime dependency is added. Ordinary
  default/no-Heat81, no-Bees79 and no-Curios78 populations include pending-read ownership and
  whole-bone placement cases; every previous native identity remains required without this flag.
  This does not replace existing required optional profiles or authorize native execution.
  Raw/mapped paired class pins and compiled site/forwarding checks run in `verifyFast`;
  WrapMethod/Inject composition and actual native behavior require the separately admitted gate.
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
