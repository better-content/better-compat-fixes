# Pinned VS interface handlers: both Mixin phases matter

Build 469 passed source gates and candidate fast checks but failed real Dist on
`MixinVibrationSystemTicker.destWorldPos`. It is **not accepted**. The pinned raw
VS 2.4.11 class has three PRIVATE|STATIC (0x000a) handlers. The old local
`bc-interface.1` recipe widened them to PUBLIC|STATIC (0x0009). That satisfies
Mixin 0.8.5's interface preparation visibility check but fails its later static
method application check. The failure is in the existing local correction, not
an assertion that upstream shipped public handlers.

The revised **bc-interface.2** recipe retains PRIVATE|STATIC and adds only
SYNTHETIC (0x100a). The actual 0.8.5 `MixinPreProcessorInterface.prepareMethod`
allows private synthetic interface helpers with the requisite Java compatibility
level; the original config is JAVA_17. The later applicator retains its private
static invariant. Both stage predicates are regression controls, not a claimed
native transformation or runtime acceptance receipt.

Only the high byte in each of three independently parsed U2 access words changes.
The class/interface kind, constant pool (including the self InterfaceMethodref),
method bodies, calls, descriptors, annotations, config requirements, plugin,
refmap and STATIC/PRIVATE bits remain unchanged. Existing exact common-to-client
relocations remain. All other recursive classes/resources/licenses are checked
byte-for-byte. The original pinned binaries and old failed artifacts are retained;
new derivative filenames, hashes and receipts prevent stale reuse. Root has an
independent structural oracle and rebound-hash negative controls for the old
public-static patch, missing SYNTHETIC, code/annotation/descriptor changes and
unrelated archive mutations. No global Mixin upgrade, reflection or gate filtering
is introduced. Modified local artifacts do not confer publication permission.

Fresh owning qualification and a new candidate with exact-hash Dist then full
Debug acceptance are still required. Focused checks never substitute for them.
