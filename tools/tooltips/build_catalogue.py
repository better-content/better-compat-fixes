#!/usr/bin/env python3
"""Reproducible source-evidence catalogue seed, not a localization-key-as-registry scan.

Inputs: pinned pack cache, authored combat catalogue, and the decompiled registration
sources retained by the audit. Generated JSON is checked in; no build/runtime dependency
on these development paths. Native registry coverage must be checked separately.
"""
import json, re, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CACHE = Path('/home/dev/.cache/bc/packwiz-downloads/mods')
AUDIT = Path('/tmp/tcon-tooltip-audit')
PACK = Path('/home/dev/better-content-modpack')
modules, lang, evidence, descriptors = {}, {}, {}, {}
# Only concrete data-driven definitions and actual modifier registrations are candidates.
for path in sorted(CACHE.glob('*.jar')):
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            if name.startswith('assets/') and name.endswith('/lang/en_us.json'):
                try: lang.update(json.loads(archive.read(name)))
                except (ValueError, UnicodeError): pass
            match = re.fullmatch(r'(?:datapacks/[^/]+/)?data/([^/]+)/tinkering/modifiers/(.+)\.json', name)
            if match:
                value = json.loads(archive.read(name))
                if 'redirects' not in value:
                    key = ':'.join(match.groups()); modules[key] = value.get('modules', [])
                    evidence[key] = path.name + '!' + name

for file, namespace in [
    (AUDIT/'tcon/slimeknights/tconstruct/tools/TinkerModifiers.java', 'tconstruct'),
    (AUDIT/'extras/com/c2h6s/etstlib/register/EtSTLibModifier.java', 'etstlib'),
    (AUDIT/'extras/com/minhhjjj/epicfighttinkercompat/modifiers/EpicFightModifiers.java', 'epicfighttinkercompat'),
]:
    for line in file.read_text().splitlines():
        if 'Modifier' not in line or not re.search(r'\b(MODIFIERS|MODIFIER|MODIFIER_REGISTER)\.register', line): continue
        match = re.search(r'\.register(?:Dynamic)?\("([^"]+)"', line)
        if match:
            key = namespace + ':' + match[1]
            evidence.setdefault(key, str(file.relative_to(AUDIT)))

# These modules contribute only to stats already displayed, appearance, storage,
# or bench accounting. 'attribute' is deliberately NOT blanket-hidden.
folded = {'stat_boost', 'set_stat', 'rarity', 'modifier_slot', 'requirements',
          'capacity_bar', 'durability_color', 'variant_color', 'material_variant_color',
          'craft_count', 'trait'}
mining_types = {'conditional_mining_speed', 'mining_capacity', 'melting', 'smashing',
                'smelting', 'enderclearance', 'tool_action_transform', 'armor_harvest_enchantment'}


def fragment(key, text, requires=(), kind='behavior', scale=(), named=False):
    result = dict(key=key, text=text, requires=list(requires), kind=kind)
    if scale: result['per_level'] = list(scale)
    if named: result['named'] = True
    return result


def describe(key, text, requires=(), kind='behavior', scale=(), suffix='effect'):
    return fragment('tooltip.better_compat_fixes.'+key.replace(':', '.')+'.'+suffix,
                    text, requires, kind, scale)


for key in evidence:
    mod = modules.get(key)
    if mod and all(m.get('type', '').removeprefix('tconstruct:') in folded for m in mod):
        descriptors[key] = []
        continue
    description_key = 'modifier.' + key.replace(':', '.') + '.description'
    if description_key not in lang: continue  # retain runtime unknown fallback, not an invented classification
    requires = []
    behavior = [m for m in mod or [] if m.get('type', '').removeprefix('tconstruct:') not in folded]
    if behavior and all(m.get('type', '').removeprefix('tconstruct:') in mining_types for m in behavior): requires = ['mining']
    kind = 'conditional' if any(m.get('type', '').split(':')[-1].startswith('conditional_') for m in behavior) else 'behavior'
    descriptors[key] = [fragment(description_key, lang[description_key], requires, kind=kind, named=True)]

# Actual assembled affix contributions are cached stats + the native item attribute hook.
# Behavioral grants appear once in the native modifier list, never from affix-name accounting.
descriptors['better_tinkers_loot_affixes:affix_stats'] = []
descriptors['epicfighttinkercompat:epic_part_stats_calculator'] = []
descriptors['epicfighttinkercompat:epic_stats'] = []
descriptors['tconstruct:empty'] = []
for key in ['creative_slot', 'stat_override', 'energy_handler', 'tank_handler', 'offhand_attack',
            'embellishment', 'dyed', 'trim', 'farsighted', 'nearsighted', 'unbreakable']:
    descriptors['tconstruct:'+key] = []

# Hand-reviewed fragments for mixed effects. No runtime English parsing is used.
overrides = {
 'tconstruct:airborne': [('No airborne mining penalty', ['mining']), ('While airborne: faster projectiles', ['launcher'], 'conditional')],
 'tconstruct:emerald': [('Repair effectiveness increased by 50%', [], 'behavior'), ('Conditional damage bonuses increased by 25%', ['combat'], 'conditional'), ('Conditional mining bonuses increased by 25%', ['mining'], 'conditional')],
 'tconstruct:entangled': [('Takes durability damage when unequipped', [], 'restriction')],
 'tconstruct:heavy': [], # movement penalties are actual item attributes
 'tconstruct:haste': [],
 'tconstruct:jagged': [('Damage rises as durability is lost', ['melee'], 'conditional'), ('Mining slows as durability is lost', ['mining'], 'restriction')],
 'tconstruct:stonebound': [('Damage falls as durability is lost', ['melee'], 'restriction'), ('Mining speeds up as durability is lost', ['mining'], 'conditional')],
 'tconstruct:dwarven': [('Deeper: faster mining; high altitude: slower mining', ['mining'], 'conditional'), ('Deeper: faster projectiles; high altitude: slower projectiles', ['launcher'], 'conditional')],
 'tconstruct:lightspeed': [('Well-lit blocks mine faster', ['mining'], 'conditional'), ('Movement speeds up in bright light', ['armor'], 'conditional')],
 'tconstruct:maintained': [('Higher durability: faster mining', ['mining'], 'conditional'), ('Higher durability: faster projectiles', ['launcher'], 'conditional'), ('Higher durability: increased movement speed', ['armor'], 'conditional')],
 'tconstruct:temperate': [('Cold: faster mining', ['mining'], 'conditional'), ('Cold: faster drawing', ['launcher'], 'conditional'), ('Heat: reduced durability loss', [], 'conditional'), ('Cold: increased protection', ['armor'], 'conditional')],
 'tconstruct:momentum': [('Consecutive blocks increase mining speed', ['mining'], 'conditional'), ('Consecutive shots increase draw speed', ['launcher'], 'conditional')],
 'tconstruct:enderporting': [('Teleports you to broken blocks', ['mining'], 'behavior'), ('Teleports you to killed enemies', ['melee'], 'behavior')],
 'tconstruct:chrysophilite': [('Increased gold from ores', ['mining']), ('Increased gold from monsters', ['combat'])],
 'tconstruct:luck': [('Fortune %s', ['mining'], 'behavior', [1]), ('Looting %s', ['combat'], 'behavior', [1])],
 'tconstruct:fortune': [('Fortune %s', ['mining'], 'behavior', [1])],
 'tconstruct:silky': [('Silk touch', ['mining'])],
 'tconstruct:silky_shears': [('Can shear mobs', ['silky'])],
 'tconstruct:silky_aoe_shears': [('Can shear nearby mobs', ['silky'])],
 'tconstruct:aqua_affinity': [('No underwater mining penalty', ['mining'])],
 'tconstruct:exchanging': [('Replaces broken blocks with the offhand block', ['mining'])],
 'tconstruct:autosmelt': [('Automatically smelts mined drops', ['mining']), ('Automatically cooks mob drops', ['combat'])],
 'tconstruct:expanded': [('Larger area for supported actions', [])],
 'tconstruct:harvest': [('Use on crops to harvest and replant', ['mining'])],
 'tconstruct:snow_boots': [('Walk on powder snow', ['armor'])],
 'tconstruct:offhand_attack': [('Can attack with the offhand', ['melee'])],
 'tconstruct:netherite': [('Dropped tool resists external damage, including fire', [])],
 'tconstruct:vintage': [], # slots are bench-only; movement penalty comes from attributes
 'tconstruct:necrotic': [('Fully charged critical hits heal %s%% of damage dealt', ['melee'], 'conditional', [5]), ('Projectile hits heal %s%% of their damage', ['projectile'], 'conditional', [5]), ('When hurt: 15%% chance of regeneration, scaling with damage and modifier level', ['defense'], 'conditional')],
 'tconstruct:experienced': [('+%s%% experience from kills while held', ['combat', 'held'], 'behavior', [50]), ('+%s%% experience from mined blocks', ['mining'], 'behavior', [50])],
 'tconstruct:depth_protection': [('Deeper: increased protection; high altitude: reduced protection', ['armor'], 'conditional')],
 'tconstruct:stoneshield': [('Mined stone can restore a durability shield', ['mining'], 'conditional'), ('Stored stoneshield protects durability', [], 'behavior')],
 'tconstruct:war_charge': [('Mining while loaded builds war charge', ['mining'], 'conditional'), ('Stored war charge increases the next shot’s velocity', ['projectile'], 'conditional')],
 'etstlib:atomic_decompose': [('Uses native mining speed and ignores harvest-tier restrictions', ['mining'])],
 'tinkers_things:fortified': [('Repairable with copper', [])],
 'tinkers_things:hyperoptic': [], # actual item mining-speed attribute is displayed only when mining is enabled
 'tconstruct:pierce': [('Attacks weaken armor for 5 seconds', ['attack']), ('Direct attackers have weakened armor for 5 seconds', ['defense'], 'conditional')],
 'tconstruct:featherweight': [('Protection -%s', ['defense'], 'restriction', [1.25]), ('Use-item speed +%s%% while worn', ['armor'], 'behavior', [10])],
 'tconstruct:spectral': [('Hits make targets glow for %s s', ['attack'], 'behavior', [10]), ('Projectiles make struck blocks glow', ['projectile']), ('Direct attackers glow; duration scales with level and blocking', ['defense'], 'conditional')],
 'tconstruct:enderference': [('Hits briefly prevent teleporting', ['attack'])],
 'tconstruct:raging': [('Low health increases melee damage', ['melee'], 'conditional'), ('Low health increases projectile power', ['projectile'], 'conditional')],
 'tconstruct:blocking': [('Use to block incoming damage', ['blocking'])],
 'tconstruct:dual_wielding': [('Offhand attacks when both tools support dual wielding', ['melee'], 'conditional')],
 'etstlib:momentum_accelerate': [('Consecutive attacks increase attack speed', ['melee'], 'conditional'), ('Consecutive blocks increase mining speed', ['mining'], 'conditional')],
 'etstlib:static_discharge': [('Movement and attacks build charge; charged hits deal extra damage', ['combat'], 'conditional'), ('Mining also builds charge', ['mining'], 'conditional')],
 'etstlib:extra_dense': [('Reduced repair effectiveness', [], 'restriction')],
 'etstlib:hardened': [('Reduced durability loss', [], 'behavior')],
 'etstlib:aerial_reinforced': [('Stored air replaces durability loss and boosts combat stats', ['combat'], 'conditional'), ('Stored air boosts mining speed', ['mining'], 'conditional')],
 'etstlib:cheap': [('Projectile destroys itself on landing', ['ammo'], 'restriction')],
}
for key, facts in overrides.items():
    descriptors[key] = [describe(key, *fact, suffix=str(i)) for i, fact in enumerate(facts)]
    # Retain effective level for formulas whose magnitude is not a linear scalar.
    for f in descriptors[key]:
        if not f.get('per_level') and ('modifier.' + key.replace(':', '.')) in lang: f['named'] = True

# Assembly-wide stat carriers from the pinned Epic Fight integration contain only stat modules.
# They are discovered from data, not a guessed list of material names.
for key, mods in modules.items():
    if mods and all(m.get('type') == 'tconstruct:stat_boost' for m in mods): descriptors[key] = []

combat = json.loads((PACK/'kubejs/config/tcon_edps_catalogue.json').read_text())
items = combat['tools'] + combat['weapons']
assert len(items) == 44
# All 44 entries are recorded for coverage, not mining classification. Innates come from the
# resolved capability's item-only skill map, so this list never chooses a combat/mining policy.
actions = {tool['item']: [] for tool in items}
for tool in items:
    alternate = tool.get('alternate', {}).get('mode')
    texts = {'returning throw': 'Alternate: returning throw', 'throw': 'Alternate: throw',
             'war_charge projectile': 'Alternate: spend war charge to fire a projectile',
             'charged Bonk': 'Alternate: charge and release a knockback strike'}
    if alternate in texts: actions[tool['item']] = [describe(tool['item'], texts[alternate])]

skills = {
 'sweeping_edge': 'Sweep nearby enemies',
 'heartpiercer': 'Three rapid spear thrusts',
 'relentless_combo': 'A rapid flurry of blows',
 'dancing_edge': 'Three-strike flourish',
 'steel_whirlwind': 'Charge a forward cleave to increase its range and speed',
 'the_guillotine': 'Heavy strike; executes sufficiently weakened targets',
 'grasping_spire': 'Pull a struck enemy in, then strike again with knockback',
 'rushing_tempo': 'Add an extra strike after basic attacks',
 'liechtenauer': 'Deflect frontal attacks while attacking; kills extend the stance; cannot sprint',
 'eviscerate': 'Stab; a successful hit leads to a critical follow-up',
 'blade_rush': 'Apply Instability; three stacks execute the target',
 'everlasting_allegiance': 'Recall a thrown trident, damaging enemies along its return path',
}
for key, text in skills.items(): actions['skill:epicfight:'+key] = [describe('epicfight:'+key, text)]

output = ROOT/'src/main/resources/assets/better_compat_fixes/tooltips/effects.json'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps({'schema': 1, 'modifiers': dict(sorted(descriptors.items())), 'actions': actions,
    'moduleTypes': {key: sorted(m.get('type', '') for m in value) for key, value in sorted(modules.items())}}, indent=2)+'\n')
report = ROOT/'docs/tooltips/descriptor-evidence.json'
report.parent.mkdir(parents=True, exist_ok=True)
report.write_text(json.dumps({'note': 'Source evidence only. Native loaded-registry verification is a separate acceptance gate.',
 'combat_profiles': len(items), 'classified_entries': len(descriptors),
 'data_module_types': sorted({m.get('type') for mods in modules.values() for m in mods}),
 'source_evidence': evidence, 'unclassified_source_candidates': sorted(set(evidence)-set(descriptors))}, indent=2)+'\n')
print(f'{len(descriptors)} descriptors; {len(items)} combat profiles; {len(set(evidence)-set(descriptors))} unclassified source candidates')
