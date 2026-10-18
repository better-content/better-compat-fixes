# Better Compat Fixes

Forge 1.20.1 / Java 17; mod ID `better_compat_fixes`.

## Local verification

- Deterministic CI-equivalent lane: `./gradlew verifyFast`.
- Runtime/GameTest changes: `./gradlew verifyFull`.
- Stage: `./gradlew stageRuntimeJar`, `build/libs/better-compat-fixes-<version>.jar`.
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
- Inventory compatibility changes also read [native parity](../../better-content-modpack/docs/inventory-parity.md).

## Shared authority

Read [workspace policy](../../better-content-modpack/docs/policies/workspace.md),
[testing](../../better-content-modpack/docs/testing.md) and
[disposal](../../better-content-modpack/docs/policies/generated-data.md).
Docs-only changes use the shared documentation check and `git diff --check`.
