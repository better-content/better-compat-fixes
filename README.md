# Better Compat Fixes

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


Pack-owned compatibility and runtime patch mod for Forge `1.20.1`.

With Weapons of Miracles **2.0.171**, Saulomonk admission measures horizontal distance
from the **nearest living, nonspectating player's permanent Better Spawns onboarding
point** in the current dimension, strictly greater than **2,000 blocks**. It no longer
measures from `(0, 64, 0)` or requires 5,000 blocks. Selection follows the nearest player
at the attempted mob position, not the nearest stored point; it does not borrow a farther
player's point when the nearest player lacks one. Missing, malformed or other-dimension
points deny admission, with no world-origin/bed fallback. The four typed
`better_spawns:respawn_{dim,x,y,z}` persistent-data fields are the existing owner contract;
no player files, chunks or offline-player histories are scanned. Native desert biome
injection, sand, difficulty and native mob-spawn checks remain unchanged. This is
an admission gate, not a despawn rule or a change to encounter weight/equipment/drops.
The optional mixin is version-gated and requires exactly one distance expression and
one threshold. `verifyFull` includes the exact WoM pin and executes its transformed
native predicate, including boundary, nearest-player, missing/different-dimension anchor,
spectator, sand and peaceful regressions with the NATURAL reason; the preserved
upstream predicate is `Mob.checkMobSpawnRules`, not the darker-light monster predicate. This is source-native verification, not full-pack
natural-spawn acceptance or deployment.

Chopping logs and Dynamic Trees branches emits material chips at a rate increasing linearly
from 2 to 18 particles per second with actual break completion: `2 + 16 * completion`.
A fractional accumulator integrates that rate once per native mining tick (20 Hz), never per
rendered frame or progress delta, and never sends a per-particle packet. Chips originate at the ray's cut-face
intersection, with Dynamic Trees using its native primitive-log/stripped-log material. Native hold-hit
particles are suppressed only for logs/branches; completion debris and other blocks remain native.
Stopping, restarting, target/material changes and progress rollback discard obsolete fractional debt.
Client visual acceptance remains separate from headless/source checks.

When Industrial Heat is installed, spoiled produce enters vanilla composting at a 65% layer chance
(first layer guaranteed by vanilla), yielding native bone-meal fertilizer on completion. With
Starcatcher `2.2.1`, spoiled meat is admitted by its additive native bait tag and receives the native
basic `DECREASES_LURE_TIME` default catch modifier. Its unchanged rod menu persists the stack;
Starcatcher's bobber applies the benefit and owns consumption. No fish pools, specialist/legendary
correct-bait lists, rod UI, or production loot are replaced. The default `verifyFull` requires the
canonical staged `better-industrial-heat-0.1.0.jar` provider (via `BC_CUSTOM_MOD_JAR_DIR` or sibling
`build/libs`) and actual pack-pinned Kotlin for Forge 4.12.0/ChemLib 2.0.19. It asserts production
common-setup registration on the actual spoiled items, without stand-ins or manually installing
bridge defaults, then native compost consumption/fertilizer and rod-menu persistence/lure benefit.
The real bobber's public ticks traverse fluid contact, bite and missed-bite consumption; only its
public timing fields are accelerated. `./gradlew clean verifyFull stageRuntimeJar -PspoilageWithoutHeat=true --no-daemon`
separately proves actual provider/registry absence without fabricating bait defaults. These isolated
source profiles are not full-pack, elapsed fishing, real-client or human-pointer acceptance.
A resources-only `nativeFixture` mirrors the pack's existing authored disabled
`starcatcher:aquaculture_fillet` and five narrowly scoped Farmer's Respite data repairs (three
optional Miner's Delight cup recipes, kettle loot function and tea-bush advancement trigger).
Exact fixture/pack byte parity and native dependency hashes are checked; every override is excluded
from this mod's production JAR. No diagnostic is suppressed or allowlisted by these repairs.
Previous source worlds, mods and logs move to `run-gametest/retained/` before each fresh native run.
The same nonshipping fixture gives the two unused Deep Void dimensions empty flat generators while
retaining their exact native dimension-type IDs. This isolates incidental upstream `flesh_stuff` /
`giant_skulls` far-chunk placement failures during dimension startup; it is not an error exemption.
The existing native fall-boundary tests still execute the unchanged Overworld/End capability decisions.
Neither this fixture nor its pass proves Deep Void terrain generation or a real player teleport;
full-pack runtime gates must use the actual unmodified production dimension generators.

The dispenser registry's one native initializer expression (`Util.make`) returns a synchronized
map using pinned development/production invocation selectors (exactly one must match). Its original
final-field assignment stays in native `<clinit>`: no shadow, access-transformer or relocated
final-write bridge is needed in consumers. Native direct writers and `registerBehavior` share that
same map; the native gate exercises concurrent writes and rejects failed mixin applications as
failed operations rather than treating them as harmless warnings. The archive/native-bytecode
contract also checks the original final-field assignment and prohibits mixin field-write bridges.
The concurrency diagnostic itself lives only in `nativeFixture`: its exact getter-only Mixin
accessor reads the pinned private static final registry (`0x001a`) without changing its visibility,
modifiers or value. Only the GameTest launcher loads that fixture config; the production archive
must exclude its test, accessor and configuration, and production classes do not refer to them.

With Curios `5.14.1+1.20.1` and Sophisticated Backpacks `1.20.1-3.24.35.1675`
(FML mod version `3.24.35.1675`), every installed
`BackpackItem` tier can be worn by players only in functional Curios `better_backpack` index 0.
Final native API/handler admission overrides competing Curios predicates and equip-event results
for backpacks and reserves `better_backpack` exclusively for them, including against a competing
ALLOW for a nonbackpack. Other Curios identifiers, indices and cosmetic slots reject backpacks.
Forge armor admission and native player dispenser eligibility reject them independently of Curios:
with the pinned Sophisticated version but no Curios, bags remain carry/place/open items, not wearable
equipment. The two armor hooks and their recognition/warning policy have no Curios class linkage. Nonplayer
wearing mobs, unrelated armor/accessories, hands, ordinary inventory/hotbar carrying and native
backpack placement/opening remain under their original owners. No storage or packet protocol is
replaced. The armor hooks require only the exact Sophisticated FML version; Curios hooks require both
exact versions. Tests inspect hash-pinned upstream bytecode and transformed native GameTests covering
all six tiers, reserved-slot nonbackpack denial, competing listeners and a real no-Curios mod profile.

The Curios equipment allowlist also recognizes Better Drinking Water's functional
`empty_bottle[0]`: glass bottles are left to the owning mod's native validator, just as water
bottles are in `water[0]`. Unrelated items, extra indices and cosmetic empty-bottle slots remain
denied. This prevents the allowlist from blocking manual glass-bottle placement and causing
automatic empty returns to fall through to backpack/hotbar storage. Return order, stacking and
storage remain owned by Better Drinking Water; paired in-game acceptance is pending.

Already misplaced equipped bags are not moved or deleted. Login gives one concise warning when
such a bag is present; native removal remains available. Operator authoring commands/direct saved
state setters are deliberately outside gameplay admission. The pack owns the `better_backpack`
slot declaration. Production-JAR checks and native admission tests do not replace paired real
client review or human pointer acceptance under [native parity](../../better-content-modpack/docs/inventory-parity.md).

With Oculus `1.8.0` and Distant Horizons `2.4.5-b`, the client opaque-depth sampler uses DH's
connected live depth when DH transparency is `DISABLED`. Oculus otherwise supplies an opaque-depth
copy refreshed only by the transparent-pass event, which DH skips in this mode. Live depth is
already opaque-only when transparency is disabled. `FAKE`/`COMPLETE`, unavailable DH configuration,
and unconnected/cleared framebuffers retain Oculus' native result. The setting and texture ID are
read on each request, so no cached override survives a setting change or resize. Camera transforms,
view bobbing, shader TAA history, and DH transparency settings are unchanged. This is a depth-input
correction, not a claim of completed in-game blur/bobbing acceptance; visual verification is pending.

The Deep Void `1.98.1` fall entry uses each source dimension's actual minimum build height minus
six blocks instead of its obsolete absolute `TeleportHeight`. All three native comparisons (normal,
any-dimension, and End entry) are patched; native enable switches, dimension eligibility, comparison
operators, capability updates and teleport operations remain unchanged. Native Overworld defaults
still trigger below -70; a -128 floor triggers below -134, and a zero-based floor uses -6. The pack's
six-block margin deliberately supersedes the native absolute-height setting. Tests inspect the pinned
native procedure, exercise the transformed native entry branches and check the staged production JAR.

The same pinned Deep Void integration anchors both one-time origin entrance templates to the actual
minimum build height instead of `HoleStructureY`. Both delayed floor-opening callbacks use that same
bottom (retaining native bottom+1), while native X/Z offsets, template choice, config/gamerule guards
and the saved `CultTemplePlaced` flag remain unchanged. This does not regenerate or move an already
placed entrance. The pack enables `PlaceCultTemple`; existing unplaced worlds become eligible on their
next native level load. A nonshipping -128-bottom fixture exercises real template placement, both
delayed openings and disabled/already-placed controls. No distributed-hole generator or Abductor
spawn-rate change is introduced.

The Better Caves compatibility hook clips only block-carve attempts outside a chunk's legal vertical
range. This preserves Better Caves and C2ME generation at valid heights while preventing Better Caves
`2.0.6` from writing negative `CarvingMask` indices in zero-minimum dimensions such as
`lostcities:lostcity`.

When Lost Cities and C2ME are installed, biome decoration and Lost Cities feature placement share one
reentrant lock only in `lostcities:lostcity`. This prevents neighboring Lost Cities feature jobs from
accessing the same chunk section concurrently while leaving C2ME enabled and unrelated dimensions
parallel. The existing `lostCities.serializeDhC2meFeaturePlacement` config key remains for compatibility;
Distant Horizons is not required for the guard to activate.

Lost Cities 1.20-7.4.11 can read or write its 16-section cache at the exclusive upper build limit
while correcting scattered-building blocks. Better Content Fixes routes out-of-height reads through
the level accessor and skips impossible out-of-height writes, preserving every cached in-height
operation while avoiding the otherwise invalid section index 16.

Fallout Wastelands Beta 3.8.1 registers its city ruins as placed features even though its largest
template is 38 blocks wide. Better Content Fixes minimally shifts those templates in X/Z so their
rotation- and mirror-aware bounds, plus vanilla's one-block shape-update margin, remain inside
`WorldGenRegion`'s writable 3x3 chunk envelope. Templates wider or deeper than the resulting 46-block
content span are skipped before placement, preventing partial ruins and far-chunk writes. Placement
outside `WorldGenRegion` is unchanged. Fallout exposes the
non-distinct mod version `1.0.0`, so this compatibility is additionally tied to the pack's pinned Curse
artifact `431248/7127023` (Beta 3.8.1).

Twilight Forest 4.3.2508 retains one random source on each generated maze and may place different
chunks of that maze concurrently under C2ME. Better Content Fixes synchronizes only
`TFMaze.copyToStructure` calls that share that random source. Other structures, dimensions, and C2ME
work remain parallel.

Weather2 `1.20.1-2.8.3` server precipitation-height observations read only resident FULL chunks.
Loaded columns retain native heightmaps and the vanilla +1 surface convention; unavailable columns
retain Weather2's -255 sentinel. Initial storm biome sampling keeps vanilla biome zoom/seed and
resident native biomes, falling back to native uncached noise instead of awaiting terrain generation.
This avoids synchronous chunk waits during Weather2 ticks without disabling storms, changing their
spawn decisions, adding tickets, or suppressing watchdog/log failures. Client behavior is unchanged.
Local GameTests cover loaded height/biome fidelity and positive/negative absent columns; the staged
artifact gate checks both development/production biome selectors in this no-refmap repository.

With Pretty Pipes 1.15.0 and PneumaticCraft installed, the native Pipe Pressurizer
uses PNCR's tier-one machine air handler rather than Forge Energy. Its 1,000 mL
chamber accepts standard pressure tubes on all faces; acceleration costs 10 mL
per item and must leave at least 2 bar afterward. Payment and simulation are
server-authoritative and whole-stack atomic. There is no idle drain or FE
fallback. PNCR owns equalization and basic-machine overpressure behavior (5-bar
danger, 7-bar critical); ordinary Pretty Pipes routing continues without air.

The existing pressurizer screen/menu retains its player slots and uses PNCR's
actual `PressureGaugeRenderer2D` machine-screen gauge, including its native
working-pressure/danger coloring and needle. PNCR's `WidgetTooltipArea` displays
the native air handler's manometer pressure text. The two 16-bit menu data slots
preserve the complete signed air quantity; every screen has its own native
pneumatic display handler. No gauge artwork or needle renderer is duplicated.
The electrical bar and tooltip are removed, with localized operating text beside
the gauge. Dedicated servers do not load the client mixin.

Pneumatic state is saved separately under `better_compat_fixes:pressurizer_air`.
Existing electrically charged pressurizers become empty pneumatic machines;
legacy FE is discarded, not converted. Item/block/menu identities and pipe
networks are unchanged. Pack recipes and hover teaching are owned by
`kubejs/server_scripts/progression/20_logistics_roots.js` and
`kubejs/config/hover_annotations.json` in the modpack. Local tests are not real
GUI acceptance; paired in-game visual and human-pointer review is separately
required under the shared GUI policy.

Runtime behavior includes pack-owned compatibility fixes, including permanent Epic Fight Battle mode,
physical-arrow ParCool double-tap dodge, first-person player-limb hiding
that preserves Epic Fight held-item animations, automatic affirmative Explosion Overhaul scan decisions,
and the pinned Dynamic Trees Aether 1.3.3 obsolete-branch tag repair.

Fighting Styles replaces the basic-attack combo, including its dash/air slots, without replacing
held-item capabilities or special skills. Selected-style ordinary swings preserve combo progression
instead of being rejected by the held weapon's native animation-membership check. Dash/air clips
still reset the counter; timeouts, unrelated actions and the default selection retain native
weapon handling. The local native regression exercises the transformed Epic Fight counter path
with an axe style selected while holding a sword; this is not full-pack gameplay acceptance.

First-person limb hiding also applies while swimming/submerged: body arms/sleeves and chest
armor arms no longer have an underwater exemption. The hooks remain scoped to first-person body
and armor draw calls, not held-item layers. Both wrappers snapshot native hidden flags before
applying the first-person mask and restore them in `finally`, including when the native draw
throws. Pinned Epic Fight 20.14.17 native contracts additionally verify shared body/armor meshes
reinitialize their hidden flags on subsequent third-person draws; this source gate is not human
visual acceptance.

Epic Fight 20.14.17 first-person swimming/floating also respects visible melee attack overlays.
With non-stiff combos, the native POV selector otherwise skips a melee attack without a dedicated
POV clip and selects the lower swimming POV, hiding the weapon swing despite server-side damage.
A client-only selector wrapper stops at that attack and clears the aquatic POV through native
`FirstPersonLayer.off()`, allowing the renderer to use the combined movement/attack pose. Native
selection resumes when the attack layer goes away. Dedicated attack POV clips, non-attack layers,
land movement, limb hiding, combat movement locks and server-side damage are unchanged. The hook
is version-pinned and covered by selector regressions and hash-pinned native bytecode contracts;
these checks are not real-client visual acceptance.

With Aether `1.20.1-1.5.2-neoforge`, Curios `5.14.1+1.20.1`, and Epic Fight `20.14.17`,
Aether's gloves, pendants, and Shield of Repulsion opt into Epic Fight's native per-item Curios
adapter. A scoped geometry bridge skins their humanoid models to the animated skeleton while
retaining Aether's original material passes: slim/standard arms, dye, armor trims, enchantment/
Quark rune glint, and active/inactive shield textures. Unused default humanoid cubes are excluded
from gloves/pendants. Bind-pose baking restores model poses/visibility afterward; the pose scope
is exception-safe, and resource reload destroys cached meshes at Epic Fight's own Curios reload
boundary. Normal non-Epic rendering and Aether's separate first-person hand methods stay native.
This does not replace Curios slots, equipment/storage protocols, other mods' renderers, or cape
rendering. Hash-pinned native contracts and staged-JAR checks are source validation, not real-client
visual acceptance. Both personal-inventory paths (with/without backpack), mixed equipment,
resource reload, slim arms, materials, and combat/locomotion poses need same-artifact visual review
before claiming acceptance; no deployment or pack runtime run is implied by these sources.

With RBP 1.0.0 and Realistic Physics 1.0.1, only `better_spawns:respawn_marker` is rejected
at RBP's common falling/breaking-operation admission method. Placement integrity checks,
neighbor fall checks and chunk-analysis operations converge on this method, as verified against
the exact native archives. Other blocks, including `minecraft:crying_obsidian`, retain native
physics; neighbors still get their ordinary checks. No global physics switch, indestructibility,
world scan or crying-obsidian-wide exclusion is introduced.

Newly placed structure templates remove vanilla effect potions, splash and lingering potions, and tipped
arrows from embedded inventories. Plain water bottles and unrelated or mod-owned items remain intact, and
cached templates, existing chunks, player inventories, and command-given stacks are not changed.

For Quark 4.0-462 with Zeta 1.0-31, a middle mouse button actually bound to a native sort key
routes to at most one existing Quark sort callback. Vanilla personal inventory and both native
Curios screens use the player provider; eligible container storage and its player section use their
respective native providers. Cursor-held stacks, invalid/stale hover, hotbar/equipment/crafting,
overlaid controls and disallowed screens do not sort. Sophisticated storage keeps its independent
native input/sort handler. Other buttons, keyboard bindings and unbound middle-click retain native
Quark dispatch. Native packet, sound and storage authority remain unchanged; paired in-game visual
and human-pointer acceptance is pending.

Third-person views have independent toggle buttons. The existing vanilla perspective binding
(default F5, including any player customization) is now **Toggle Rear Third Person**; the new
**Toggle Front Third Person** binding defaults to F7 (F6 remains Epic Fight's skill screen).
Each button enters its view from either other view and returns to first person when pressed
again; simultaneous rear/front input favors rear. Both inputs use vanilla's camera-change branch,
preserving its post-effect/chunk-render updates and Epic Fight's camera-change callbacks rather
than setting the camera in a separate tick handler. No key migration, option rewrite, packet or
server change is introduced. Keep Epic Fight's `camera_perspective_toggle_mode = "VANILLA"`
(the pack default); its optional front-view skip policy still rejects front view. Native-shape,
shipping-selector and toggle/queue regressions are source validation; real-client combat/camera
acceptance remains pending.

Recommended control defaults are authored in the modpack's tracked `options.txt`, not migrated or
rewritten by this mod. They bind ParCool Fast Run to Shift, ParCool's contextual mouse actions to
Mouse 5, Ping Wheel to Mouse 4, Epic Fight lock-on to Caps Lock, and free target switching to
Shift+Caps Lock; Epic Fight dodge is unbound. ParCool's direct R dodge remains available, and
vanilla's forward double-tap sprint behavior is left intact. Double-tap a physical arrow key
within seven client ticks (about 350 ms) while holding physical Left Shift to request ParCool's
dodge in that direction while not crouching. Right Shift does not enable the shortcut. Movement-key rebinding does not move this shortcut onto WASD. Menus, focus loss,
pausing, crouching, releasing Left Shift and player/world changes clear pending taps. ParCool retains its native
stamina, cooldown, grounded-state and action restrictions; no velocity or key state is injected.
The pinned native direction-local/vector bridge remains pending real-client acceptance.

For ParCool 3.4.3.3, ledge grabbing no longer rejects an otherwise valid edge solely because
of upward speed. Native grab input, stamina, wall-running exclusion, facing, collision/reach and
surface-friction checks remain intact, as does Epic ParCool's additional terrain scan. This lets
the native detector attempt a catch during ascent instead of waiting until the jump slows down.
The change is pending human in-game verification; it is not a confirmed fix for every two-block
ledge failure.

With Epic Fight 20.14.17, Epic ParCool 20.12.0.1 and ParCool 3.4.3.3, the five ledge-hanging
first-person POV definitions no longer impose pitch/yaw viewing limits. This prevents Epic Fight's
view-limit correction from recentering the camera on wall-facing model yaw, including a correction
that would otherwise finish during pull-up. Body alignment, climbing movement, POV root/visibility
settings and camera transforms remain unchanged; unrelated animations and third-person camera
behavior are not altered. Source contracts and the repository's verification gate cover packaging
and build compatibility; human in-game verification remains pending. This is not a confirmed
diagnosis or fix for every pull-up camera snap.

Epic Fight 20.14.17's native `epicfight:biped/living/climb` first-person POV also has
its pitch/yaw viewing limits removed for ladder/vine climbing. The override preserves camera
transforms, root transformation, visibility and animation synchronization; it does not change
climbing movement, ParCool routes or other animations. Human in-game verification remains pending.

Vanilla crouch toggle remains a Survival land control. In water or bubble columns, descent
follows the held crouch binding instead, and leaving water does not restore the old land latch.
Jump input, sprint input, or an already sprinting player clears the land crouch latch without
changing the saved toggle option or the sprint mapping. Hold-crouch mode remains hold-based.
Source wiring checks cover these hooks; human in-game movement acceptance is pending.

The pack's `options.txt` also binds Rotavision rotate/toggle to Alt+R / Alt+T, Relics' ability list
to Alt+A, and More Artifacts' Eye teleport to Alt+P, and leaves Quark Lock Rotation unbound.
There is no runtime keymap migration or profile-version marker.

When Better Content Threads is installed, accepted server-side ParCool starts for the configured dodge,
fast-run, and contextual actions emit optional learning signals. The first action opens one persistent
episode; only a different qualifying action completes it, using the original correlation token.

On a client with EMI and TConstruct, a profile that has no `emi.json` starts with the TConstruct
Part Builder and Tinker Station pinned in EMI. The file is created before EMI initializes. Existing
EMI data is never parsed, merged, or replaced, so players can remove either favorite permanently.

Sleeping Overhaul's timelapse keeps simulating the world but is paced to 800 ticks per second by
default, making a full 12,000-tick night take about 15 seconds. An active timelapse stops when a
sleeper takes damage or a nearby hostile has that sleeper targeted with line of sight; owners of a
committed immediate warning, injury, or Font danger can call the typed interruption bridge. World
time, food, machines, and ordinary ticking continue under their existing owners. Ordinary item pickup extends one
block horizontally without changing vertical reach, and moving living entities have one 10% roll
per block entry to remove vanilla grass or tall grass. These policies are configurable in the
common Better Content Fixes config.

When Better Content Threads is installed, each participating sleeper emits one correlated
`sleep_started/night` to `sleep_finished/simulated_time` episode across a successfully simulated night.

Several formerly bundled features now ship as standalone Better Content mods: Realistic Placement
Preview, Exploration Tick Governor, Airtight Machinery, Regolith Farmland, Burnt Grass Compatibility,
and ReHooked Intro Hooks. Their registries, assets, config, and commands live in those repositories.

## Common commands

```bash
./gradlew verifyFast
./gradlew verifyFull
./gradlew stageRuntimeJar
# Native optional-absence regression: also omits Ars Nouveau, whose native dependency requires Curios.
./gradlew clean verifyFull stageRuntimeJar -PbackpackWithoutCurios=true --no-daemon
# Restore the default paired-inventory candidate after the absence profile.
./gradlew clean verifyFull stageRuntimeJar --no-daemon
```

## Release artifact

Deploy the reobfuscated jar from:

- `build/libs/better-compat-fixes-<version>.jar`

The pack currently tracks the canonical release jar in `mods/`, not source outputs or IDE runtime state.

## Community and support

For modpack and mod discussion, playtest feedback, and bug reports, join the [Better Content Discord](https://discord.gg/EkRnZbzqS9).

## Canonical identity

- Repository and Gradle project: `better-compat-fixes`
- Mod ID and resource namespace: `better_compat_fixes`
- Maven group: `com.bettercontent`
- Runtime artifact: `build/libs/better-compat-fixes-<version>.jar`

The canonical identity is a clean break. Legacy mod IDs, resource namespaces, configuration paths, commands, network channels, and saved-data keys are not migrated or aliased.
