#!/usr/bin/env python3
"""Union-of-config reserved namespace admission; exact class annotations, never CP substrings."""
import argparse
import json
import re
import struct
from pathlib import Path

MIXIN = 'Lorg/spongepowered/asm/mixin/Mixin;'
ROLES = ('main', 'choppingMixins', 'caravanMixins', 'urnMixins', 'nativeFixture', 'registrateFixture', 'test')
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
    configs = sorted((repo/'src').glob('*/resources/*mixins.json')); need(len(configs) == 8, 'Explicit config population drift')
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
        source_role = 'main' if role in ('choppingMixins', 'caravanMixins', 'urnMixins') else role
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
    found = set()
    for item in roots:
        role, path = item.split('=', 1); root = Path(path)
        need(role in (*ROLES, 'generatedOracle', 'repairSnapshot'), 'Unknown compiled output role ' + role)
        files = sorted(root.rglob('*.class')); need(root.is_dir() and files, 'Missing/empty compiled role ' + item)
        for file in files:
            name, annotated = class_identity(file.read_bytes()); admit(name, annotated, prefixes, configured)
            if name in configured: found.add(name)
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
    print('Union namespace guards passed: configs=8 configured=' + str(len(configured)) + '; strict class annotation controls, no silent roots')
