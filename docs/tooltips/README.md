# Unified TCon hover tooltips

Assembled TCon `IModifiable` equipment has one ordinary-hover summary. Shift, Ctrl,
Alt and advanced-tooltip flags do not select alternate content. Loose parts,
repair kits and unrelated items retain their existing hover paths.

The summary shows the item name, current condition/resources, relevant intrinsic
statistics, available actions, conditional effects and drawbacks, then custom
lore. It uses detached stacks and fixed vanilla baselines, not the current
player's buffs, health, other equipment or fighting stance. Installed affix
attributes are included through the item's attribute map rather than added a
second time. Unknown effects remain identifiable and produce bounded diagnostics.

Mining is classified by the loaded definition's `IS_EFFECTIVE` hook, shared with
the existing combat-first guard. Broken state does not change that classification.
Mining-only fragments are omitted when mining is unavailable; mixed effects keep
their applicable fragments. Armor no longer receives the default weapon damage
and attack-speed row; supported unarmed damage is identified separately.

The target width is 300 GUI pixels, constrained by the viewport. There is no line
cap, ellipsis or hidden-effects counter. Extra meaningful effects remain visible.
Epic Fight's ordinary tooltip replacement, Quark's attribute graphics and Satako's
independent Alt panel are scoped out for owned equipment. The outer item-tooltip
wrapper also covers EMI's late footer injection.

Bench stat/material/modifier panels, gameplay, item NBT, controls and global
client settings are not changed.

## Resource catalogue

`assets/better_compat_fixes/tooltips/effects.json` contains effect fragments,
capability requirements, action descriptions and expected data-module types.
It reloads with client resources. Module shapes are re-read from synced modifier
objects: a changed behavioral module does not silently inherit a stat-only
omission. Unknown/custom modifiers use a visible fallback instead of disappearing.
The source-evidence generator is `tools/tooltips/build_catalogue.py`; it is not
needed to build or run the mod. Native registry coverage is distinct from the
source-evidence inventory in `descriptor-evidence.json`.

## Build and scope

Build the deployable artifact with `./gradlew --no-daemon --max-workers=2
stageRuntimeJar -Pforge_version=47.4.22`. Do not ship a development-mapped JAR.
Only the affected runtime JAR and its Packwiz/index hashes need deployment.

Earlier targeted tests and native generic-hover runs exercised both GUI scales,
NBT preservation, F3+H, missing-player/off-thread construction, and health/buff
independence, including a run with EMI, Quark and Satako loaded. Those runs are not
a claim that every addon, physical-key combination, inventory/recipe-viewer screen,
resource reload or bench comparison has been accepted. Further verification work
was stopped at the user's direction; the final deployment is not a full-pack
acceptance certification.
