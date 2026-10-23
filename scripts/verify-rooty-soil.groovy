import groovy.json.JsonSlurper
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.security.MessageDigest
import java.util.zip.ZipFile
import com.bettercontent.bettercompatfixes.compat.dynamictrees.RootySoilPolicy

def need={boolean b,String why->if(!b)throw new IllegalStateException(why)}
def sha={byte[] b->MessageDigest.getInstance('SHA-256').digest(b).encodeHex().toString()}
def node={byte[] b->def n=new ClassNode();new ClassReader(b).accept(n,0);n}
def annotations={n->(n.visibleAnnotations?:[])+(n.invisibleAnnotations?:[])}
def values={a->def v=[:];for(int i=0;i<(a?.values?.size()?:0);i+=2)v[a.values[i]]=a.values[i+1];v}
need(args.size()==4,'pins raw mapped classes required')
def pins=new JsonSlurper().parse(new File(args[0]));def root=new File(args[3]);int checks=0
String descriptor='(Lcom/ferreusveritas/dynamictrees/worldgen/GenerationContext;)V'
String conversion='(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;I)Z'
def validate={ClassNode n->
 def method=n.methods.find{it.name=='generate'&&it.desc==descriptor};need(method!=null,'Unknown generate descriptor')
 def sites=method.instructions.toArray().findAll{it instanceof MethodInsnNode&&it.owner=='com/ferreusveritas/dynamictrees/tree/species/Species'&&it.name=='placeRootyDirtBlock'&&it.desc==conversion}
 need(sites.size()==1,'Missing/duplicate conversion site');int site=method.instructions.indexOf(sites[0])
 [5:'Z',7:'Lnet/minecraft/world/level/block/state/BlockState;'].each{slot,type->
  need(method.localVariables.count{it.index==slot&&it.desc==type&&method.instructions.indexOf(it.start)<=site&&method.instructions.indexOf(it.end)>site}==1,'Missing/changed live final-root/worldgen local')
 }
 def before=method.instructions.toArray().take(site)
 need(before.count{it instanceof MethodInsnNode&&it.name=='preGeneration'}==1,'Native preGeneration prefix changed')
 def snapshots=before.findAll{it instanceof MethodInsnNode&&it.desc=='(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;'}
 need(snapshots.size()==1,'Existing final state read changed')
 def store=snapshots[0].next;while(store!=null&&store.opcode<0)store=store.next
 need(store instanceof VarInsnNode&&store.opcode==Opcodes.ASTORE&&store.var==7,'State snapshot no longer slot7')
 def next=sites[0].next;while(next!=null&&next.opcode<0)next=next.next
 need(next.opcode==Opcodes.POP,'Original ignored conversion result changed')
 need(!before.any{it instanceof MethodInsnNode&&it.name=='generateFork'},'Guard after branch writes')
}
['raw','mapped'].eachWithIndex{role,i->new ZipFile(args[i+1]).withCloseable{z->
 need(sha(new File(args[i+1]).bytes)==pins[role].sha256,'Unknown DT SDK')
 pins[role].classes.each{name,hash->need(sha(z.getInputStream(z.getEntry(name)).bytes)==hash,'Unknown class pin');checks++}
 byte[] original=z.getInputStream(z.getEntry('com/ferreusveritas/dynamictrees/worldgen/JoCode.class')).bytes
 validate(node(original));checks++
 def changed=node(original);changed.methods.find{it.name=='generate'&&it.desc==descriptor}.localVariables.find{it.index==7}.desc='I'
 boolean rejected=false;try{validate(changed)}catch(IllegalStateException expected){rejected=true};need(rejected,'Changed local negative accepted');checks++
 changed=node(original);changed.methods.find{it.name=='generate'&&it.desc==descriptor}.instructions.toArray().find{it instanceof MethodInsnNode&&it.name=='placeRootyDirtBlock'}.name='missing'
 rejected=false;try{validate(changed)}catch(IllegalStateException expected){rejected=true};need(rejected,'Missing invoke negative accepted');checks++
}}
def mixin=node(new File(root,'com/bettercontent/bettercompatfixes/mixin/dynamictrees/JoCodeRootySoilAdmissionMixin.class').bytes)
def target=values(annotations(mixin).find{it.desc.endsWith('/Mixin;')})
need(target.remap==false&&target.value==[Type.getObjectType('com/ferreusveritas/dynamictrees/worldgen/JoCode')],'Wrong pinned target class')
def handler=mixin.methods.find{it.name=='better_compat_fixes$admitFinalRoot'};def inject=values(annotations(handler).find{it.desc.endsWith('/Inject;')})
need(inject.require==1&&inject.allow==1&&inject.cancellable==true&&inject.remap==false&&inject.method==['generate'+descriptor],'Required final-root injection weakened')
def at=values(inject.at[0]);need(at.value=='INVOKE'&&at.remap==false&&at.target=='Lcom/ferreusveritas/dynamictrees/tree/species/Species;placeRootyDirtBlock'+conversion,'Wrong cancellation anchor')
[2:5,3:7].each{parameter,slot->def a=(handler.visibleParameterAnnotations?[parameter]?:[])+(handler.invisibleParameterAnnotations?[parameter]?:[]);need(values(a.find{it.desc.endsWith('/Local;')}).index==slot,'Wrong existing-local capture')}
def adapter=node(new File(root,'com/bettercontent/bettercompatfixes/compat/dynamictrees/RootySoilAdmission.class').bytes)
need(adapter.fields.isEmpty(),'Retained world/state')
need(adapter.methods.collectMany{it.instructions.toArray().findAll{it.opcode==Opcodes.INSTANCEOF}}*.desc==['com/ferreusveritas/dynamictrees/block/rooty/RootyBlock'],'Wrong existing root classification')
def calls=adapter.methods.collectMany{it.instructions.toArray().findAll{it instanceof MethodInsnNode}}
need(calls.findAll{it.name!='<init>'}.collect{it.owner+'.'+it.name}.sort()==[
 'net/minecraft/world/level/block/state/BlockState.getBlock',
 'com/ferreusveritas/dynamictrees/block/rooty/SoilHelper.isSoilRegistered',
 'com/bettercontent/bettercompatfixes/compat/dynamictrees/RootySoilPolicy.reject'].sort(),'Extra world/BE/RNG/getter/logger work')
need(handler.instructions.toArray().count{it instanceof MethodInsnNode&&it.name=='cancel'}==1,'Whole generation not cancelled')
def opcodes={m->m.instructions.toArray().findAll{it.opcode>=0}.collect{it.opcode}}
need(opcodes(adapter.methods.find{it.name=='reject'})==[
 Opcodes.ILOAD,Opcodes.IFNE,Opcodes.ICONST_0,Opcodes.IRETURN,Opcodes.ALOAD,Opcodes.INVOKEVIRTUAL,
 Opcodes.ASTORE,Opcodes.ICONST_1,Opcodes.ALOAD,Opcodes.INVOKESTATIC,Opcodes.ALOAD,Opcodes.INSTANCEOF,
 Opcodes.INVOKESTATIC,Opcodes.IRETURN], 'Nonworldgen early return/metadata forwarding changed')
need(opcodes(handler)==[Opcodes.ILOAD,Opcodes.ALOAD,Opcodes.INVOKESTATIC,Opcodes.IFEQ,
 Opcodes.ALOAD,Opcodes.INVOKEVIRTUAL,Opcodes.RETURN], 'Reject-only cancellation forwarding changed')
checks++
// Exactly eight primitive admission cases, not a new native fixture or broad test framework.
[false,true].each{w->[false,true].each{registered->[false,true].each{rooty->
 need(RootySoilPolicy.reject(w,registered,rooty)==(w&&!registered&&!rooty),'Admission truth table');checks++
}}}
println('PASS Rooty final-root actual raw/mapped site/local/read and eight policy controls: '+checks+'; NOT native composition')
