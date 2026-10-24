# Local upstream runtime resource corrections

This source-only recipe belongs to Better Compat Fixes. It deterministically creates **local**,
separately named `-bc-runtime.1.jar` replacements from five exactly pinned original archives.
It does not build this mod, launch Minecraft, package a pack, deploy, update Packwiz or publish
anything. It is not an alternate pack assembler. Root owns qualification, evidence-local support
staging, candidate bindings and use by its existing packager. Never modify an existing upstream
pin, cached original, earlier output, or retained receipt in place.

`overrides-v1.json` is the complete versioned recipe and immutable upstream identity inventory:
original filename, SHA-256/SHA-1, CurseForge project/file, exact pinned download URL, nested archive
and resource SHA-256, expected old JSON structure and the one permitted operation. The large
AdPother registration map records the three exact touched old key/value pairs and pins **the
entire old resource bytes** by SHA-256; its unrelated map entries are preserved, not regenerated
from a reduced whitelist. This is not a general-purpose resource-normalization mechanism.

## Permitted corrections

- Forgified Fabric API: add `minVersion: "0.8.5"` to the five specified item API, item-group API
  and data-attachment API configs. Move Screen API `MouseMixin` and `ScreenAccessor` from the
  common list to the client list. Keep the two original client mixins and every other setting.
- EMI Loot and YACL: add `minVersion: "0.8.5"` to the two specified configs. This declares the
  required Mixin baseline; the supported Forge runtime uses a newer implementation. Native
  qualification still establishes actual injection behavior, not this metadata declaration.
- Relics: move only `ScreenMixin` from common to client, preserving original list order and
  existing client mixins. Its existing `minVersion: "0.8"` is **not** changed.
- AdPother: remove only `coldsweat$hearth`, `littlelogistics$steam_locomotive` and
  `littlelogistics$steam_tug` from `META-INF/coremods.json`. Leave the corresponding scripts,
  shared coremod helpers, error path, logger and all other registrations byte-identical.

C2ME is deliberately outside this recipe. Its dedicated-server client-only module can use the
existing supported server configuration override instead of an artifact correction.

### Mandatory AdPother replacement condition

These three upstream selectors no longer match Cold Sweat **2.4** and Little Logistics
**1.20.1.2**: Hearth `drainFuel()V` now calls `drainHotFuel(IZ)V`, not `setHotFuel(IZ)V`; both steam
vehicles' `tickFuel()Z` now call `FuelItemStackHandler.tryConsumeFuel()I`, not `ItemStack.shrink`.

Removing registrations is valid only together with the existing Better Compat Fixes production
pollution replacements for AdPother **8.1.49.0**:

- `adpother.coldsweat.HearthBlockEntityMixin`: report the actual consumed hot-fuel delta;
- `adpother.littlelogistics.SteamLocomotiveEntityMixin` and `SteamTugEntityMixin`: wrap actual
  fuel consumption and report exactly one consumed item after success.

The builder requires an explicit `--compat-jar`. It verifies the three exact production class
hashes, their registration exactly once, and the pinned production plugin class containing the
version gates. The existing main config has `required: false`; that original setting is checked
and retained, **not** misrepresented as a mandatory Mixin config. The replacement injectors'
existing `require=1` bytes are hash-bound. Structural admission does not prove native application,
config activation, successful fuel consumption or absence of double emissions. Root's native
qualification must cover those conditions with the staged provider and the actual pinned target
mods. A new provider must retain these class/plugin bytes or receive a reviewed recipe revision.
Do not repair/re-enable the three original coremods while retaining these replacement emissions.

## Commands

From the owning repository, use explicit original files rather than historical output discovery:

```sh
python3 -B scripts/build_upstream_runtime_overrides.py \
  --inputs /absolute/original-archives \
  --compat-jar /absolute/qualified/better-compat-fixes-0.1.9.jar \
  --output /absolute/fresh-local-runtime-overrides
```

`--inputs` is a directory containing the **original filenames** in the spec. It may be an explicit
retained fixture or an original-archive cache directory. The builder is offline by default and
validates both original digests, every modified resource digest, exact old structures and each
nested parent digest. It will never infer inputs from receipts or silently fetch missing files.
Inputs and outputs must be disjoint trees. Output must not exist and its parent must already
exist. Every file is created exclusively; original files and old outputs remain untouched.
Validation completes before an output directory is created. A subsequent I/O failure leaves
partial output retained; use another fresh directory rather than cleaning or overwriting it.

Optional, separate fresh preparation (network opt-in, never an existing cache):

```sh
python3 -B scripts/build_upstream_runtime_overrides.py \
  --fetch-originals /absolute/fresh-original-archives
```

Only the exact spec HTTPS CDN URLs are requested, with a finite per-request safety timeout and
size cap. Redirects outside the two Forge CDN hosts are rejected; both pinned digests must match
before a downloaded file is written under its original filename. A failed fetch retains its new
directory and any preceding valid inputs. The CLI emits a machine-readable fetch result; callers
may retain stdout beside their own evidence. URL availability is not a promise of third-party
service uptime; independently supplied bytes with the exact hashes are equally valid.

No corrected JAR is added to source control or uploaded. The source recipe can be reviewed
independently of generated proprietary binaries.

## Determinism and safety boundaries

The builder copies untouched compressed local ZIP records verbatim and preserves local and
central entry order, timestamps, filename bytes, flags, compression methods, attributes, extra
fields, archive/entry comments and original data-descriptor conventions. Only changed JSON
payloads, their CRC/sizes, affected nested archive payloads and necessary central/EOCD offsets
change. Changed JSON is UTF-8/ASCII-compatible, two-space indented with a final newline; modified
deflate payloads use level 9. The receipt identifies Python and zlib versions. Byte reproducibility
is established for identical originals/recipe/runtime; cross-zlib compressor-version identity
is not claimed. Root must bind actual produced hashes rather than trusting a filename.

All classes, licenses and other unchanged entry bytes are retained. Every recursive class is
fingerprinted before and after. Each affected archive is independently compared against its
original entry map with only the declared resources replaced. Ordinary detached signature files,
`SIG-*` entries or manifest digest attributes cause rejection, including inside untouched nested
JARs; nothing strips signatures. Supporting signed archives would require a separate explicit
review and implementation, not a bypass flag.

The implementation rejects unsafe/duplicate archive names, symlinks, duplicate JSON keys,
nonfinite JSON, invalid old structures/types, wrong hashes, missing/duplicate/conflicting patch
paths, encrypted entries, unsupported compression, ZIP64/multi-disk/self-extracting/padded ZIP
layouts and invalid CRCs. Limits cover archive size, entry size/count, total expanded/recursive
inventory bytes, JSON size and nesting depth. Filesystem paths reject symlink ancestors and final
symlinks; files use no-follow reads/exclusive writes. This is a local single-user build utility,
not a security boundary against concurrent hostile replacement of ancestor directories. No
source/cache/output deletion is performed.

## Receipt contract

`receipt.json`, outside the JARs, uses `bc.upstream_runtime_receipt.v1`:

- `revision`, `spec_sha256`, `builder_sha256`, `python`, `zlib`;
- `compat`: consumed whole provider SHA-256, replacement class pins, explicit condition and
  `runtime_behavior: "NOT_EVALUATED"`;
- `artifacts`: each original/output basename, `input_sha256`, `output_sha256`, `output_sha1`,
  modified resource paths and before/after SHA-256, recursive `class_count`,
  `class_inventory_sha256`, `class_bytes_invariant`, license assessment and redistribution status;
- `runtime_acceptance: "NOT_EVALUATED"` and
  `publication: "NOT_AUTHORIZED_BY_THIS_RECEIPT"`.

Receipt existence is not qualification. Root must revalidate recipe/builder/provider/original/
output bindings, preserve native evidence, and use its ordinary same-candidate runtime gates.
A local generated distribution containing these files is not authorized for public redistribution
by this recipe, by a successful test or by a receipt.

## Focused controls

Pure, Minecraft-free and network-free controls:

```sh
python3 -B scripts/test_upstream_runtime_overrides.py
```

Explicit original-artifact qualification adds two byte-identical actual builds and unchanged
original/resource/class checks:

```sh
python3 -B scripts/test_upstream_runtime_overrides.py \
  --originals /absolute/original-archives \
  --compat-jar /absolute/qualified/better-compat-fixes-0.1.9.jar \
  --output-root /absolute/existing-evidence-parent
```

Controls retain fresh temporary directories under `/home/dev/.tmp` by default, or the explicitly
selected evidence parent. No cleanup deletes positive outputs or negative evidence. The tests
cover pre-repair missing metadata/selectors, positive changes, raw compressed-record and ZIP
metadata preservation (seekable and data-descriptor formats), nested multi-resource corrections,
class/license invariance, whole original hash rejection, wrong old values/types, duplicates,
unsafe paths, CRC/size/count/expansion limits, signatures, mandatory replacement provider checks,
fresh outputs and mocked fresh-input fetch/hash/origin rejection. These are source/artifact
controls, not Forge startup, gameplay, client visual or release acceptance.

## License assessment — no publication grant

Inspected original archive license entries and `META-INF/mods.toml` declarations:

| Artifact | Observed original terms | Delivery constraint |
|---|---|---|
| Forgified Fabric API | Apache-2.0; root `LICENSE-fabric-api` and nested notices | Preserve license/notices. Review Apache notice and modified-file marking obligations before any redistribution. |
| EMI Loot | MIT in mod metadata; no bundled license-text entry found | Obtain/retain applicable upstream copyright and permission notice before redistribution; metadata alone is not a complete notice bundle. |
| YACL | LGPL-3.0-or-later in mod metadata; no bundled license-text entry found | Review corresponding-source, modification, license/notice and other LGPL obligations before redistribution. |
| Relics | `All Rights Reserved` in mod metadata; no bundled grant found | Public modified-binary redistribution **blocked** absent explicit permission or independently verified applicable grant. |
| AdPother | `All rights reserved` in mod metadata; no bundled grant found | Public modified-binary redistribution **blocked** absent explicit permission or independently verified applicable grant. |

This is an artifact-based engineering assessment, not legal advice or a claim that rights were
obtained. The builder preserves every original license/notice/metadata byte and adds no new
embedded notices because its allowed changes are deliberately limited to the runtime resources.
That preservation is necessary but does **not** satisfy all potential redistribution obligations.
Even permissively licensed artifacts remain local-only in this task. Keep generated corrected
binaries and distribution ZIPs local; publish only reviewed patch source/spec, never infer a
public hosting grant from download availability, local use, an upstream project link, or tests.
