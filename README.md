# Better Compat Fixes

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


Pack-owned compatibility and runtime patch mod for Forge `1.20.1`.

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
disabled ParCool directional double-tap dodge, recommended control migration, first-person player-limb hiding
that preserves Epic Fight held-item animations, automatic affirmative Explosion Overhaul scan decisions,
and the pinned Dynamic Trees Aether 1.3.3 obsolete-branch tag repair.

First-person limb hiding also applies while swimming/submerged: body arms/sleeves and chest
armor arms no longer have an underwater exemption. The hooks remain scoped to first-person body
and armor draw calls, not held-item layers. Both wrappers snapshot native hidden flags before
applying the first-person mask and restore them in `finally`, including when the native draw
throws. Pinned Epic Fight 20.14.17 native contracts additionally verify shared body/armor meshes
reinitialize their hidden flags on subsequent third-person draws; this source gate is not human
visual acceptance.

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

The client control profile moves ParCool Fast Run to Shift, ParCool's contextual mouse actions to Mouse 5,
Ping Wheel to Mouse 4, and Epic Fight lock-on to Caps Lock. Existing customized bindings are preserved;
only exact legacy defaults are migrated once. ParCool's direct R dodge remains available, and vanilla's
forward double-tap sprint behavior is left intact.

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

Keymap profile v2 moves Relics' ability list to Alt+A
and More Artifacts' Eye teleport to Alt+P, and unbinds Quark Lock Rotation. As with the original profile,
only bindings still equal to the prior pack defaults migrate; player customizations always win.

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
