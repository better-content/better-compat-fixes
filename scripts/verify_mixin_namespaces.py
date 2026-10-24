#!/usr/bin/env python3
"""Union-of-config reserved namespace admission; exact class annotations, never CP substrings."""
import argparse
import json
import re
import struct
from pathlib import Path

MIXIN = 'Lorg/spongepowered/asm/mixin/Mixin;'
ROLES = ('main', 'choppingMixins', 'caravanMixins', 'urnMixins', 'chunkIoMixins', 'nativeFixture', 'registrateFixture', 'nativeForgeApiFixture', 'test')
CHUNK_IO_CONFIG = 'better_compat_fixes.chunk_io.mixins.json'
CHUNK_IO_CLASS = 'com.bettercontent.bettercompatfixes.mixin.minecraft.IOWorkerPendingReadMixin'
# Explicit roles as well as names: adding reviewed configs must not turn
# this guard into a count-only wildcard or permit moving a test config to main.
EXPECTED_CONFIGS = {
    'main/resources/better_compat_fixes' + suffix + '.mixins.json'
    for suffix in ('', '.caravan', '.chopping', '.chunk_io', '.moonlight_platform', '.oc2r', '.registrate', '.startup_abi', '.urn')
} | {
    'nativeFixture/resources/better_compat_fixes.native_fixture.mixins.json',
    'nativeFixture/resources/better_compat_fixes.fuel_fixture.mixins.json',
    'registrateFixture/resources/better_compat_fixes.registrate_fixture.mixins.json',
    'nativeForgeApiFixture/resources/better_compat_fixes.native_forge_api_fixture.mixins.json',
}
def need(ok, message):
    if not ok: raise AssertionError(message)

class Reader:
    def __init__(self, data): self.data, self.pos = data, 0
    def take(self, size):
        result = self.data[self.pos:self.pos+size]; self.pos += size
        need(len(result) == size, 'Truncated class/annotation'); return result
    def u1(self): return self.take(1)[0]
    def u2(self): return struct.unpack('>H', self.take(2))[0]
    def u4(self): return struct.unpack('>I', self.take(4))[0]

def class_identity(data):
    r = Reader(data); need(r.u4() == 0xCAFEBABE, 'Not class bytes'); r.take(4)
    cp = [None] * r.u2(); i = 1
    while i < len(cp):
        tag = r.u1()
        if tag == 1: cp[i] = r.take(r.u2()).decode('utf-8', errors='replace')
        elif tag == 7: cp[i] = r.u2()
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): r.take(4)
        elif tag in (5, 6): r.take(8); i += 1
        elif tag in (8, 16, 19, 20): r.take(2)
        elif tag == 15: r.take(3)
        else: raise AssertionError('Unknown constant-pool tag ' + str(tag))
        i += 1
    r.u2(); name = cp[cp[r.u2()]].replace('/', '.'); r.u2(); r.take(2*r.u2())
    def attributes(reader):
        return [(cp[reader.u2()], reader.take(reader.u4())) for _ in range(reader.u2())]
    for _ in range(2):
        for _ in range(r.u2()): r.take(6); attributes(r)
    annotations = set()
    def annotation(a):
        kind = cp[a.u2()]
        for _ in range(a.u2()): a.u2(); value(a)
        return kind
    def value(a):
        tag = chr(a.u1())
        if tag in 'BCDFIJSZsc': a.u2()
        elif tag == 'e': a.take(4)
        elif tag == '@': annotation(a)
        elif tag == '[':
            for _ in range(a.u2()): value(a)
        else: raise AssertionError('Unknown annotation element ' + tag)
    for key, body in attributes(r):
        if key in ('RuntimeVisibleAnnotations', 'RuntimeInvisibleAnnotations'):
            a = Reader(body)
            for _ in range(a.u2()): annotations.add(annotation(a))
            need(a.pos == len(body), 'Annotation parser did not consume attribute')
    need(r.pos == len(data), 'Class parser did not consume class')
    return name, MIXIN in annotations

def configuration(repo):
    configs = sorted((repo/'src').glob('*/resources/*mixins.json'))
    actual_configs = {str(file.relative_to(repo/'src')) for file in configs}
    need(actual_configs == EXPECTED_CONFIGS,
         'Explicit config population drift: ' + str(actual_configs ^ EXPECTED_CONFIGS))
    for name, prefix, entries, server in (
        ('better_compat_fixes.startup_abi.mixins.json',
         'com.bettercontent.bettercompatfixes.mixin.startupabi', [], ['EpicFightCapabilitiesServerAbiMixin']),
        ('better_compat_fixes.fuel_fixture.mixins.json',
         'com.bettercontent.bettercompatfixes.fuelfixture.mixin',
         ['HearthFuelAccess', 'SteamLocomotiveFuelAccess', 'SteamTugFuelAccess', 'ChunkPollutionWitnessMixin'], []),
        ('better_compat_fixes.moonlight_platform.mixins.json',
         'com.bettercontent.bettercompatfixes.mixin.moonlightplatform', ['MoonlightForgePlatformMixin'], []),
        ('better_compat_fixes.native_forge_api_fixture.mixins.json',
         'com.bettercontent.bettercompatfixes.nativeforgeapifixture.mixin',
         ['FFAPIInteractionWitnessMixin', 'FFAPIPackFinderWitnessMixin',
          'FFAPIResourcePackFinderWitnessMixin', 'MoonlightGeneratedBytesWitnessMixin'], []),
    ):
        data = json.loads(next(file for file in configs if file.name == name).read_text())
        need(data.get('required') is True and data.get('injectors', {}).get('defaultRequire') == 1,
             'New dedicated config must fail closed: ' + name)
        need(data.get('package') == prefix and data.get('mixins') == entries
             and data.get('server', []) == server and not data.get('client'),
             'Exact dedicated config namespace/population drift: ' + name)
    chunk_io_files = [file for file in configs if file.name == CHUNK_IO_CONFIG]
    need(len(chunk_io_files) == 1, 'Exactly one dedicated chunk I/O config required')
    chunk_io = json.loads(chunk_io_files[0].read_text())
    need(chunk_io.get('required') is True and chunk_io.get('injectors', {}).get('defaultRequire') == 1,
         'Chunk I/O config must fail closed')
    need(chunk_io.get('package') == CHUNK_IO_CLASS.rsplit('.', 1)[0]
         and chunk_io.get('mixins') == [CHUNK_IO_CLASS.rsplit('.', 1)[1]]
         and not chunk_io.get('client') and not chunk_io.get('server'), 'Chunk I/O config must contain only its exact vanilla hook')
    need(chunk_io.get('refmap') == 'better_compat_fixes.chunk_io.refmap.json', 'Chunk I/O dedicated refmap required')
    prefixes, configured = set(), set()
    for file in configs:
        data = json.loads(file.read_text()); prefix = data['package'] + '.'; prefixes.add(prefix)
        for key in ('mixins', 'client', 'server'):
            configured.update(prefix + name for name in data.get(key, []))
    for file in configs:
        plugin = json.loads(file.read_text()).get('plugin', '')
        need(not any(plugin.startswith(p) for p in prefixes), 'Plugin in union of reserved namespaces: ' + plugin)
    return prefixes, configured

def admit(name, annotated, prefixes, configured):
    if any(name.startswith(p) for p in prefixes):
        need(name in configured and annotated, 'Reserved namespace ordinary/unconfigured/non-Mixin class: ' + name)

def source_check(repo, prefixes, configured):
    found = set()
    for role in ROLES:
        # These AP-isolated roles explicitly select files from the same authored main root.
        source_role = 'main' if role in ('choppingMixins', 'caravanMixins', 'urnMixins', 'chunkIoMixins') else role
        root = repo/'src'/source_role/'java'; need(root.is_dir(), 'Missing explicit source role ' + role)
        for file in root.rglob('*.java'):
            text = file.read_text()
            clean = re.sub(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', ' ', text, flags=re.S)
            package = re.search(r'\bpackage\s+([\w.]+)\s*;', clean)
            need(package is not None, 'No explicit Java package: ' + str(file))
            name = package[1] + '.' + file.stem
            if any(name.startswith(p) for p in prefixes):
                need(len(re.findall(r'\b(?:class|interface|enum|record)\s+\w+', clean)) == 1, 'Nested/extra class in reserved source: ' + str(file))
                need(not re.search(r'\bnew\s+[\w.$<>?,\s]+\([^{};]*\)\s*\{', clean), 'Anonymous helper in reserved source: ' + str(file))
                admit(name, bool(re.search(r'@(?:org\.spongepowered\.asm\.mixin\.)?Mixin\s*\(', clean)), prefixes, configured)
                found.add(name)
    need(found == configured, 'Configured/source Mixin set differs: ' + str(configured ^ found))

def compiled_check(roots, prefixes, configured):
    found, seen_roles = set(), set()
    for item in roots:
        role, path = item.split('=', 1); root = Path(path)
        need(role in (*ROLES, 'generatedOracle', 'repairSnapshot'), 'Unknown compiled output role ' + role)
        files = sorted(root.rglob('*.class')); need(root.is_dir() and files, 'Missing/empty compiled role ' + item)
        seen_roles.add(role)
        if role == 'chunkIoMixins':
            need(len(files) == 1, 'Chunk I/O AP role must contain exactly one class')
        for file in files:
            name, annotated = class_identity(file.read_bytes()); admit(name, annotated, prefixes, configured)
            if role == 'chunkIoMixins':
                need(name == CHUNK_IO_CLASS and annotated, 'Chunk I/O AP role must contain only its annotated hook')
            if name == CHUNK_IO_CLASS:
                need(role == 'chunkIoMixins', 'Chunk I/O hook escaped its isolated AP role')
            if name in configured: found.add(name)
    need('chunkIoMixins' in seen_roles, 'Explicit compiled chunk I/O AP role required')
    need(found == configured, 'Configured/compiled Mixin set differs: ' + str(configured ^ found))

if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[1]); p.add_argument('--classes', action='append', default=[]); p.add_argument('--source-only', action='store_true')
    a = p.parse_args(); prefixes, configured = configuration(a.repo); source_check(a.repo, prefixes, configured)
    if not a.source_only:
        need(a.classes, 'Explicit compiled output roles required'); compiled_check(a.classes, prefixes, configured)
    # Independent admission negatives: even annotated-but-unconfigured or ordinary configured names fail.
    for name, annotated in [(next(iter(prefixes))+'OrdinaryHelper', False), (next(iter(prefixes))+'UnconfiguredMixin', True), (next(iter(configured)), False), (next(iter(configured))+'$Nested', True)]:
        try: admit(name, annotated, prefixes, configured)
        except AssertionError: pass
        else: raise AssertionError('Namespace negative accepted')
    print('Union namespace guards passed: configs=' + str(len(EXPECTED_CONFIGS)) + ' configured=' + str(len(configured)) + '; strict class annotation controls, no silent roots')
