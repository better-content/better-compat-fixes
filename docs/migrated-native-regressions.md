# Owned compatibility regressions

The source-only `nativeFixture` adds an explicitly selected regression profile:

```sh
export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-tem"
export PATH="$JAVA_HOME/bin:$PATH" # Existing bytecode-oracle checks invoke javap.
export TMPDIR="$HOME/.tmp"
# Point to explicitly prepared API providers, as for existing owner verification.
export BC_CUSTOM_MOD_JAR_DIR=/path/to/verified/providers
flock "$HOME/.tmp/better-content-gradle.lock" ./gradlew verifyFast --max-workers=2
flock "$HOME/.tmp/better-content-gradle.lock" ./gradlew headlessGameTest -PwithMigratedRegressionFixture=true --max-workers=2
```

The opt-in adds exact Curse Maven runtime pins to the existing nonshipping fixture: Cataclysm 8246047 (3.31), Lionfish API 8095554 (3.0), The Flesh That Hates 5235839, Untamed Wilds Au Naturel 7441092, and its real Citadel dependency 7476570 (2.6.3; commented out in upstream metadata). No JAR is copied from a pack or historical lane cache. Registration fails if an opted-in required mod is absent. Without the opt-in, these optional upstream integrations are not claimed as executed by the ordinary gate.

Coverage:

- Actual transformed Cursed Pyramid marker callback: suspicious-sand marker inside the active chunk still executes; the same marker outside it leaves stone untouched. A real bundled pyramid template is placed through `TemplateStructurePiece.postProcess` with fixed RNG seed `0xC0FFEE`; a forwarding WorldGenLevel observes native writes and rejects writes outside the current chunk. This is a pinned native template-placement regression, not full desert-biome structure discovery or a pack log quota.
- Actual `FleshBlockSavedData.load` and `SavedData.save`: the captured `src/nativeFixture/resources/regressions/flesh-spread-printed-keys.dat` fixture preserves its printed origin key and false value; an added negative-coordinate counter key exercises the second map. Both maps are rewritten as packed-position keys and then round-trip. This captured fixture was migrated intact from the retired pack test resource (SHA-256 `38702860ea499a3d283702bc77606a60b3f44f86dec5a34d6d4cf2f01e0adca1`); tests read only the owning repository's copy. It does not substitute a parser-only assertion for the upstream persistence entry point.
- Real bundled baleen-whale species JSON is decoded into `ComplexMob.ENTITY_DATA_HASH` when absent; subsequent consumers preserve cached identity. Foreign entity and dedicated-server admission are rejected. The small production-used `Dist` overload permits this client-policy boundary on a headless server. It does **not** claim real-client startup, rendering, packet delivery, or absent-data execution of the client mixin itself.

The existing spawner native scenario now uses the owning fixture's `data/minecraft/tags/blocks/needs_diamond_tool.json` as a controlled configured-tier example, with the production-JAR exclusion check retained. This tag is not a production mod policy or a copy/read from the current pack. Existing felling contracts retain the mod's own default/native behavior checks, not assertions about sibling pack configuration.

These tests own no pack warning allowlist, restart scenario, biome override, or sibling pack resource. Existing broader native-fixture setup still has its separately documented provider and authored-resource requirements; this profile does not add new sibling-checkout inputs.
