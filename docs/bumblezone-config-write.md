# Pinned Bumblezone dual-config-reader write race

Bumblezone `7.13.4+1.20.1-forge` (CurseForge 8548194) registers each of its six
configuration specs with Forge, then independently builds, autoreloads and loads
a second `CommentedFileConfig` for the same path. The pinned raw
`BzConfigHandler.class` SHA-256 is
`7055df824f0a9af3221846c3189bfa1b1a7e23f5209b33c6a5b11476dc9d1fcf`.
`verifyBumblezoneConfigAbi` checks that exact binary, call ordering and six paths,
with changed-pin/missing-loader-call negative controls.

Forge 47.4.13 and the delivered 47.4.22 have identical `ConfigFileTypeHandler`
code here: their later config load installs its own watcher, fires Loading, then
unconditionally saves with NightConfig's truncating `WritingMode.REPLACE`.
NightConfig 3.6.4 synchronizes load/save on each individual config object, not
across the independent Bumblezone and Forge objects. Thus even a complete,
byte-identical preseeded config can expose a partial quoted section header to
the other reader during the later save. This was retained in release attempt
`20261025T135014Z-3098819`: all eleven Crops tests passed but the strict child
console audit rejected the actual `ParsingException` from the raw autoreloader.
Preseeding alone is not a repair for this lifecycle race.

The first prototype attempted to replace a private writer field. The source
reflection gate correctly rejected it; the gate and diagnostic-only allowlist
were not relaxed. The delivered design uses **public extension APIs only**.

A separately version-gated, fail-closed Mixin redirects only the pinned handler's
`ModLoadingContext.registerConfig` call. It registers a `ModConfig` subclass via
the original public container API. Its public `getHandler()` extension supplies
a handler which invokes the **original `ConfigFileTypeHandler.reader` unchanged**,
inside a thread-local writer-construction scope. A public NightConfig format
supplier returns the identical original TOML singleton for every other thread
or unscoped config; inside this exact reader scope it delegates the parser,
comments/config factory and serialization but constructs an atomic writer.
The scope is removed in `finally`, including failed/nested construction.

Both original watchers, callbacks, parsers, spec binding, authored values/comments
and Forge load/save/reload events remain. No watcher registration is copied,
removed, filtered or replaced. All six paths and the exact Bumblezone version
are checked, unknown format registration/reader construction fails closed, and
the historical compatibility plugin remains unchanged. No production reflection,
private field access, MethodHandles or Unsafe is used.

The original TOML writer serializes to a same-directory temporary file, which
is forced and atomically moved over the target. There is no truncating fallback.
Symlink ancestry, a different target path or append mode is rejected. Failed temporary
files are retained for diagnosis. Existing POSIX permissions are preserved.
NightConfig 3.6.4 watches `ENTRY_MODIFY`, not the rename event: after the complete
inode is published the bridge changes its mtime, preserving real notification
of both original observers without exposing a partial file. An actual
NightConfig watcher test failed for rename-only publication and passes with
this post-publication metadata notification.

Tests deterministically pause a writer inside a partial quoted header: the
original REPLACE reader fails, while atomic publication exposes complete old
then complete new TOML. Additional controls cover the real watcher, settings,
comments/encoding, failure retention, exact scope, symlinks, mode/path refusal,
actual Forge-style public-factory construction, nested/mismatched scope refusal,
and unchanged original format identity in other threads. The Crops
nonshipping canonical-provider proof additionally requires all five actual
Bumblezone COMMON configs to use the production bridge and observes a genuine
Forge reload event after a byte-preserving save. The existing seven compat
provider profiles remain intact; Crops' canonical full-pack closure supplies
the actual Bumblezone presence integration.
