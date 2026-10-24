#!/usr/bin/env python3
"""Independent pinned upstream/compiled hook controls. No target class initialization/native verdict."""
import argparse
import hashlib
import re
import subprocess
import zipfile
from pathlib import Path
import uuid

OWNER = 'com/tterrag/registrate/AbstractRegistrate'
PAIRS = [
 ('7bfa4965a2cf3fc790227e6d94bb47d5b9a898b532f4f489570db2ae158c4147', '37bb5b0230e0a7d9fb594c9ec53d37317094db089035b4e0f19282f06ba780ee'),
 ('a2052e843b729659493d725127556df6931ddf5a3aab2937ef1e48cf1318034f', 'e1e9154a7036be8584b81bede8f7c41cb1a0301e3c16c664e71e7d0d2346d029'),
 ('a0ec2629eb755af8e2f769c836f1efff3b50e36e3cf2dc57a1418f21ab978225', 'f51ca9bdef1842b6d1ffb3c7cc59ead3616f3e2a079eb1ae6a13c5e9f12292ce'),
 ('923bc6c760a84def349071c986044e7906503824c3ac460166f77a88973b65bc', 'f150c0f07209434ae135ceb32480b22d05c539fd81c60e1e49605cd0c8e86864')]
def require(value, message):
    if not value: raise AssertionError(message)
def javap(path, owner, verbose=False):
    return subprocess.run(['javap', '-c', '-p', *(['-v'] if verbose else []), '-classpath', str(path), owner.replace('/', '.')],
        text=True, capture_output=True, check=True, timeout=30).stdout

def method(text, name):
    matches = list(re.finditer(r'(?m)^  [^\n]* ' + re.escape(name) + r'\([^\n]*\);', text))
    require(len(matches) == 1, 'Missing/ambiguous pinned method ' + name)
    start = matches[0].start(); end = re.search(r'\n  (?:public|private|protected|static|[a-zA-Z].*\()[^\n]*', text[matches[0].end():])
    return text[start:matches[0].end() + end.start()] if end else text[start:]

def validate(args):
    with zipfile.ZipFile(args.raw) as archive:
        raw_pair = tuple(hashlib.sha256(archive.read(name + '.class')).hexdigest() for name in [OWNER, OWNER + '$Registration'])
    require(raw_pair in [PAIRS[0], PAIRS[2]], 'Unreviewed original Registrate class pair')
    # Each source profile owns one coherent raw/mapped version; the five-profile matrix
    # runs the explicit fixture on 1.3.11 and the ordinary/absence paths on 1.3.3.
    version = 0 if raw_pair == PAIRS[0] else 2
    for path, pair in [(args.raw, PAIRS[version]), (args.mapped, PAIRS[version + 1]),
                       (args.other_raw, PAIRS[2 if version == 0 else 0])]:
        with zipfile.ZipFile(path) as archive:
            actual = tuple(hashlib.sha256(archive.read(name + '.class')).hexdigest() for name in [OWNER, OWNER + '$Registration'])
        require(actual == pair, f'Unreviewed Registrate class pair: {path}: {actual}')
        text = javap(path, OWNER)
        accept = method(text, 'accept')
        # The two add overloads are intentionally distinguished by exact signature.
        add = text.split('S addRegisterCallback(java.lang.String,', 1)[1].split('\n  public ', 1)[0]
        lookup = method(text, 'getRegistrationUnchecked'); all_entries = method(text, 'getAll')
        require(accept.count('Multimap.removeAll:') == 1 and accept.count('Table.put:') == 1, 'accept slice cardinality')
        require(accept.index('Registration."<init>"') < accept.index('Logger.debug:') < accept.index('Multimap.removeAll:') < accept.index('Collection.forEach:') < accept.index('Table.put:') < accept.index('Registration.getDelegate:'), 'accept ctor/log/transfer/publication/delegate order')
        require(add.count('getRegistrationUnchecked:') == 1 and add.count('Multimap.put:') == 1 and add.count('Registration.addRegisterCallback:') == 1 and add.count('Method self:') == 1, 'add branching/site counts')
        require(add.index('getRegistrationUnchecked:') < add.index('Multimap.put:') < add.index('Registration.addRegisterCallback:') < add.index('Method self:'), 'add native order')
        require(lookup.count('Table.get:') == 1 and 'checkcast' in lookup, 'private-return reader must remain original cast')
        require('Collectors.toList:' in all_entries and all_entries.count('Stream.collect:') == 1, 'getAll ceased to be eager snapshot')
        registration = javap(path, OWNER + '$Registration')
        require(method(registration, 'getDelegate').count('invoke') == 0, 'Delegate getter gained callout')
        append = method(registration, 'addRegisterCallback')
        require(append.count('Preconditions.checkNotNull:') == 1 and append.count('List.add:') == 1 and append.count('invoke') == 2, 'Native callback appender gained callout')
        require('NonNullFunction.apply:' in registration and 'NonNullSupplier.get:' in registration, 'Native arbitrary constructor/creator calls drift')
    compiled = javap(args.classes, 'com/bettercontent/bettercompatfixes/mixin/registrate/RegistrateTransactionMixin', True)
    compiled_contract(compiled)
    for broken in [compiled.replace('require=1', 'require=0', 1), compiled.replace('// InterfaceMethod com/llamalad7/mixinextras/injector/wrapoperation/Operation.call:', '// InterfaceMethod com/llamalad7/mixinextras/injector/wrapoperation/Operation.changed:', 1)]:
        try: compiled_contract(broken)
        except AssertionError: pass
        else: raise AssertionError('Actual compiled hook mutation accepted')
    source_and_archive_controls(args.evidence)
    print('Registrate raw/mapped pins, compiled hook mutation/forward controls and source/archive guards passed; composition/native pending')

def compiled_contract(compiled):
    require(compiled.count('require=1') == 8 and compiled.count('allow=1') == 8, 'Actual compiled required hook counts')
    require(compiled.count('com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(') == 3, 'Three original-call cleanup/read wrappers required')
    require(compiled.count('com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(') == 1, 'One public Object table reader operation required')
    require(compiled.count('org.spongepowered.asm.mixin.injection.Inject(') == 4, 'Four narrow acquire/release points required')
    require(compiled.count('// InterfaceMethod com/llamalad7/mixinextras/injector/wrapoperation/Operation.call:') == 4, 'Original forwarders must invoke each original exactly once')
    require('onRegister' not in compiled and 'setReturnValue' not in compiled and 'cancel:' not in compiled, 'No dispatch/cancellation patch permitted')
    require('java.lang.Object bc$readTable(com.google.common.collect.Table' in compiled, 'Unsupported private Registration return coercion')

def check_archive(path):
    with zipfile.ZipFile(path) as archive:
        require(not any('/registratefixture/' in name or '/urnfixture/' in name or name.startswith('urn-oracle/') or name == 'better_compat_fixes.registrate_fixture.mixins.json'
            for name in archive.namelist()), 'Nonshipping Registrate fixture leaked into archive')

def source_and_archive_controls(evidence):
    repo = Path(__file__).resolve().parents[1]
    build = (repo / 'build.gradle.kts').read_text()
    require(build.count('source(registrateFixture)') == 1 and 'if (withRegistrateFixture) source(registrateFixture)' in build, 'Fixture source-set scope drift')
    require('if (withRegistrateFixture) add(registrateFixture.runtimeOnlyConfigurationName' in build, 'Optional runtime library not conditional')
    require('val mixinExtrasVersion = "0.5.4"' in build, 'Required MixinExtras API floor drift')
    require(build.count('config("better_compat_fixes.registrate.mixins.json")') == 1, 'Production source launcher config missing/duplicate')
    config = (repo / 'src/main/resources/better_compat_fixes.registrate.mixins.json').read_text()
    require('compat.registrate.RegistrateMixinPlugin' in config and 'mixin.registrate.RegistrateMixinPlugin' not in config, 'Plugin inside reserved mixin prefix')
    root = Path(evidence) / str(uuid.uuid4()); root.mkdir(parents=True)
    entries = ['better_compat_fixes.registrate_fixture.mixins.json',
        'com/bettercontent/bettercompatfixes/registratefixture/TransferGate.class',
        'com/bettercontent/bettercompatfixes/registratefixture/RegistrateGameTests.class',
        'com/bettercontent/bettercompatfixes/registratefixture/mixin/RegistrateStateAccessor.class',
        'com/bettercontent/bettercompatfixes/registratefixture/mixin/RegistrationCallbacksAccessor.class',
        'com/bettercontent/bettercompatfixes/registratefixture/mixin/RegistrateTransferGateMixin.class',
        'com/bettercontent/bettercompatfixes/urnfixture/UrnRandomScope.class',
        'com/bettercontent/bettercompatfixes/urnfixture/UrnOracleRegistration.class',
        'com/bettercontent/bettercompatfixes/urnfixture/oracle/PinnedUrnOracle.class',
        'urn-oracle/data/supplementaries/loot_tables/blocks/urn.json']
    for role in ['runtime', 'sources']:
        positive = root / f'{role}-positive.zip'
        with zipfile.ZipFile(positive, 'x') as archive:
            archive.writestr('better_compat_fixes.registrate.mixins.json', b'positive guard control only')
        check_archive(positive)
        for i, name in enumerate(entries):
            path = root / f'{role}-{i}.zip'
            if role == 'sources': name = name.replace('.class', '.java')
            with zipfile.ZipFile(path, 'x') as archive: archive.writestr(name, b'negative only')
            try: check_archive(path)
            except AssertionError: pass
            else: raise AssertionError('Fixture leak negative accepted')
    print('Retained source-only archive negatives:', root)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--raw'); parser.add_argument('--mapped'); parser.add_argument('--other-raw'); parser.add_argument('--classes')
    parser.add_argument('--evidence', default=str(Path(__file__).resolve().parents[1] / 'build/registrate-contract-controls'))
    parser.add_argument('--archive-only', action='store_true'); parser.add_argument('--archives', action='append', default=[])
    args = parser.parse_args()
    if args.archive_only:
        require(bool(args.archives), 'Missing archive input')
        for path in args.archives: check_archive(path)
    else:
        require(args.raw and args.mapped and args.other_raw and args.classes, 'Missing exact ABI inputs'); validate(args)
