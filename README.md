# Better Compat Fixes

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).

## Bounded playtest diagnostics

Loader SLF4J `bc.playtest.better_compat_fixes.*` reports one INFO own-source/version/integration
status (UNKNOWN without exact own metadata). DEBUG observes actual custom-control episode
publication versus repeated input and sleep episode start/finish/defer after existing state
updates/publication. Arbitrary action text, player names, respawn positions, NBT and episode
UUIDs are not copied; records do not provide per-player cross-process correlation. Unchanged
sleep ticks, block/worldgen loops and render frames remain silent. One mod-wide 120/minute
burst20 detail budget, 256 aggregate keys, 2048-character records and 30-second summaries bound
output; disconnect/server stop clears aggregate state without bucket refill. Native behavior,
RNG, event contracts and existing failure severity/stacks remain unchanged. Standalone stays
INFO; the pack selects scoped DEBUG. Logging does not waive native inventory parity acceptance.


Pack-owned compatibility and runtime patch mod for Forge `1.20.1`.

## Dynamic Trees final-root soil admission (source candidate)

Pinned Dynamic Trees7502488 accepts water-fluid foliage through its water soil mapping, while
`Species.placeRootyDirtBlock` tests the raw block, warns, writes fallback dirt and returns false;
`JoCode.generate` ignores that false. This is a demonstrated contract gap, not proof of the
historical Species/caller or a concurrent terrain change behind the five canonical30 warnings.

The worldgen-only hook captures native `worldGen` slot5 and `initialDirtState` slot7 **after**
`preGeneration` and its final root-state read, immediately **before** the sole root conversion.
Raw registered soil and existing RootyBlock retain the original path; unsupported final soil
cancels the whole JoCode before fallback root conversion and subsequent branch writes. Earlier
preGeneration callbacks, writes and RNG remain intact; rejected paths intentionally skip their
remaining work. This is not a global no-write, RNG-neutrality or concurrent snapshot fence claim.
Nonworldgen saplings, water/depth traversal, ground/Y selection, registration and logging are unchanged.

`verifyRootySoilAdmission` under `verifyFast` checks actual raw/mapped class pins, exact invocation
and live locals, require=allow=1, compiled read/forwarding boundaries and eight small policy cases.
Helpers live outside reserved Mixin namespaces; the existing union guard and an owning JAR guard
cover composition. These portable controls are not native qualification: the existing canonical
mandatory population and strict log audit must pass for this candidate before acceptance.

## DeepVoid template producer coordinates

The eight `StructureFeatureConfiguration.structure()` IDs used by pinned `flesh_stuff` are
admitted: `flesh_rib1`, `flesh_rib2`, `flesh_ribs1`, `giant_rib1`, `giant_rib2`,
`fleshy_giant_skull`, `flesh_ribs_eyes`, `flesh_tentacles_new` in `the_deep_void`. The same
whole-template repair also admits the three pinned `broken_bones` producers: `broken_bone1`,
`broken_bone2`, and `broken_bone3new`. Their separate palette admission allows only exact native
OldBoneBlockBlock, BlockOfBoneMarrowBlock and dry RottenGrowthBlock identities (plus AIR/STRUCTURE_BLOCK).
This identifies
the template producer, not arbitrary top-level placed-feature provenance. Other IDs/nonregion
calls forward unchanged once. In the actual `WorldGenRegion`, a typed read-only radius field
must equal the pinned FEATURES constructor's1. The whole native rotated/mirrored bounding box
is minimally translated into the actual center's48-block writable envelope with a one-block
**direct native edge-update** margin (46-block interior). No bounding-box clipping, skipped
native neighbor pass, global write guard or diagnostic suppression is used. Fallout is unchanged.

Every actual loaded palette is inspected through public `Palette.blocks()` (not the mutating
filtered-cache overload): positions must lie inside actual size and the native transform box;
entities must be empty; block IDs/classes must belong to the17 pinned vanilla/DeepVoid choices;
waterlogged states are rejected; NBT is allowed only on actually ignored structure blocks.
Changed bounded overrides with these properties may pass; an ID/raw-JAR hash alone is not
runtime loaded-resource proof. Settings must contain exactly one freshly constructed exact-class
BlockIgnoreProcessor with precisely AIR/STRUCTURE_BLOCK contents for flesh templates, or only
STRUCTURE_BLOCK for broken bones, matching the actual pinned configured features; local pivotZERO,
null BB and flags4 remain required. Unsupported metadata/radius/size/overflow throws a bounded explicit exception before
placement, never a silent false/true success. There is no serializer, template/world cache,
terrain/BE/light query, palette RNG selection or resource-manager I/O in this admission scan.
Typed readonly Shadow-backed views bind explicit named/SRG private-field aliases without adding
a global refmap or changing the historical compilation path; their bridges live outside reserved
Mixin prefixes. Both actual SDK graphs and getter bytecode are pinned by the owning ABI gate.

Safe supported placements retain original position/pivot references. Unsafe placements change
only both native world-position arguments by identical X/Z delta (including their original alias
identity), **not** settings-local rotationPivot or Y. One original placeInWorld call retains the
same template/level/settings/random/flags, Boolean and Throwable; the producer still discards
Boolean and returns true. Original rotation/mirror/offset/palette draws and callbacks remain.
Unsafe relocation intentionally changes terrain context: it is NOT a promise of equal historical
RNG consumption, successful writes or callback results. No height/upgrading fix, global callback
fence or guarantee about arbitrary outside-neighbor callbacks, deferred fluids or other Mixins.

`verifyDeepVoidPlacementControl` and `verifyDeepVoidPlacementAbi` run under `verifyFast` with
nonshipping SDK roles and production archive assertions. Pure/property/recording/ASM controls
and cached actual SDK geometry are not native composition or acceptance; supported shared
full-five-profile qualification and canonical consumer strict audit are still required. The source-only
`brokenBonesFitWholeTemplatesInsideNativeWriteRegion` regression invokes the actual upstream feature
in a real native region from an unsafe origin and checks every translated payload block for all three
bone templates. Its pre-repair negative must fail; it does not suppress native errors or clip the payload.

## Local-only upstream runtime resources

`qualifyUpstreamRuntimeOverrides` is part of `verifyFast`. It fetches only the five exact pinned
originals into a separate immutable input directory, then runs all 17 resource controls against
the current production Compat JAR. Two independently built copies must match; 3,144 recursive
class bytes, untouched compressed records, licenses and scripts remain invariant. The reviewed
recipe changes seven missing `minVersion` fields, three client-only Mixin list entries, and three
obsolete AdPother coremod registrations already replaced by exact-gated production hooks.
It does not repair both emission paths or alter logging/error handlers.

Fresh outputs/receipts remain in `build/upstream-runtime-evidence/`; successful stable support
paths are `build/upstream-runtime-overrides/`. Root source qualification binds those support
hashes, the recipe and actual staged replacement classes before its existing packager assembles
local candidates. Originals, existing `.pw.toml` identities and download caches remain immutable.
These outputs are **local only**: Relics and AdPother declare All Rights Reserved without a found
grant, and other artifacts have notice/source obligations. Neither the recipe nor a receipt grants
public redistribution rights. No corrected third-party binary is committed or publicly hosted.
See [the exact resource/rights contract](src/upstreamRuntime/README.md).

Resource controls and source-native profiles are not exact-candidate client/server acceptance;
Root must still qualify the full real pack and strict logs without exemptions.

## Death worm terrain aliases (source candidate)

Ice and Fire file5633453 has both SRG and official-mapped override names. The terrain replacements
bind exact descriptors for navigation `m_183431_`/`canMoveDirectly`, attack `m_8056_`/`start`,
jump `m_8037_`/`tick` and entity `m_20314_`/`moveTowardsClosestSpace`; the remaining custom method
names and overloaded node paths are also audited. Each of the seven paired field-alias groups
requires exactly its pinned number of sites (15 total per graph), rather than accepting a partial
mapped override subset. Handlers still only return the original intended `BURROWABLE` tag; no
native method body, call/result/order, RNG or terrain-layout algorithm is replaced.
`verifyDeathWormTerrainAbi`, included in `verifyFast`, pins the actual raw/mapped SDK JAR/class
bytes and inspects compiled annotations/handler bodies with ASM, including missing-mapped-alias
and unknown-class negatives. Its two nonshipping ABI configurations add no runtime provider.
Cached JDK/Mixin-AP/ASM controls are not actual native Mixin composition or pack acceptance.

## Registrate construction transactions (source candidate)

The optional production repair targets exact raw/official-mapped **Registrate1.3.3 and 1.3.11**
Base and private Registration class pairs, not a version-only guess. A plugin outside the reserved
Mixin prefix checks bounded original class resources without target loading/initialization:
complete absence is inert; partial or unknown present pairs fail closed. MixinExtras0.5.4
provides required WrapMethod/WrapOperation support. No Create/Ponder/Flywheel runtime dependency
is added by this fixture; the existing default dependency graph already includes Registrate/Create.
Default fixture-disabled staging is NOT target-library absence. Only the resource-only JDK
control currently proves the genuinely absent plugin branch. Root Create's embedded 1.3.3 bytes
match the legacy raw pair; Fruits Delight's JarJar 1.3.11 wins in the actual full pack. The newer
`accept` adds mod-ID logging before the original locked slice; reviewed lookup, transfer,
publication, callback and delegate instructions preserve the admitted transaction semantics.

One eagerly initialized reentrant lock per registrar protects only original construction
transactions: accept's pending.removeAll -> original callback transfer -> registrations.put;
and String-key addRegisterCallback's original table lookup -> pending.put or existing-list
append. Exact private Table.get is wrapped using its public Object return (never private
Registration coercion), and getAll holds the same lock for its original **eager** list collection.
Every original forwarder executes once with original arguments/results; the original maps,
callback values/identity/order, virtual self, and exception objects are retained. Finally cleanup
restores the caller's baseline hold count on exceptional and nested paths.

The original Registration constructor/delegate factory and debug logger run before accept's
OWN acquisition; getDelegate/virtual self run after release. **No lock surrounds onRegister,
onRegisterLate, actual registry creators, consumer/Runnable execution or native diagnostics**;
unused callback WARN/clear/DEV throw remain unchanged. Forge construction must finish before
registry-event dispatch. This is not a repair for background mutation during dispatch, auxiliary
afterRegisterCallbacks/completedRegistrations, all builder state, or arbitrary foreign Mixins.

**Supported callback contract:** original callback hashCode/equals do not perform blocking or
reentrant registry mutations. No whitelist, rejection, prehash, copy or exception substitution
enforces that assumption. Native HashMultimap put and removeAll's removed-set copy may invoke
callback hashing inside the slice. Reentrant hashing can therefore execute a nested constructor
under an INHERITED outer lock even before its own acquisition. That explicit negative is
UNSUPPORTED, not a claim of unconditional deadlock freedom or universally lock-free factories.
The historical exact caller/key interleaving is still unknown: retained actual native iteration
CMEs and a deterministic real-Guava corruption capability are not historical-cause proof.
An unlocked diagnostic projection may still see intermediate changes during one serialized
writer; its CME presence/absence alone is not a post-repair verdict.

`-PwithRegistrateFixture=true` attaches a separate nonshipping source set **only** to gameTestServer
and selects the exact pinned **1.3.11** runtime library, not a copied oracle/provider. Other
profiles retain **1.3.3**, so each invocation has one coherent raw/mapped SDK rather than conflicting
ForgeGradle versions. Two required native tests (83 with the default graph) call an actual protected
Base subclass; each native witness additionally requires both exact official-mapped 1.3.11 class
resource hashes from the active game loader. Read-only native
accessors inspect original pending multimap, table and callback list. A required original-removeAll
gap schedules real callback lookup against transfer/publication; tests require exact callback/
delegate identities, native get/getAll snapshot behavior, original collection retention, and no
creator invocation. A real constructor delegate factory requires another thread's actual getAll
to finish before returning (rejecting whole-accept locking). Original factory and critical hash
exception identities plus subsequent foreign native reads prove cleanup. RegistryObject creation
only installs its original synchronized holder handler/lookup; capture never registers fake items,
mutates a frozen native registry, invokes creators or substitutes a native provider.

Both case witnesses are exact-attempt bound in original native debug.log; the ordinary strict
log/count guards remain required. Default/no-Heat81, no-Bees79 and no-Curios78 populations include the source-only whole-bone
placement regression; every earlier test identity remains required. The fixture classes/accessors/gap/configuration are rejected from all
runtime/source archives. `verifyFast` includes paired raw/mapped method/order pins, compiled
required eight-site/four-original-forward checks and mutation/exclusion/absence/cleanup controls.
These source controls are **not** actual transformed Mixins or native acceptance. The lead must
run the documented fixture-enabled and fixture-disabled/other required profiles after reviewing source;
actual MixinExtras WrapMethod/Inject composition and native fixture gates remain pending.
Production Registrate config is registered in both the archive manifest and global source Mixin
configuration. Local qualification retained in Root fresh-fixes20261023: actual registry-present81,
without-bees77, without-heat79, without-Curios76 and default79 all passed with strict audits.
The authored inventory controls preserve imported five/three-item NBT and separately prove the
pinned Moonlight one-item insertion policy; malformed-tag controls bind the original native
coordinate decoder before asserting lookup preservation. These are owning SOURCE gates, not
historical-cause proof, deployment, Dist or paired native inventory GUI acceptance.
After each successful source launcher, its latest-launch file-backed Mixin
selection/preparation must contain the production config exactly once and the fixture config
exactly once iff requested on gameTestServer. A pre-launch JavaExec.args snapshot is not the
assembled ForgeGradle/MixinGradle argument stream; engine failure remains an original failure.

All eight Mixin configs reserve their complete package descendant union. Plugins and ordinary
helpers live outside it, including urn registration/random scope/generated oracle and ordinary
JUnit controls. `verifyMixinNamespaces` admits only configured classes with actual class-level
Mixin annotations under those prefixes, rejects nested/anonymous helpers, and requires explicit
source/compiled output roles. It never treats a constant-pool descriptor string as an annotation.
The generated oracle root is a builtBy nativeFixture classesDirs entry, not merely output.dir:
ForgeGradle08 actually omitted that latter root from MOD_CLASSES (log442–443). The native
registration callback now requires its exact coordinate in the actual JVM MOD_CLASSES environment
and emits one file-backed witness; post-run validation requires that witness exactly once.
Gradle's pre-launch environment snapshot was null and is not authoritative. Pre-launch RunConfig
checks still reject ordinary-run source exposure. Runtime/source archive guards reject the complete urnfixture tree and oracle
resources. Namespace/module/config wiring remains source-qualified until the next lead native gate.

## Supplementaries treasure urn source contract

Supplementaries **1.20-3.1.43** deliberately returns no `UrnBlockTile` for `treasure=true`.
The native `WorldGenRegion` producer treats every `EntityBlock` state as a BE state and writes
a synthetic pending `DUMMY` into protochunks. Promotion/access then calls the null factory and
warns. The candidate guards **only** `LevelChunk.getBlockEntity` and
`WorldGenRegion.getBlockEntity`, using dedicated required typed Mixins and a generated refmap,
exact registered urn identity plus its actual `TREASURE` property, and pure lookup admission.
The original global `hasBlockEntity()` predicate stays **true**, the original producer still
writes its DUMMY, and the original treasure factory still returns null. This avoids invalid
synthetic promotion without breaking real authored BE registration or later state transitions.
Ordinary `treasure=false` urns and unrelated native BE blocks keep their original results.
No extra inventory tile, alternate loot implementation, logger change, or diagnosis exemption
is part of this candidate. Optional absence is inert; a loaded unreviewed Supplementaries
version or raw/mapped urn-class pair fails closed.

Both nonmutating lookup guards admit only an exact factory-stateless treasure urn with **no
existing live-map entry** and either absent pending data or keys exactly `id,x,y,z`, optionally
`keepPacked`: STRING `id=DUMMY`, matching INT coordinates, optional BYTE `keepPacked=1`.
They return null without writing/removing NBT. Exact `ProtoChunk` and `LevelChunk` direct map
accessors and the exact native `ImposterProtoChunk.getWrapped()` field read are independently
bytecode-pinned; wrappers are admitted only around an exact `LevelChunk`. No materializing
`LevelChunk.getBlockEntity` is called inside a guard. Unknown chunk subclasses and region subclasses keep the native path.
The region guard uses the same cached native chunk lookup and does not catch its exceptions.
Real IDs, extra fields, inventory/loot/owner data, missing/wrong-type/wrong-position coordinates,
and other packed values decline interception. The original decoder/diagnostics remain intact.
Do not interpret declining interception as a migration or arbitrary authored-data guarantee.

**Lifecycle boundary:** independently pinned vanilla lookup removes pending NBT **before**
promotion and its failure warning. The guard instead preserves the exact minimal marker
through repeated lookups; native ordinary save copies it and adds `keepPacked=1`, without
changing the pending original. Native packed reload preserves that marker. Original
`postProcessGeneration` performs lookups and then clears its pending map unconditionally;
that original consumption remains unchanged. There is no restore, deletion pass, postprocess
hook, hidden saved data, or retention promise beyond this native lifecycle boundary.
Although native worldgen treasure urns have no BE, genuine authored treasure BE/inventory tags
must still load/register/save through the original native decoder. The global state predicate
is deliberately **not** repaired: making it false would reject those formerly loadable tiles.
Native controls cover authored treasure inventory loading, full/proto save and reload, both
lookup paths, live-map retention and treasure/ordinary state transitions. Malformed/extra-field
inputs are rejected by the pure admission tests, not decoded with silenced expected warnings.

`verifyTreasureUrnInputs` checks the declared raw pin, FG-mapped original class pair, original
null factory, native loot/Silk Touch/hidden/Fortune branches, producer resources, and real
source/production-SRG SDK selectors and lookup/save/reload/postprocess bytecode. Relevant
SDK classes were also compared independently for Forge 47.4.13 and 47.4.22. The nonshipping
oracle is the pinned FG-mapped upstream class with **only its self name remapped**, plus raw
loot resource bytes. Method-by-method instruction/operand controls reject oracle rewrites.
Three required native cases cover the actual protochunk producer/lookup with unchanged markers
and unsupported-accessor exception forwarding; legacy minimal marker lookup/save/reload/native
consumption plus authored treasure inventory and ordinary inventory save/drop; and seeded
original-versus-actual native loot/Fortune/hidden reroll/Silk Touch/use/comparator/spawn.
Scoped deterministic RNG inputs and actual spawn observation forward the original algorithms;
temporary hidden membership, game rule and spawned entities are restored in `finally`/scope
closure. No test or accessor/oracle class/resource ships. Expected required populations are
80 default/no-Heat, 78 no-Bees, 77 no-Curios. Actual native failed BE promotion remains fatal
to the native-log guard, including its WARN forms; no new acceptable-operation category exists.

**Validation status:** source candidate only. The small in-memory JDK-only predicate/admission
positive/negative controls and independent raw/mapped + SDK bytecode checks passed. Gradle,
AP/refmap generation, compiled-hook/NBT/oracle JUnit and all three native cases remain for
the lead's canonical build and runtime validation; none are claimed as executed acceptance.

## OC2R fallback echo payload ownership

With OC2R **2.2.12**, the native session method consumes its caller buffer into an owned
byte array before choosing native ICMP or JVM reachability. Upstream accidentally captures
the exhausted, reusable caller buffer in its asynchronous JVM branch, losing the echo
payload. A required, exactly-one-site Mixin substitutes only the local buffer reference
with a read-only view of that already-owned array after original consumption. Caller
position/limit, the native byte-array/length path, original executor, TTL/timeout, actual
reachability result, response publication and diagnostics remain native. No caller storage
survives the synchronous method. Optional absence is inert; a loaded unsupported version or
actual target-class hash fails closed, including in development (the pinned class bytes
are unchanged by ForgeGradle mapping).

Pure snapshot/compiled-injection controls live here. The Controls repository owns the
nonshipping transformed-native regression fixture and declares the canonical Compat
provider input: held-original-task/caller-reuse echo assertions, native TCP/UDP loopback,
and complete native filesystem-load proof. No upstream archive or logger is changed.
The source is authored but uncompiled/unrun; pinned source checks are not native acceptance.

## Exact Born in Chaos caravan jigsaw anchor repair

With pinned Born in Chaos **1.7.5**, only the LEFT resource key
`born_in_chaos_v1:clown_caravan` in vanilla `SinglePoolElement.getSettings` receives an appended,
registered unit-codec entity processor on a settings copy. Existing settings/processors/order/RNG
and original resources remain unchanged. Only an actual `minecraft:item_frame` with three typed
INT TileXYZ anchors gets copied NBT anchors set from its already-processed WORLD block position.
Other NBT/positions/entities are untouched. Inline templates and other resource keys do not match.
The separate native caravan item-use procedure is **not covered**. Source/API/NBT and generated
mapping controls do not establish transformed placement, hanging-frame survival, or rendering.

## Cosmetic colonies and ingredient-first EMI

With pinned Complicated Bees **1.20.1-3.4.6**, occupied native nests (valid species) and working
native apiaries emit miniature client-only bee particles. Working means a native queen, native
`isQueenSatisfied()` and an empty output buffer—the same production eligibility, not a new
simulator. Apiaries retain the legacy activity bit and add strictly versioned primitive
active/known/body-RGB metadata, sent only on effective transitions. The existing expressed-effect
chromosome return supplies body color through pinned pure field/map getters: no extra genetic
decode, inventory/world query, or retained native object. Native queen-change notifications and
removal clear appearance. Nests use their already-synchronized native `Species.getColor()`, never
nest/honey tint. Unknown appearance does not invent a color. No inventory/genome/production
payload, bee entities, genetics, yields, storage or production changes.

The original particle/texture implements a short outbound/return excursion. It was informed by
Forestry's pinned `72e15b0f08daa25d8293cf28b5d5e1da200f9161` `ParticleBeeExplore` /
`ParticleBeeRoundTrip` behavior and LGPL-3.0 license, without copying code or assets.
The legacy `colony_bee` SimpleParticleType remains unchanged. New `colony_bee_tinted_v1` options
carry strict versioned RGB24. One particle renders a white four-pixel body mask plus the unchanged
11 brown-marking and nine wing pixels as two quads, preserving the original silhouette/path/RNG
and lifetime. Source/pixel controls are not actual-frame acceptance.
Client `complicatedBees.swarmDensity` is 0–4 (default 2; 0 disables). Minimal particles disables
emission; Decreased halves the budget. Every ten unpaused client ticks, at most eight emission
requests are admitted within 24 blocks, with an 80-tick lifetime, an additional 128-outstanding
request cap, and a 512-iterator-operation scan budget over at most 25 already-loaded chunks.
Persistent round-robin chunk/entity cursors prevent unrelated dense chunks or >512 colonies
from permanently starving later sources. Unloaded/replaced chunks lose their cursors; no chunks
are loaded to find colonies. No continuous flower search, entity pathfinding or per-particle
network packet is introduced. Emission counters are requests, not proof of rendering.

With pinned EMI **1.1.24+1.20.1+forge**, each new native recipe screen snapshots
`EmiPlayerInventory.of(player)` and uses its greedy `canCraft(recipe, 1)` matcher. Recipes with
inputs for one execution come first within each category; categories with any available recipe
come first, with EMI's category order as the tie-breaker. Both partitions are stable, copied,
and applied before native pagination. Resizing/browsing does not resnapshot or reshuffle.
Generic opening starts at the first promoted result instead of restoring the previous category
or applying the bill-of-materials preferred recipe; explicit recipe/focus requests remain native.
Workstation/catalyst/process feasibility and native filling/crafting authority are unchanged.
Native greedy overlap false negatives remain possible; this is not a new exact allocation solver.

Inventory sources are exclusively EMI's existing exposed native sources: vanilla/Curios personal
inventory (with or without an equipped backpack) and separately opened Sophisticated menus retain
their independent handlers. There is no unopened/nested-bag search, storage handler, transfer engine,
or global recipe-manager mutation. Optional absence is inert; unexpected present versions fail
visibly and require API revalidation. `IngredientOrderSnapshot` exposes immutable observations of
actual native tab/list/page state, not a mutable screen replacement.

`verifyFast` checks exact upstream archive hashes/API sites and stable partitions/fair bounded scans.
`verifyFull` adds transformed-native colony tests; `-PcolonyWithoutBees=true` exercises actual optional
absence. These local gates do not establish the paired native personal-inventory/separate-backpack
client behavior, rendering, visual density, or representative gameplay performance. Those require
same-candidate native probes and actual framebuffer review under the shared testing policy.

Nonflower offhand block items cannot be placed through ordinary right-click use. A common-side
Forge interaction listener denies only that `BlockItem` item-use leg, on client and server.
Decorative flower blocks in the Cultivars-owned `better_bumblezone_crops:decorative_flowers`
block tag are exempt, preserving native placement from both hands, substrate/clearance checks,
and item consumption. This is not a forced admission and does not override other denials.
Main-hand placement, clicked-block interactions, and non-block offhand items remain unchanged.
Items remain carryable in either hand. This does not prohibit non-BlockItem tools such as buckets
from changing the world, nor command/automation placement. Native GameTests cover denied block/torch
placement without consumption, ordinary main-hand placement, and interaction admission controls.
Additional source/GameTest assertions cover both-hand flower placement, invalid support, tall-flower
clearance, petal quantities, nonflower/crop/azalea negative controls, and preservation of other denials.
The nonshipping native fixture supplies only four declared vanilla representatives of the canonical
flower tag; the Cultivars Python contract checks that subset against the full authored classification.
These new assertions are uncompiled/unrun; full-pack client acceptance requires a separately authorized run.

Non-Creative player mining of exactly `minecraft:spawner` requires the main-hand stack's native
correct-tool-for-drops capability. The pack's additive `needs_diamond_tool` block tag supplies
the diamond-or-higher threshold, including for Tinkers tools. Unsuitable tools are denied at
left-click, break-speed and authoritative block-break events; Creative bypass remains native.
This does not add collection, alter hardness/loot/spawner data, grant Adventure/Spectator
permission, or intercept independent explosions, spells, machines, movement or extraction.
Fake-player breaks traversing player events receive the same check. The nonshipping native
fixture loads the exact pack tag and tests vanilla/Tinkers stacks and actual player removal;
these source tests do not establish exact-candidate full-pack acceptance.

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

Dynamic Trees **1.20.1-1.4.10** player felling multiplies each existing nominal durability
charge by `dynamicTrees.fellingDurabilityMultiplier` (COMMON integer, default **10**, range
1–100; **1** restores upstream wear). The ordinary axe mining charge and deferred Dynamic
Trees surcharge are scaled separately before native mitigation, preserving their dispatch order
and the native thickness rounding/subtraction. Tinkers' native harvest path does not pay its
ordinary charge when Dynamic Trees queues removal and returns false, so only its actual
additional charge is scaled; zero-cost thin branches remain zero-cost. No minimum-durability
check is added. Tool modifiers, creative immunity, unbreakable/broken tools and break callbacks
remain native. Ordinary logs, stripping, combat, FakePlayer automation and non-axe mining are
unchanged. Player/held-stack identity scopes are synchronous, restored in `finally` on exceptions,
and never retained across the deferred tick. No saved-world server configuration is rewritten;
the pack keeps `THICKNESS`. Arithmetic/scope and pinned-bytecode contracts are Minecraft-free;
transformed-native and full-pack gameplay acceptance require separately authorized runs.
The native wear fixture isolates copied in-memory Forge configs in a dedicated batch and restores
exact original objects, effective values and unchanged backing-file bytes. A required nonshipping
read-only Mixin observes the actual multiplier returned to native calculations; wrong/missing
observations reject, and deliberately different final temporary values test restoration. All original
radius/tool/multiplier arithmetic and break/immunity assertions remain intact; production config
reload policy and production artifacts do not include the observer.

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
Each source native invocation binds one fresh `build/gametest/<UUID>` directly to Forge's
RunConfig, structures, setup and audited sink. Legacy fixtures and ordinary/visual runs stay
untouched. Absent-only CREATE_NEW settings preserve any existing property bytes exactly. The
full gate requires one completed/passed native count (default80, no-Bees78, no-Heat80,
no-Curios77) and rejects ERROR/FATAL or mixin failure in actual latest/debug logs. Pure negative
controls reject missing/stale-count/duplicate/failed sinks; source passes still require an admitted
full invocation for each profile, default last. Build-tree retention is required before `clean`.
The native preparation also uses an immutable outside-build snapshot in
`gradle/native-default-configs-v2/` of exactly three tracked pack inputs: `config/adpother-common.toml`,
`config/forgeendertech-common.toml` and `config/sophisticatedcore-common.toml`. The historical two-file
snapshot remains retained at `gradle/native-default-configs/`, outside active preparation. Pinned
SHA256/HEAD-blob provenance is checked against `../../better-content-modpack` before launch. Original
Root HEAD is provenance, not a whole-repository version lock: unrelated Root commits are accepted
when all three tracked blobs and working bytes remain approved; a changed committed config fails.
The first two match the SDK's supported generated defaults byte-for-byte. SophisticatedCore uses the
exact production configuration, including thirteen intentionally disabled copper/jukebox identities;
it is not copied from a warm native run. Its predeclared entries cover all currently observed native
item-enabled conditions. Chipped-only recipe conditions short-circuit while Chipped is absent;
new integrations still require input and runtime requalification. No Sources.cfg/emission settings
or warm generated inputs are imported. Missing,
untracked, modified, extra or mismatched inputs fail closed. CREATE_NEW never overwrites existing
bytes; differing existing destinations are retained and rejected. A source/destination before/after
receipt is written to the actual UUID root as `native-default-config-inputs.json` before native
launch. `verifyNativeDefaultConfigInputs` runs the retained Minecraft-free Python controls before
`runGameTestServer`. These authored inputs complete first-start configuration without disabling
watchers, masking diagnostics, changing production code or claiming a cure before all four fresh
gates pass. Neither the snapshot nor its preparation script is part of the production JAR.

### Pending chunk-read ownership

Vanilla's pending `IOWorker.loadAsync` branch exposes the same mutable root that the mailbox will
serialize. `ChunkMap` asynchronously upgrades that result, and `ChunkStorage` inserts/removes root
`__context` even for current data versions. The required `chunk_io` mixin copies only that pending
field read, on the mailbox thread before future completion. Disk reads, original futures, write
ordering, deletion sentinels and exception propagation are unchanged. Callers must still transfer
ownership when calling `store`; this is not protection against arbitrary post-store caller mutation.
The exact external mutator/interleaving of the retained C41 save failure remains unknown.

Four JUnit controls cover null, deep-copy isolation and a deterministic actual-NBT serialization
interleaving: the old shared root fails with one `CompoundTag.write` frame, while the detached
reader preserves the writer. One additional required native case uses a test-owned worker and two
foreground mailbox barriers before background flush, then checks read isolation, persisted bytes,
null deletion and missing chunks. It adds one case without removing any existing native case.
`verifyChunkIoOwnershipArtifact` requires the shipped fail-closed config, exact generated method
and field SRG mappings, native copy binding and absence of nonshipping actor helpers. These
controls establish only their executed scope; fresh profile and same-artifact pack acceptance remain
required. No save/config diagnostic is waived.

A separate default-off, nonshipping Rooty-soil witness is prepared under `src/nativeFixture`.
Only an explicitly admitted source `gameTestServer` invocation may opt in with
`-PnativeRootySoilWitness=true`; ordinary client/server/visual/data runs are unchanged. It observes
actual DT1.4.10 unsupported-soil locals and original fallback `setBlock` arguments/result once,
without world/biome queries, RNG, branch logs, NBT or retained native objects. It preserves the
WARN, fallback and original false return/caller continuation; it is not a soil repair. Bounded
scalar evidence is CREATE_NEW-written after server stop in the actual `build/gametest/<UUID>`.
After JVM exit, `python3 -B scripts/rooty_witness.py --witness <UUID>/rooty-soil-witness-v1.json
--debug <UUID>/logs/debug.log --latest <UUID>/logs/latest.log` reconciles complete original logs.
Missing locals/context, overflow, double-forwarding, unfinished calls or mismatched warnings fail
closed. Its result is diagnostic-only: actual failed soil operations still reject qualification,
even when Native counts are green. All archive tasks exclude/check both runtime and source
fixture paths. Source/API controls are not transformed-Mixin/Native/feature acceptance; a new
explicit grant, retention, exact closure and all-severity/manual review remain required.

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

Deep Void entrances now use the registered `better_compat_fixes:deep_void_entrance` structure and
`deep_void_entrances` structure set: 128-chunk spacing, 64-chunk separation, equal selection among all
18 inspected native temple, ruin, platform and marrow-hole templates (including upstream revisions).
A saved piece anchors native decoration to the actual minimum build height and adds a separate
lined 3×3 bore, a surrounding floor/chamber and an approach into the decoration. All writes are
chunk-clipped; each placement uses private settings, filters authoring structure blocks, preserves
explicit native air and ignores saved entities. Native loot/spawners are retained. A dimension guard
excludes even other dimensions sharing Overworld biomes. Entrances are searchable using
`/locate structure better_compat_fixes:deep_void_entrance`. New starts do not retrofit old chunks.

The pack disables the competing one-time origin temple with `PlaceCultTemple = false`. Its optional
height compatibility remains: both native templates and their delayed block cleanup use the actual
bottom. The modern delayed cleanup removes a saved authoring structure block, not the main hole.
Existing origin temples and the native placement flag are not migrated. Abductors are unchanged.

Carried ordinary compasses below Y=0 in the Overworld use a server-selected nearest entrance start
within an eight-region square search radius; Y≥0, other dimensions, lodestone/recovery compasses,
item frames and absent targets retain native behavior. The client receives only a transient target;
no item NBT is changed. Candidate order follows the registered placement seed/spacing, then confirmed
saved/generated starts, skipping old chunks without entrances. Search requests are asynchronous and
limited to one globally in flight / one new request per ten ticks, exclusively at `STRUCTURE_STARTS`:
structure metadata may be created, but no terrain/full chunks are requested. A 512-entry start cache,
32-block movement refresh, five-minute target refresh and lifecycle/data-reload invalidation bound
work and stale targets. Native tests cover registered start generation, cross-dimension rejection,
all variant bores at -128, saved pieces, clipping and compass policy. These checks are not actual
client needle/packet or full-pack exploration acceptance.

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
Shift+Caps Lock; Epic Fight and ParCool direct dodge are unbound in pack defaults (a direct
ParCool dodge key can still be assigned in Controls), and
vanilla's forward double-tap sprint behavior is left intact. Double-tap a physical arrow key
within seven client ticks (about 350 ms) while holding physical Left Shift to request ParCool's
dodge in that direction while not crouching. Right Shift does not enable the shortcut. Movement-key rebinding does not move this shortcut onto WASD. Menus, focus loss,
pausing, crouching, releasing Left Shift and player/world changes clear pending taps. ParCool retains its native
stamina, cooldown, grounded-state and action restrictions; no velocity or key state is injected.
Independent arrow requests do not participate in Epic ParCool's direct-key collision guard:
in combat mode, two unbound dodge bindings otherwise compare as the same key and cancel
ParCool dodge when an Epic Fight dodge skill is equipped. The narrow expression hook keeps
native direct-key collision handling and Epic Fight's subsequent stamina check intact.
`EpicParCoolDodgeKeyConflictTest` executes the hash-pinned native guard with deterministic input
doubles for shared/unbound/distinct keys, equipped/absent skills, stamina and creative exemption.
This is not a Forge client/input verdict; the bridge remains pending real-client acceptance.
Arrow requests also feed Epic ParCool's native roll-animation selector with their direction instead
of unrelated vanilla WASD impulses; no vanilla input state, animation implementation or native
stamina deduction is changed. Real-client acceptance remains pending.

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

## Geological ordinary admission and causal offspring API

**New source/tests are authored, not compiled or runtime-qualified for this change.**
Better Cave Encounters consumes these common APIs; no hard dependency on a creature mod
or the Director is added here. The pack's complete populations and tuning live in
[`docs/dimension_ecology.md`](../../better-content-modpack/docs/dimension_ecology.md).

`GeologicalAmbientEligibilityEvent` defaults to no exception. A positive answer can waive
only native geological habitat selection and the migrated legacy veto, never difficulty,
darkness, physical suitability, player distance, native caps or independent protection
results. The exact former twenty-eight selected-mob IDs are pack-owned optional entries in
`better_compat_fixes:legacy_underground_ambient_denied`; Overworld NATURAL attempts at Y<=62
remain denied without an eligible answer. Surface reserve/tagged-ground and vanilla zombie/
skeleton denials remain independent. Compat never forces PositionCheck ALLOW.

`NaturalSpawnContext` marks only vanilla's selection, placement and position-admission calls,
with nested exception-safe stage restoration. Finalization and native offspring do not inherit
that authority. External spawners can also use `NATURAL`: notably In Control's pinned lava
Magma Cube spawner does so, and is deliberately not reclassified. A narrow vanilla membership
adapter permits fresh geological SpawnerData for an eligible type still present in the final
post-event list; cancelled/empty lists or independently removed types still reject it.
Non-MONSTER categories are outside the replacement contract.

`CausalMobSpawns`/`ActorScopes` track the immediate producer through server entity/passenger
ticks, supported damage/death, vanilla slime removal and conversion. Optional guarded hooks
cover Ice and Fire Dread kill conversion and TConstruct's separate armored-slime split loop.
Nested unowned actors mask enclosing owned actors, and every scope unwinds in `finally`.
Actual insertion runs as the child, so insertion callbacks do not retain its parent's identity.
There is no proximity/species/reason-based lineage inference.

Cancelable `MobOffspringAdmissionEvent` exposes parent/child and completion callbacks tied
to actual insertion success, including veto/failure. All subscribers complete even when one
throws, so one consumer cannot skip another's commit/rollback. The Director owns finite
prepaid credits, provenance, capacity admission and SavedData; Compat does not rewrite native
child chance/types, attacks, rewards or terrain. Unowned populations remain unmarked.

Authored tests cover pure scope/callback restoration and nonshipping native insertion/veto,
unowned-victim and slime paths, plus narrow legacy-veto/surface negative controls. The native
legacy tag is an exact current-pack fixture copy, with pack contract parity and declared
verification inputs, not a harvested runtime baseline. The Director's separately declared
25-pin `verifyWorld` fixture covers the twenty additions and all supported reproductive roster
actors. Neither these unrun checks nor archive inspection establishes full-pack acceptance.
The existing Death Worm burrow tag/mixin and natural weight-zero policy are unchanged.
Prepare the newly verified Compat provider explicitly before compiling the Director consumer;
no old staged provider is silently replaced by this source work.

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
