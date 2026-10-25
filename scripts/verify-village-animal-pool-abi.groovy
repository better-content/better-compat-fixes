import java.security.MessageDigest
import java.util.jar.Manifest
import java.util.zip.ZipFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode

assert args.length == 2
def read = { byte[] bytes -> def node = new ClassNode(); new ClassReader(bytes).accept(node, 0); node }
def sha = { byte[] bytes -> MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString() }
def pool = 'net/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool'
new ZipFile(args[1]).withCloseable { zip ->
    byte[] bytes = zip.getInputStream(zip.getEntry('mokiyoki/enhancedanimals/GeneticAnimals.class')).bytes
    assert sha(bytes) == 'c970a1c91f29b99017e85ab7d853d78fd1df376af48740aa103ae830a69b97ee': 'Unreviewed upstream village listener'
    def valid = { node ->
        def method = node.methods.find { it.name == 'addAnimalsToVillages' && it.desc == '(Lnet/minecraftforge/event/server/ServerAboutToStartEvent;)V' }
        method != null && method.instructions.iterator().findAll {
            it instanceof FieldInsnNode && it.opcode == Opcodes.GETFIELD && it.owner == pool && it.name == 'f_210559_' && it.desc == 'Ljava/util/List;'
        }.size() == 5 && method.instructions.iterator().findAll {
            it instanceof MethodInsnNode && it.owner == 'java/util/List' && it.name == 'add' && it.desc == '(Ljava/lang/Object;)Z'
        }.size() == 18
    }
    def actual = read(bytes)
    assert valid(actual)
    def field = actual.methods.find { it.name == 'addAnimalsToVillages' }.instructions.iterator().find {
        it instanceof FieldInsnNode && it.name == 'f_210559_'
    }
    field.name = 'unknown_field'
    assert !valid(actual): 'Unknown upstream field must fail closed'
}
new ZipFile(args[0]).withCloseable { zip ->
    def at = zip.getInputStream(zip.getEntry('META-INF/accesstransformer.cfg')).text.readLines()
        .collect { it.split('#', 2)[0].trim() }.findAll { it }
    assert at == ['public-f net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool f_210559_']
    def manifest = new Manifest(zip.getInputStream(zip.getEntry('META-INF/MANIFEST.MF')))
    assert manifest.mainAttributes.getValue('FMLAT') == 'accesstransformer.cfg'
    assert zip.entries().iterator().every { !it.name.contains('VillageAnimalPoolGameTests') }
    byte[] bytes = zip.getInputStream(zip.getEntry('com/bettercontent/bettercompatfixes/compat/eanimod/VillageAnimalPoolMutability.class')).bytes
    def valid = { node ->
        def method = node.methods.find { it.name == 'beforeAnimalRegistration' && it.desc == '(Lnet/minecraftforge/event/server/ServerAboutToStartEvent;)V' }
        if (method == null) return false
        def annotation = method.visibleAnnotations.find { it.desc == 'Lnet/minecraftforge/eventbus/api/SubscribeEvent;' }
        def priority = annotation?.values
        if (priority == null || priority[0] != 'priority' || priority[1][1] != 'HIGHEST') return false
        def fields = method.instructions.iterator().findAll { it instanceof FieldInsnNode && it.owner == pool }
        fields.size() == 2 && fields.every { it.name == 'f_210559_' && it.desc == 'Ljava/util/List;' } &&
            fields.collect { it.opcode }.sort() == [Opcodes.GETFIELD, Opcodes.PUTFIELD].sort()
    }
    def actual = read(bytes)
    assert valid(actual): 'Production field namespace/priority changed'
    actual.methods.find { it.name == 'beforeAnimalRegistration' }.visibleAnnotations.find {
        it.desc == 'Lnet/minecraftforge/eventbus/api/SubscribeEvent;'
    }.values[1][1] = 'LOWEST'
    assert !valid(actual): 'A late listener must fail closed'
}
println 'Village animal raw ABI, production field/AT/priority, nonshipping boundary and negative controls PASS'
