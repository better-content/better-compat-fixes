import groovy.json.JsonSlurper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.security.MessageDigest
import java.util.zip.ZipFile

def need = { boolean ok, String message -> if (!ok) throw new IllegalStateException(message) }
def sha = { byte[] bytes -> MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString() }
def requirePin = { byte[] bytes, String expected -> need(sha(bytes) == expected, 'Unknown target class pin') }
def node = { byte[] bytes -> def n = new ClassNode(); new ClassReader(bytes).accept(n, 0); n }
def values = { AnnotationNode a -> def result = [:]; for (int i=0; i<(a?.values?.size() ?: 0); i+=2) result[a.values[i]]=a.values[i+1]; result }
def annotations = { MethodNode m -> (m.visibleAnnotations ?: []) + (m.invisibleAnnotations ?: []) }
def check = { ClassNode target, ClassNode mixin, int expected ->
    int total = 0
    mixin.methods.each { handler ->
        def expression = annotations(handler).find { it.desc == 'Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;' }
        if (expression == null) return
        def data = values(expression)
        def at = values(data.at)
        String field = at.target
        def selected = target.methods.findAll { method -> data.method.any { selector ->
            selector.contains('(') ? selector == method.name + method.desc : selector == method.name
        } }
        int count = selected.sum { method -> method.instructions.toArray().count { insn ->
            insn instanceof FieldInsnNode && insn.opcode == Opcodes.GETSTATIC &&
                field == 'L' + insn.owner + ';' + insn.name + ':' + insn.desc
        } } ?: 0
        total += count
        def group = values(annotations(handler).find { it.desc == 'Lorg/spongepowered/asm/mixin/injection/Group;' })
        need(group.name == 'wormTerrain' && (group.min as int) == expected && (group.max as int) == expected,
            'Exact required group weakened/widened')
        need(data.require == 0 && data.remap == false && at.remap == false, 'Alias-local admission changed')
        def body = handler.instructions.toArray().findAll { it.opcode >= 0 }
        need(body.size() == 2 && body[0] instanceof FieldInsnNode && body[0].opcode == Opcodes.GETSTATIC &&
            body[0].owner == 'com/bettercontent/bettercompatfixes/compat/DeathWormTerrain' && body[0].name == 'BURROWABLE' &&
            body[1].opcode == Opcodes.ARETURN, 'Original terrain replacement body changed')
    }
    need(total == expected, 'Missing/duplicate method/field aliases: expected=' + expected + ', actual=' + total)
}
need(args.size() == 4, 'pins raw mapped compiled-classes required')
def pins = new JsonSlurper().parse(new File(args[0]))
int checks = 0
['raw','mapped'].eachWithIndex { role, index ->
    new ZipFile(args[index+1]).withCloseable { zip ->
        need(sha(new File(args[index+1]).bytes) == pins[role].sha256, 'Unknown whole Ice and Fire SDK pin')
        pins.targets.each { name, spec ->
            byte[] targetBytes = zip.getInputStream(zip.getEntry(spec.target.replace('.','/')+'.class')).bytes
            requirePin(targetBytes, pins[role].classes[spec.target])
            def target = node(targetBytes)
            File compiled = new File(args[3], 'com/bettercontent/bettercompatfixes/mixin/iceandfire/' + name + '.class')
            def mixin = node(compiled.bytes)
            check(target, mixin, spec.sites as int); checks++
            // A missing mapped override used to be silently hidden by another method in the group.
            if (role == 'mapped' && spec.officialAlias) {
                mixin.methods.each { method -> annotations(method).findAll {
                    it.desc == 'Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;'
                }.each { a ->
                    for(int i=0;i<a.values.size();i+=2) if(a.values[i]=='method')
                        a.values[i+1] = a.values[i+1].findAll { !it.startsWith(spec.officialAlias + '(') }
                } }
                boolean failed=false
                try { check(target,mixin,spec.sites as int) } catch(IllegalStateException expected) { failed=true }
                need(failed,'Missing mapped alias negative accepted'); checks++
            }
            byte[] changed = java.util.Arrays.copyOf(targetBytes, targetBytes.length)
            changed[changed.length-1] = (byte)(changed[changed.length-1] ^ 1)
            boolean pinFailed = false
            try { requirePin(changed, pins[role].classes[spec.target]) } catch (IllegalStateException expected) { pinFailed = true }
            need(pinFailed, 'Unknown class pin negative accepted'); checks++
        }
    }
}
println('PASS DeathWorm actual raw/mapped ASM controls: ' + checks + '; original field replacements only; NOT native composition')
