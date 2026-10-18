# Changelog

## Unreleased

- Bridged pinned Aether gloves, pendants, and Shield of Repulsion into Epic Fight's native Curios rig adapter while retaining native material passes and non-Epic rendering. Real-client visual acceptance remains separate.
- Tracked the chopping annotation processor's actual task-named refmap output and copied it under its packaged name, so recompilation does not silently omit the required map.

## 0.1.8 - 2026-09-12

- Extended RotaVision 1.0.2 ghosts to every BlockItem and added a rate-limited, server-authoritative, read-only RBP placement prediction with supported, fall/crush, and neutral fallback tints.
- Added keymap profile v2 for RotaVision, Relics, More Artifacts, and Quark conflicts while preserving customized controls.
- Removed the redundant full-body first-person add-on integration and applied armor-limb suppression directly to Epic Fight's native animated first-person renderer.
- Disabled ParCool's native and pack-added directional double-tap dodge paths while retaining its explicit dodge binding and vanilla forward double-tap sprint.
- Added active-tick percentile monitoring and a hysteretic Distant Horizons generation governor, with read-only operator status output.
- Paced Sleeping Overhaul timelapses, extended ordinary-item pickup one block horizontally, and made vanilla grass/tall-grass trampling a 10% roll once per block entry.
- Seeded the TConstruct Part Builder and Tinker Station as editable EMI favorites for new client profiles while preserving every existing `emi.json` byte-for-byte.
- Added a one-time conservative client keymap migration for Shift fast run, Mouse 5 contextual parkour, Mouse 4 ping, unbound Epic Fight dodge, and Caps Lock targeting controls.
- Added an optional public-API Threads bridge at ParCool's authoritative server action-start boundary; one persisted episode now requires two distinct configured actions.
- Added an optional public-API Threads episode across Sleeping Overhaul's authoritative simulated-night boundaries.
- Hid player and armor limb geometry in Epic Fight first person without cancelling held-item layers or weapon animation transforms.
- Replaced the Structure Generation Improver compatibility post-pass with bounded, write-time Hyle/Unearthed stone translation, eliminating a second Hyle feature run and full-chunk rock sweep.
- Serialized global dispenser-behavior registration during Forge's parallel common setup, preventing concurrent mod registrations from corrupting vanilla's backing map.
- Serialized Dynamic Trees 1.4.10 Poisson-disc chunk-data access on its existing provider monitor, preventing C2ME asynchronous chunk loads from corrupting the shared cache without disabling asynchronous I/O pack-wide.
- Locked every player into Epic Fight Battle mode and removed the obsolete Battle/Mining mode switch from keyboard and controller configuration.
- Automatically accepted Explosion Overhaul's affirmative load-or-scan policy without displaying its scan-choice HUD.
- Extracted Dynamic Survival HUD, Threads, quest task types, water survival, and economy ownership into standalone mods.
- Allowed ReHooked grapples to attach to mobs and tug both endpoints according to entity size and knockback resistance.
- Rebuilt and fully resynchronized materialized TConstruct tools after login datapack sync so durability and other dynamic stats are authoritative on the first client inventory update.
- Restored Polymorph recipe selection and persistent choices in TConstruct Crafting Stations.
- Reverted the experimental Combat Roll double-tap integration; Combat Roll is not part of the current pack or runtime behavior.
- Capped Explosion Overhaul concussion hold and accumulated durations at 45 seconds for large and repeated blasts.
- Restored vanilla daylight burning for phantoms while keeping other daylight-sensitive mobs protected.
- Replaced random wandering-trader arrivals with one recurring themed world visitor on a two-day initial and five-day repeat schedule.
- Allowed AmbientSounds effects rejected by Minecraft's full sound-channel pool to retry instead of remaining permanently silent.
- Prevented Weather2 custom fog from overriding an active Oculus shader pack's sky and fog rendering.
- Restored vanilla hopper extraction from Sophisticated Storage barrels and limited barrels while preserving their input/output rules.
- Hid the obsolete TConstruct Part Builder pattern slot while routing sand-cast crafting through its visible input.
- Standardized the repository, artifact, package, and mod namespace under the canonical Better Content identity; this is a clean break without legacy aliases or migration.
