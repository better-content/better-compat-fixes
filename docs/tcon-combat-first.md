# TCon combat-first block clicks

## Shipping behavior

A main-hand stack is protected from block attacks only when both optional mods are
present, its item implements `IModifiable`, its loaded tool definition has no
`ToolHooks.IS_EFFECTIVE` entry, and its resolved, stack-specific Epic Fight
capability is non-null and nonempty. No item list or eligibility cache is used.
Ranged, shield, and integration-specific wrappers qualify. Definition-level mining
categories are authoritative regardless of broken status, materials, harvest tier,
mining speed, or modifier-only effectiveness. Unknown/unloaded state preserves
existing behavior. An offhand item never controls main-hand eligibility.

The client changes only the adaptive block-versus-combat choice in Epic Fight's
`canPlayAttackAnimation()`. It uses the method's selected first-/third-person hit
result and preserves early vanilla-mode, nonliving-entity, and lock-on handling.
Normal EF requests, reservations, skill restrictions, combos, and collision remain
native. A ranged capability does not acquire a fabricated melee combo.

Separate vanilla start/continue guards prevent block callbacks, hit effects,
progress, and instant creative breaks, even without a playable EF attack. They
call native `stopDestroyBlock()` before cancellation to abort old mining after a
tool swap. They never consume attack keys or change global targeting. The new
mixins run before EF's own vanilla-attack cancellation to retain abort cleanup.
Forge left-click and break-event backstops use the same eligibility predicate;
ABORT events remain untouched. All game modes are covered. Right-click is unchanged.

No production moveset/tool data, user preferences, packets, or configuration are
changed. The existing permanent battle-mode policy remains separate. The input
mixin is gated on EF 20.14.17 plus TCon presence; common optional references are
isolated behind a ModList-guarded bridge. A dedicated production mixin config/plugin
is registered in both the runtime manifest and development launcher. The legacy
bytecode-pinned mixin plugin and its config are unchanged.

## Deterministic tests

- `TconCombatFirstInputTest`: executes the pinned EF decision bytecode, baseline
  and with the intended expression substitution, using input doubles. Covers
  384 free-aim pairs, eight TPS/lock-on pairs, and hook/registration invariants.
- `TconCombatFirstPolicyTest`: executes actual compiled eligibility/facade bytecode
  with integration doubles. Covers the condition matrix, absent mods, null/empty,
  ranged/shield/TCon capabilities, conditional results, same-stack data reloads,
  broken/material invariance, and main/offhand swaps.
- `TconCombatFirstGuardTest`: executes actual compiled client guard handlers with
  progress doubles. Covers initial/held/released attack, block/entity/null hit,
  eligibility, absent game mode, cancellation, and native progress cleanup.

These tests are not a substitute for applying mixins in Minecraft or real input
and rendering. The extracted-EF tests intentionally change only the one adaptive
expression; no test doubles are shipped.

## Nonshipping native profile

`-PwithTconCombatFirstFixture=true` adds the pinned Tinkers/Epic integration only
for the opted-in source runtime, forwards the fixture flag, and includes four
controlled capability fixtures (Battlesign/Javelin/Longbow/Plate Shield). These
resources are outside `src/main` and are never shipped. They test actual loaded
runtime mappings rather than assuming production pack resource precedence.
Default native profile populations are unchanged; the opted-in strict population
increases by exactly three. No old count/severity requirement is relaxed.

`TconCombatFirstGameTests` scans the actual loaded modifiable items/capabilities,
checks mining negatives, tests real definition reload setters with synchronous
restoration, and posts real Forge events plus native player block removal in all
game modes. It also verifies main-hand-only behavior and ABORT cleanup.

From an isolated source snapshot with an isolated Gradle home and existing provider
JARs, run bounded Java 17 / max-two-worker checks:

```sh
BC_CUSTOM_MOD_JAR_DIR=/path/to/providers ./gradlew --max-workers=2 \
  test --tests 'com.bettercontent.bettercompatfixes.TconCombatFirst*Test' assemble
BC_CUSTOM_MOD_JAR_DIR=/path/to/providers ./gradlew --max-workers=2 \
  -PwithTconCombatFirstFixture=true nativeFixtureClasses
# Only after the user accepts the EULA for the owned disposable native fixture:
BC_CUSTOM_MOD_JAR_DIR=/path/to/providers ./gradlew --max-workers=2 \
  -PwithTconCombatFirstFixture=true verifyFull
```

Do not infer native success from compilation, generated fixtures, test doubles,
or build success. Preserve failed logs/worlds and audit the strict wrapper result.

## Required real-client acceptance

In first and third person, free aim and lock-on: block-targeted attack with an
eligible melee weapon must issue the ordinary EF attack, with no mining callback,
cracks, hit effects, or removal. Hold attack; exercise shared attack/innate bindings;
swap from an actively mining pickaxe while still holding attack; repeat in creative
and adventure and against left-click-sensitive blocks/fire. Verify visible-entity
attacks, cooldowns, skill restrictions, selected-style combos, and existing wall
collision/reach behavior. Verify ranged use, shields, throwing, staff abilities,
and right-click block interactions remain normal. Mining-category tools (including
Sword/Dagger/Scythe/Melting Pan), non-TCon items, and empty hands retain baseline
behavior. Repeat on a dedicated server and after definition/capability reloads.

## Current evidence

Evidence is retained in `/home/dev/.tmp/tcon-combat-inspection/` and the isolated
`/home/dev/.tmp/tcon-combat-qualification/` snapshot. Direct actual-API javac of the
production changes and native fixture passed. All eight deterministic methods
passed when invoked through the retained JUnit-annotation runner. An initial
Gradle configuration attempt failed due to a missing default provider path; a
second attempt with explicit existing provider directory passed selected tests
and assemble. The first full gate passed all 390 JUnit tests, then correctly
rejected a changed legacy-plugin hash. That attempt is retained; the feature was
moved to its own config/plugin instead of changing or weakening any pinned hash.
Final `verifyFast assemble nativeFixtureClasses` passed (390 JUnit tests, including
all eight new tests; zero failures/skips). The explicit mixin namespace inventory
includes the new config with exact-population and negative mutation checks.
The opt-in fixture compilation/configuration check also passed: exact integration
20.2.8 runtime resolved, all four controlled capability resources were copied only
to native output, the child opt-in flag was forwarded, and strict native-wrapper
count/severity negative controls passed. The production archive contains the new
policy/config and no native fixture classes/capability data; its legacy plugin hash
still matches the unchanged upstream contract. Logs: `gradle-qualified.log` and
`gradle-fixture-wiring.log` in the qualification snapshot; earlier failures retained.
Native GameTests, multiplayer, applied client mixins, and visual/input acceptance
remain unqualified until actually run. No EULA was accepted, pack deployed, or
artifact pushed by this work.
