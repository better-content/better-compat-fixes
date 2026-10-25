import java.security.MessageDigest
import java.util.zip.ZipFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

String sha(byte[] bytes) { MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString() }
ClassNode node(byte[] bytes) { def n = new ClassNode(); new ClassReader(bytes).accept(n, 0); n }
byte[] entry(ZipFile zip, String path) { def e = zip.getEntry(path); assert e != null : path; zip.getInputStream(e).withCloseable { it.bytes } }
def calls = { method -> method.instructions.toArray().findAll { it instanceof MethodInsnNode } }
def method = { n, name -> def found = n.methods.findAll { it.name == name }; assert found.size() == 1 : name; found[0] }
def rejected = { Closure control -> boolean failed = false; try { control() } catch (AssertionError expected) { failed = true }; assert failed : 'negative control accepted' }
def upstream = { ClassNode n ->
    def create = method(n, 'createAndLoadConfigs'); def invoked = calls(create)
    def expected = [
        ['net/minecraftforge/fml/ModLoadingContext', 'registerConfig'],
        ['com/electronwill/nightconfig/core/file/GenericBuilder', 'autoreload'],
        ['com/electronwill/nightconfig/core/file/GenericBuilder', 'sync'],
        ['com/electronwill/nightconfig/core/file/GenericBuilder', 'build'],
        ['com/electronwill/nightconfig/core/file/CommentedFileConfig', 'load'],
        ['net/minecraftforge/common/ForgeConfigSpec', 'setConfig']]
    def positions = expected.collect { pair ->
        def matches = invoked.findAll { it.owner == pair[0] && it.name == pair[1] }
        assert matches.size() == 1 : pair
        invoked.indexOf(matches[0])
    }
    assert positions == positions.sort(false)
    def setup = method(n, 'setup')
    def names = setup.instructions.toArray().findAll { it instanceof LdcInsnNode && it.cst instanceof String }.collect { it.cst }
    assert names.findAll { it.startsWith('the_bumblezone/') }.toSet() == [
        'the_bumblezone/client.toml','the_bumblezone/general.toml','the_bumblezone/worldgen.toml',
        'the_bumblezone/dimension.toml','the_bumblezone/bee_aggression.toml','the_bumblezone/mod_compatibility.toml'].toSet()
    assert calls(setup).count { it.name == 'createAndLoadConfigs' } == 6
}
def writerContract = { ClassNode n ->
    def write = n.methods.find { it.name == 'write' && it.desc.contains('Ljava/nio/file/Path;') }; assert write != null
    def invoked = calls(write)
    assert invoked.count { it.owner == 'com/electronwill/nightconfig/core/io/ConfigWriter' && it.name == 'write' } == 1
    assert invoked.count { it.owner == 'java/nio/channels/FileChannel' && it.name == 'force' } == 1
    def move = invoked.find { it.owner == 'java/nio/file/Files' && it.name == 'move' }; assert move != null
    def touch = invoked.find { it.owner == 'java/nio/file/Files' && it.name == 'setLastModifiedTime' }; assert touch != null
    assert invoked.indexOf(move) < invoked.indexOf(touch)
    def fields = write.instructions.toArray().findAll { it instanceof FieldInsnNode && it.owner == 'java/nio/file/StandardCopyOption' }*.name
    assert fields.toSet() == ['ATOMIC_MOVE','REPLACE_EXISTING'].toSet()
    assert !invoked.any { it.owner.contains('FileWatcher') || it.name == 'sleep' }
}
new ZipFile(args[1]).withCloseable { raw ->
    byte[] bytes = entry(raw, 'com/telepathicgrunt/the_bumblezone/configs/forge/BzConfigHandler.class')
    def pinned = { byte[] candidate -> assert sha(candidate) == '7055df824f0a9af3221846c3189bfa1b1a7e23f5209b33c6a5b11476dc9d1fcf' }
    pinned(bytes); upstream(node(bytes))
    byte[] changed = Arrays.copyOf(bytes, bytes.length); changed[changed.length - 1] = (byte)(changed[changed.length - 1] ^ 1); rejected { pinned(changed) }
    for (String removed : ['registerConfig','autoreload','build','load','setConfig']) {
        def mutant = node(bytes); def call = calls(method(mutant, 'createAndLoadConfigs')).find { it.name == removed }
        call.name = 'unreviewed'; rejected { upstream(mutant) }
    }
}
new ZipFile(args[2]).withCloseable { forge ->
    byte[] configBytes = entry(forge, 'net/minecraftforge/fml/config/ModConfig.class')
    byte[] handlerBytes = entry(forge, 'net/minecraftforge/fml/config/ConfigFileTypeHandler.class')
    assert sha(configBytes) == 'fba955a7c64bc94fe282f6adfc0cd1de8606928257efbfa4651eae2be933c95c'
    assert sha(handlerBytes) == 'b95f4ac0e77289b016c02f1d5d07fb35ad0c4792423ef80aa92ba3620058d42e'
    def extensible = { ClassNode n, String name ->
        def selected = method(n, name)
        assert (selected.access & Opcodes.ACC_PUBLIC) && !(selected.access & (Opcodes.ACC_FINAL | Opcodes.ACC_STATIC))
    }
    extensible(node(configBytes), 'getHandler'); extensible(node(handlerBytes), 'reader')
    def mutant = node(configBytes); method(mutant, 'getHandler').access |= Opcodes.ACC_FINAL
    rejected { extensible(mutant, 'getHandler') }
}
new ZipFile(args[3]).withCloseable { night ->
    ['FileWatcher':'28a63770b31483b1d9add6454417f287161ba5469626d728296fa52133edb9ce',
     'AutoreloadFileConfig':'8139526591bcf2c2ea567718c2f057263b73cd68f3349bf739ef5386365a39b6',
     'GenericBuilder':'bbaf15a76d7931ec1dba836f21a6be6a31a7ec4490019ff11754f5e86c2c7380'].each { name, hash ->
        assert sha(entry(night, 'com/electronwill/nightconfig/core/file/' + name + '.class')) == hash
    }
    def watcher = node(entry(night, 'com/electronwill/nightconfig/core/file/FileWatcher.class'))
    def add = watcher.methods.find { it.name == 'addWatch' && it.desc.startsWith('(Ljava/nio/file/Path;') }; assert add != null
    assert calls(add).count { it.owner == 'java/util/Map' && it.name == 'computeIfAbsent' } == 2
}
new ZipFile(args[0]).withCloseable { runtime ->
    byte[] bytes = entry(runtime, 'com/bettercontent/bettercompatfixes/compat/bumblezone/AtomicConfigWriter.class')
    writerContract(node(bytes))
    for (String removed : ['move','setLastModifiedTime','force']) {
        def mutant = node(bytes); def write = mutant.methods.find { it.name == 'write' && it.desc.contains('Ljava/nio/file/Path;') }
        calls(write).find { it.name == removed }.name = 'unreviewed'; rejected { writerContract(mutant) }
    }
    String prefix = 'com/bettercontent/bettercompatfixes/compat/bumblezone/'
    def bridge = node(entry(runtime, prefix + 'BumblezoneAtomicConfig.class'))
    def all = bridge.methods.collectMany { calls(it) }
    assert all.any { it.owner == 'net/minecraftforge/fml/ModContainer' && it.name == 'addConfig' }
    assert !all.any { it.owner.contains('FileWatcher') || it.owner.startsWith('java/lang/reflect/') || it.name in ['removeWatch','setWatch','setAccessible','getDeclaredField'] }
    def config = node(entry(runtime, prefix + 'BumblezoneAtomicConfig$AtomicModConfig.class'))
    assert config.superName == 'net/minecraftforge/fml/config/ModConfig'
    assert method(config, 'getHandler').desc == '()Lnet/minecraftforge/fml/config/ConfigFileTypeHandler;'
    def handler = node(entry(runtime, prefix + 'BumblezoneAtomicConfig$AtomicModConfig$AtomicHandler.class'))
    assert calls(method(handler, 'reader')).count { it.opcode == Opcodes.INVOKESPECIAL && it.owner == 'net/minecraftforge/fml/config/ConfigFileTypeHandler' && it.name == 'reader' } == 1
    assert !handler.methods.any { it.name == 'unload' }
    def mixin = node(entry(runtime, 'com/bettercontent/bettercompatfixes/mixin/bumblezone/BzConfigHandlerMixin.class'))
    def hook = method(mixin, 'betterContent$atomicConfig')
    assert calls(hook).count { it.owner == prefix + 'BumblezoneAtomicConfig' && it.name == 'register' } == 1
    def redirect = (hook.visibleAnnotations + (hook.invisibleAnnotations ?: [])).find { it.desc == 'Lorg/spongepowered/asm/mixin/injection/Redirect;' }; assert redirect != null
    def values = redirect.values.collate(2).collectEntries { [(it[0]):it[1]] }
    assert values.require == 1 && values.remap == false && values.method == ['createAndLoadConfigs']
    for (String hookName : ['betterContent$ownManualReload', 'betterContent$buildManualReload']) {
        def ownedHook = method(mixin, hookName)
        def annotation = (ownedHook.visibleAnnotations + (ownedHook.invisibleAnnotations ?: [])).find { it.desc == 'Lorg/spongepowered/asm/mixin/injection/Redirect;' }
        assert annotation != null
        def configValues = annotation.values.collate(2).collectEntries { [(it[0]):it[1]] }
        assert configValues.require == 1 && configValues.remap == false && configValues.method == ['createAndLoadConfigs']
        assert calls(ownedHook).count { it.owner == prefix + 'BumblezoneManualReload' } == 1
    }
    def manualContract = { ClassNode manual ->
        def invoked = manual.methods.collectMany { calls(it) }
        assert invoked.count { it.owner == 'com/electronwill/nightconfig/core/file/FileWatcher' && it.name == 'addWatch' } == 1
        assert !invoked.any { it.name in ['removeWatch','setWatch','stop','sleep','setAccessible','getDeclaredField'] }
        def handles = manual.methods.collectMany { it.instructions.toArray().findAll { it instanceof InvokeDynamicInsnNode }.collectMany { it.bsmArgs.toList() } }.findAll { it instanceof org.objectweb.asm.Handle }
        assert handles.count { it.name == 'load' && it.owner.startsWith('com/electronwill/nightconfig/core/file/') } == 1
    }
    byte[] manualBytes = entry(runtime, prefix + 'BumblezoneManualReload.class'); manualContract(node(manualBytes))
    def noWatch = node(manualBytes); noWatch.methods.collectMany { calls(it) }.find { it.name == 'addWatch' }.name = 'unreviewed'
    rejected { manualContract(noWatch) }
    String jsonName = 'better_compat_fixes.bumblezone_config.mixins.json'
    def json = new groovy.json.JsonSlurper().parseText(new String(entry(runtime, jsonName), 'UTF-8'))
    assert json.required && json.injectors.defaultRequire == 1 && json.mixins == ['BzConfigHandlerMixin']
    assert json.plugin == 'com.bettercontent.bettercompatfixes.compat.bumblezone.BumblezoneConfigMixinPlugin'
    def manifest = new java.util.jar.Manifest(new ByteArrayInputStream(entry(runtime, 'META-INF/MANIFEST.MF')))
    assert jsonName in manifest.mainAttributes.getValue('MixinConfigs').split(',')
    assert !runtime.entries().toList().any { it.name.startsWith('com/bettercontent/bettercompatfixes/nativefixture/') }
}
println 'Bumblezone pinned dual-loader ABI, atomic writer/publication+watch notification, registration and negative controls PASS'
