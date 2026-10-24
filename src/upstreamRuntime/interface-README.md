# Independent local interface/side compatibility corrections

This **separate** `bc-interface.1` recipe does not modify `overrides-v1.json`, its builder,
its five outputs, or its 3,144-class byte-invariance contract. The original pipeline must
continue to pass all 17 controls. Root binds this additional recipe independently through
`config/better-content-interface-runtime-overrides.json` and its separate prepared pointer.

## Exact scope

- Epic ParCool's `ParCoolMixinAnimation` accessor targets a ParCool class annotated CLIENT
  and returns the client Animator. Its sole addon consumer is a client event. Move only
  that accessor from common to client; every class byte remains unchanged.
- Valkyrien Skies' `MixinChainConveyorInteractionHandler` has only `clientTick` and
  `drawCustomBlockSelection` handlers using ClientShip/Minecraft/PoseStack. Move that one
  registration from common to client. Keep all other registrations/conditions unchanged.
- The exact VS `MixinVibrationSystemTicker` interface contains three PRIVATE STATIC
  WrapOperation handlers. Active Forge Mixin 0.8.5 rejects them during interface preparation,
  despite the original JAVA_17/minVersion0.8 declarations. Change **only** their three
  access words from `0x000a` (PRIVATE|STATIC) to `0x0009` (PUBLIC|STATIC):
  `destWorldPos`, `destSourcePos`, `destReloadSourcePos`, with exact pinned descriptors.
  Do not make them default/instance methods: the third invokes the second with INVOKESTATIC.

The original class SHA256 is
`399ca37359fbbf851eb54f5e3524c59a25751998cae0c9f534bb68a45f56659f`;
the resulting class SHA256 is
`9d7f1f34ce0446107bbb58ec91bbbd03e8912dc3057b1f514954506229491359`.
A bounded class parser derives the offsets (3686, 3891, 4209) independently of supplied
receipt offsets. Comparing the entire expected class proves every other byte unchanged:
constant pool, descriptors, annotations, Code/max stack/locals, original instructions,
exception tables, stack maps, static delegation, and all other attributes. No bootstrap
library is replaced. All other recursive class bytes, resources, notices, ZIP metadata,
entry ordering and untouched compressed records remain identical. This covers 8,062
recursive classes in these two originals; just one class changes three low access bytes.
Unknown class/body/annotation/descriptor/static/method changes fail closed.

Relics' common Item Mixin is **not** moved: it includes real server cooldown and registration
code. Its client tooltip helper's external InputConstants.Key InnerClasses metadata can be
eagerly inspected by old Mixin. That observed WARN is not a justification to delete common
behavior, add an exemption, or claim a client/server runtime result.

## Qualification and retention

`qualifyUpstreamInterfaceOverrides`, under `verifyFast`, fetches exact original SHA1/SHA256s
into a fresh retained directory, runs 12 actual-original/pure controls, and builds twice
with byte-identical outputs and receipts. Offline entry points:

```sh
python3 -B scripts/build_upstream_interface_overrides.py --inputs ORIGINALS --output FRESH_OUTPUT
python3 -B scripts/test_upstream_interface_overrides.py --inputs ORIGINALS --evidence FRESH_EVIDENCE
```

Downloads are a separate explicit `--fetch-originals FRESH_DIR` operation. Original `.pw`
pins and all existing download caches are immutable. Outputs have separately named
`-bc-interface.1.jar` identities. Attempts/originals are retained under
`build/upstream-interface-evidence/`; the three successful support files are under
`build/upstream-interface-overrides/`. Root's receiver independently checks source hashes,
original pins, complete receipts, exact JSON moves, class access words and all remaining
bytes, then seals evidence. Root's sole `package.sh` assembler applies old five first and
new two second, with one normal Packwiz export. Both exact candidate ZIPs must match.

Receipts say `runtime_acceptance:NOT_EVALUATED` and
`publication:NOT_AUTHORIZED_BY_THIS_RECEIPT`. These controls alone do not prove that every
newly admitted handler executes correctly on both sides. Actual fresh full-pack startup,
join, heartbeat, teardown and strict raw-log acceptance remain mandatory.

## Rights

**Local-only; do not publish these binaries or resulting pack ZIPs under this receipt.**
Epic ParCool declares All Rights Reserved and no grant was established. VS declares
Apache-2.0 and includes its full license; bundled component terms/notices remain intact.
The existing Relics/AdPother ARR boundary still applies to the combined pack. Passing
controls and retaining licenses are not a general redistribution grant. Source patch
specifications/recipes may be reviewed separately; corrected archives are retained local
outputs, never uploaded by these tasks.
