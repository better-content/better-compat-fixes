# Pinned client reload compatibility repairs

These hooks are client-only and use the existing exact-version plugin gates. No
logger filters, exception suppression, resource deletion, or feature disablement
is involved. The source tests resolve raw upstream archives, independently check
SHA-256, and inspect class bytecode with ASM.

| Archive | CurseForge project/file | SHA-256 |
| --- | --- | --- |
| Ice and Fire 2.1.13 (1.20.1 beta-5) | 264231 / 5633453 | `2b80245fc9b7d6fdc61d71f9892f4c6114eb7f303f65634845aaab25f84d1e82` |
| Distant Horizons 2.4.5-b | 508933 / 7375280 | `e29c5d6a47ce40ec174f96104ac3639605de30e3811b39907de7070feb230cc2` |
| Epic Fight Tinker Compat 20.2.8 | 1491531 / 8684759 | `e8929d00d44efbff0811810f15faa31be07e53f7073a5b068638e07690670f49` |

## Dragon eyes

`LayerDragonEyes` passes three mixed-case `_Ground` paths to
`TabulaModelHandlerHelper.loadTabulaModel(String)`. The helper prepends `/` and
appends `.tbl`, then calls **ClassLoader**, not Class, `getResourceAsStream`.
The archive contains relative lowercase `_ground.tbl` entries. The hook changes
only those three exact final arguments. All other arguments and all stream/model
parsing behavior remain upstream-owned. Tests open each actual repaired ZIP model
through an isolated URLClassLoader and require its `HeadFront` geometry.

## DH level-dependent tints

`ClientBlockStateColorCache.getColor` invokes the inherited four-argument
`AbstractDhTintGetter.update`, not `TintGetterOverride`'s five-argument overload
which initializes `parent`. Therefore its thread-local getter has no LevelReader
when modded callbacks query block state, light, or height.

A tail hook on the inherited update sets the level-dependent getter's parent from
that invocation's `IClientLevelWrapper.getWrappedMcObject(): Object`. The concrete
client wrapper's bridge returns its final ClientLevel field. This is a context
fallback, not a lookup of the mutable global Minecraft level and not reuse of an
old dimension's parent. It refreshes every update and leaves the level-free fast
path untouched. The explicit five-argument overload calls super before writing
its own parent, so explicit callers retain precedence. Invalid/null wrapper
objects fail normally rather than converting the tint to a fallback color.

## Optional synthesized item skins

Epic Fight Tinker Compat's `MixinItemSkinsReloadListener` merges a private,
non-Unique `addTinkerWeapon(Map,String,DDDDDDI)` helper into the real Epic Fight
listener. Its short overload delegates to this one. Eleven default additions
include two unconditional Constructs Casting entries even when those items are
absent. A cancellable HEAD hook skips synthesis only for those two exact IDs when
the real Forge item registry lacks them. It never removes an authored map entry;
registered optional items and all nine other defaults take the original path.

The hook targets the **listener**, not the mixin class. Priority 1100 is above
upstream's default 1000: Mixin completes all MAIN method merges before injection
preparation, and its merged-method priority predicate requires the injector's
priority to be greater. Required count 1 fails closed on helper ABI drift. Both
Epic Fight 20.14.17 and Tinker Compat 20.2.8 are required by the plugin gate.

## Verification boundary

`ClientReloadCompatibilityTest`, `DhTintParentContextTest`, and
`ClientReloadNativeContractTest` supply eight behavior/negative/archive controls.
The DH controls exercise the production refresh helper with ordinary context identities,
prove the level-free supplier is never invoked, and test invalid context propagation
through the actual bridge. Compiled ASM checks verify the bridge's typed parent write,
nonnull requirement and the mixin's Object-returning wrapped-world call. They use no
reflective access, synthetic world, game bootstrap, or world query.
Pure javac/JVM controls and hook compilation are not native Mixin-application,
visual dragon-eye, asynchronous world-read, or full Dist acceptance evidence.
Those remain part of the lead-owned fresh client/server qualification.
