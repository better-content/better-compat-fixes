import groovy.json.JsonSlurper
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.security.MessageDigest
import java.util.zip.*
import com.bettercontent.bettercompatfixes.compat.deepvoid.FleshTemplateSemantics

def need={boolean b,String why->if(!b)throw new IllegalStateException(why)}
def sha={byte[] b->MessageDigest.getInstance('SHA-256').digest(b).encodeHex().toString()}
def node={byte[] b->def n=new ClassNode();new ClassReader(b).accept(n,0);n}
def vals={AnnotationNode a->def v=[:];for(int i=0;i<(a?.values?.size()?:0);i+=2)v[a.values[i]]=a.values[i+1];v}
def anns={m->(m.visibleAnnotations?:[])+(m.invisibleAnnotations?:[])}
def pin={byte[] b,String expected->need(sha(b)==expected,'Unknown actual class/resource pin')}
need(args.size()==6,'pins raw mapped sdk sdk-srg classes required')
def p=new JsonSlurper().parse(new File(args[0]));def root=new File(args[5]);int checks=0
def compiled={String name->node(new File(root,'com/bettercontent/bettercompatfixes/'+name+'.class').bytes)}
def sites={ClassNode target,ClassNode mixin->
 int total=0
 mixin.methods.each{m->def a=anns(m).find{it.desc.endsWith('/WrapOperation;')};if(a==null)return
  def v=vals(a);def at=vals(v.at);def g=vals(anns(m).find{it.desc.endsWith('/Group;')})
  need(g.name=='fleshTemplate'&&g.min==1&&g.max==1&&v.require==0&&v.remap==false&&at.remap==false,'Required exact group/alias admission changed')
  need(m.desc.endsWith('Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z'),'Enclosing context/operation ABI changed')
  need(m.instructions.toArray().count{it instanceof MethodInsnNode&&it.owner.endsWith('/FleshTemplatePlacement')&&it.name=='place'}==1,'Handler must call original adapter once')
  target.methods.findAll{method->v.method.contains(method.name+method.desc)}.each{method->
   total+=method.instructions.toArray().count{it instanceof MethodInsnNode&&at.target=='L'+it.owner+';'+it.name+it.desc}
  }
 }
 need(total==1,'Missing/duplicate native WrapOperation site: '+total)
}
['void-raw','void-mapped'].eachWithIndex{role,i->
 new ZipFile(args[i+1]).withCloseable{z->
  pin(new File(args[i+1]).bytes,p[role].sha256)
  p[role].classes.each{name,hash->pin(z.getInputStream(z.getEntry(name.replace('.','/')+'.class')).bytes,hash);checks++}
  def target=node(z.getInputStream(z.getEntry('net/mcreator/thedeepvoid/world/features/StructureFeature.class')).bytes)
  def mixin=compiled('mixin/deepvoid/FleshStructureFeatureMixin');sites(target,mixin);checks++
  mixin.methods.each{m->anns(m).findAll{it.desc.endsWith('/WrapOperation;')}.each{a->for(int n=0;n<a.values.size();n+=2)if(a.values[n]=='method')a.values[n+1]=[]}}
  boolean bad=false;try{sites(target,mixin)}catch(IllegalStateException expected){bad=true};need(bad,'Missing selector negative accepted');checks++
  p.templates.each{t->pin(z.getInputStream(z.getEntry(t.path)).bytes,t.sha256);checks++}
 }
}
['sdk','sdk-srg'].eachWithIndex{role,i->
 new ZipFile(args[i+3]).withCloseable{z->
  p.fields.each{mixinName,spec->
   byte[] bytes=z.getInputStream(z.getEntry(spec.target.replace('.','/')+'.class')).bytes
   pin(bytes,p[role].classes[spec.target]);def target=node(bytes)
   def selectedName=role=='sdk-srg'?mixinName.replace('Accessor','SrgAccessor'):mixinName
   def mixin=compiled('mixin/deepvoid/'+selectedName)
   spec.fields.each{primary,pair->
    def fieldName=role=='sdk-srg'?pair[0]:primary
    def shadow=mixin.fields.find{it.name==fieldName&&it.desc==pair[1]};need(shadow!=null,'Missing namespace-exact typed metadata field')
    def a=vals(anns(shadow).find{it.desc=='Lorg/spongepowered/asm/mixin/Shadow;'})
    def shadowRule={v->need(v.remap==false&&!v.aliases&&anns(shadow).any{it.desc=='Lorg/spongepowered/asm/mixin/Final;'},'Metadata bridge must use exact fields, never non-private aliases')}
    shadowRule(a)
    def resolve={names->need(target.fields.count{it.desc==pair[1]&&names.contains(it.name)}==1,'Unknown/ambiguous actual metadata field')}
    resolve([fieldName])
    // Production access transformers may widen these fields. Exact selectors
    // remain valid; an alias fallback would be rejected by Mixin in that case.
    def actual=target.fields.find{it.desc==pair[1]&&it.name==fieldName};int access=actual.access
    actual.access=(access&~Opcodes.ACC_PRIVATE)|Opcodes.ACC_PUBLIC
    resolve([fieldName]);actual.access=access
    boolean aliasRejected=false;try{shadowRule(a+[aliases:['futureAlias']])}catch(IllegalStateException expected){aliasRejected=true}
    need(aliasRejected,'Former non-private alias regression admitted')
    def getters=mixin.methods.findAll{it.name.startsWith('better_compat_fixes$')&&it.instructions.toArray().any{it instanceof FieldInsnNode&&it.name==fieldName}}
    need(getters.size()==1,'Missing readonly getter')
    def body=getters[0].instructions.toArray().findAll{it.opcode>=0}
    need(body.collect{it.opcode}==[Opcodes.ALOAD,Opcodes.GETFIELD,pair[1]=='I'?Opcodes.IRETURN:Opcodes.ARETURN],'Metadata getter mutates/queries')
    boolean fieldRejected=false;try{resolve(['unknownNegativeField'])}catch(IllegalStateException expected){fieldRejected=true}
    need(fieldRejected,'Unknown field negative accepted');checks+=4
   }
   byte[] changed=Arrays.copyOf(bytes,bytes.length);changed[changed.length-1]=(byte)(changed[changed.length-1]^1)
   boolean rejected=false;try{pin(changed,p[role].classes[spec.target])}catch(IllegalStateException expected){rejected=true};need(rejected,'Unknown class negative accepted');checks++
  }
 }
}
def placement=compiled('compat/deepvoid/FleshTemplatePlacement')
need(placement.fields.isEmpty(),'Retained native/world/template state')
def calls=placement.methods.collectMany{it.instructions.toArray().findAll{it instanceof MethodInsnNode}}
need(!calls.any{it.name in ['getChunk','getBlockState','getBlockEntity','getRandom','getRandomPalette','save','setBlock','ensureCanWrite','getOrCreate']},'Added native query/write/RNG/serialization')
need(calls.any{it.owner.endsWith('StructureTemplate$Palette')&&it.name=='blocks'&&it.desc=='()Ljava/util/List;'}&&!calls.any{it.owner.endsWith('StructureTemplate$Palette')&&it.name=='blocks'&&it.desc!='()Ljava/util/List;'},'Mutating filtered palette cache used')
['pos','state','nbt'].each{name->need(calls.any{it.owner.endsWith('StructureTemplate$StructureBlockInfo')&&it.name==name},'Missing actual loaded info read')}
need(calls.any{it.owner.endsWith('/FleshTemplateSemantics')&&it.name=='block'}&&calls.any{it.owner.endsWith('/FleshTemplateCall')&&it.name=='call'},'Metadata/call guard disconnected');checks++
// Bounded test-only NBT decoder: packaged bytes pinned above; never production runtime serialization.
def tag;tag={int t,DataInputStream d->switch(t){
 case 1:return d.readByte();case 2:return d.readShort();case 3:return d.readInt();case 4:return d.readLong();case 5:return d.readFloat();case 6:return d.readDouble()
 case 7:return d.readNBytes(d.readInt());case 8:return d.readUTF()
 case 9:def sub=d.readUnsignedByte();int n=d.readInt();return (0..<n).collect{tag(sub,d)}
 case 10:def result=[:];while(true){int sub=d.readUnsignedByte();if(sub==0)return result;String name=d.readUTF();result[name]=tag(sub,d)}
 case 11:int n=d.readInt();return (0..<n).collect{d.readInt()}
 case 12:int n=d.readInt();return (0..<n).collect{d.readLong()}
 default:throw new IllegalStateException('Unsupported pinned test NBT tag')
}}
new ZipFile(args[1]).withCloseable{z->p.templates.each{t->
 def d=new DataInputStream(new GZIPInputStream(z.getInputStream(z.getEntry(t.path))));int type=d.readUnsignedByte();d.readUTF();def n=tag(type,d)
 need(n.size==t.size&&n.entities.isEmpty()&&n.palette!=null&&!n.containsKey('palettes')&&n.blocks.size()==t.blockCount,'Actual packaged metadata changed')
 FleshTemplateSemantics.size(*n.size,1,n.entities.isEmpty())
 n.blocks.each{b->def state=n.palette[b.state];boolean ignored=state.Name=='minecraft:structure_block';need(!b.containsKey('nbt')||ignored,'Accepted packaged NBT')
  FleshTemplateSemantics.block(*(b.pos+n.size+[true,state.Properties?.waterlogged=='true',b.containsKey('nbt'),ignored]))
 };checks++
}}
println('PASS DeepVoid actual raw/mapped SDK/compiled ABI and packaged semantic controls: '+checks+'; NOT native composition')
