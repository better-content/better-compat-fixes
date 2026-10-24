import com.bettercontent.bettercompatfixes.compat.deepvoid.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Nonshipping pure controls, no Minecraft engine or fake SDK classes. */
public final class DeepVoidPlacementControl {
    static int checks;
    static void need(boolean b) { if (!b) throw new AssertionError("control " + checks); checks++; }
    static void rejects(Runnable r) {
        try { r.run(); } catch (IllegalStateException expected) { checks++; return; }
        throw new AssertionError("unsupported accepted");
    }
    public static void main(String[] args) {
        int[][] sizes={{4,8,1},{3,6,1},{7,5,5},{12,18,4},{15,16,4},{11,10,19},{11,12,17},{11,13,10},{21,7,7},{4,12,10},{11,4,4}};
        int[] starts={-33,-16,-1,0,1,15,16,32,47};
        int cases=0;
        for(int[] size:sizes) for(int mirror=0;mirror<3;mirror++) for(int rot=0;rot<4;rot++)
            for(int cx:new int[]{-7,0,6}) for(int cz:new int[]{-4,0,9})
                for(int dx:starts) for(int dz:starts) {
                    int px=cx*16+dx,pz=cz*16+dz;
                    int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minZ=minX,maxZ=maxX;
                    for(int a:new int[]{0,size[0]-1}) for(int b:new int[]{0,size[2]-1}) {
                        int x=mirror==2?-a:a,z=mirror==1?-b:b;
                        for(int i=0;i<rot;i++){int n=-z;z=x;x=n;}
                        minX=Math.min(minX,px+x);maxX=Math.max(maxX,px+x);
                        minZ=Math.min(minZ,pz+z);maxZ=Math.max(maxZ,pz+z);
                    }
                    var shift=FleshTemplateBounds.fit(minX,maxX,minZ,maxZ,cx,cz,1);
                    int loX=cx*16-15,hiX=cx*16+30,loZ=cz*16-15,hiZ=cz*16+30;
                    need(minX+shift.x()>=loX && maxX+shift.x()<=hiX);
                    need(minZ+shift.z()>=loZ && maxZ+shift.z()<=hiZ);
                    need(shift.x()==Math.max(loX-minX,Math.min(0,hiX-maxX)));
                    need(shift.z()==Math.max(loZ-minZ,Math.min(0,hiZ-maxZ)));
                    need(!(minX>=loX&&maxX<=hiX&&minZ>=loZ&&maxZ<=hiZ)||shift.unchanged());
                    cases++;
                }
        need(cases==96228);
        rejects(()->FleshTemplateBounds.fit(0,46,0,1,0,0,1));
        rejects(()->FleshTemplateBounds.fit(0,1,0,1,0,0,0));
        rejects(()->FleshTemplateBounds.fit(0,1,0,1,Integer.MAX_VALUE,0,1));
        rejects(()->FleshTemplateBounds.fit(2,1,0,1,0,0,1));
        rejects(()->FleshTemplateBounds.translated(Integer.MAX_VALUE,1));
        FleshTemplateSemantics.settings(1,true,2,true,true,true,true,4);
        rejects(()->FleshTemplateSemantics.settings(2,true,2,true,true,true,true,4));
        rejects(()->FleshTemplateSemantics.settings(1,false,2,true,true,true,true,4));
        rejects(()->FleshTemplateSemantics.settings(1,true,3,true,true,true,true,4));
        rejects(()->FleshTemplateSemantics.settings(1,true,2,false,true,true,true,4));
        rejects(()->FleshTemplateSemantics.settings(1,true,2,true,true,false,true,4));
        rejects(()->FleshTemplateSemantics.settings(1,true,2,true,true,true,false,4));
        rejects(()->FleshTemplateSemantics.settings(1,true,2,true,true,true,true,3));
        FleshTemplateSemantics.size(19,18,19,2,true); // bounded changed override, not exact-byte equality
        rejects(()->FleshTemplateSemantics.size(47,1,1,1,true));
        rejects(()->FleshTemplateSemantics.size(1,1,1,0,true));
        rejects(()->FleshTemplateSemantics.size(1,1,1,1,false));
        rejects(()->FleshTemplateSemantics.size(1,0,1,1,true));
        FleshTemplateSemantics.block(1,2,3,19,18,19,true,false,false,false);
        FleshTemplateSemantics.block(1,2,3,19,18,19,true,false,true,true); // ignored structure-block payload
        rejects(()->FleshTemplateSemantics.block(-1,0,0,4,8,1,true,false,false,false));
        rejects(()->FleshTemplateSemantics.block(4,0,0,4,8,1,true,false,false,false));
        rejects(()->FleshTemplateSemantics.block(0,8,0,4,8,1,true,false,false,false));
        rejects(()->FleshTemplateSemantics.block(0,0,1,4,8,1,true,false,false,false));
        rejects(()->FleshTemplateSemantics.block(0,0,0,4,8,1,false,false,false,false));
        rejects(()->FleshTemplateSemantics.block(0,0,0,4,8,1,true,true,false,false));
        rejects(()->FleshTemplateSemantics.block(0,0,0,4,8,1,true,false,true,false));
        need(!FleshTemplateSemantics.recognizes("the_deep_void:unrelated"));
        need(FleshTemplateSemantics.recognizes("the_deep_void:flesh_rib1"));
        for (String id : new String[]{"broken_bone1", "broken_bone2", "broken_bone3new"}) {
            need(FleshTemplateSemantics.recognizes("the_deep_void:" + id));
            need(FleshTemplateSemantics.isBrokenBone("the_deep_void:" + id));
        }
        need(!FleshTemplateSemantics.isBrokenBone("the_deep_void:flesh_rib1"));
        need(!FleshTemplateSemantics.recognizes("other:broken_bone1"));
        FleshTemplateSemantics.boneSettings(1,true,1,false,true,true,true,4);
        rejects(()->FleshTemplateSemantics.boneSettings(1,true,2,true,true,true,true,4));
        rejects(()->FleshTemplateSemantics.boneSettings(1,true,1,false,false,true,true,4));
        rejects(()->FleshTemplateSemantics.boneSettings(1,true,1,false,true,false,true,4));
        rejects(()->FleshTemplateSemantics.boneSettings(1,true,1,false,true,true,false,4));
        rejects(()->FleshTemplateSemantics.boneSettings(1,true,1,false,true,true,true,3));
        record P(int x,int z) {}
        P p=new P(1,2),q=new P(3,4);Object value=new Object();AtomicInteger calls=new AtomicInteger();
        Object out=FleshTemplateCall.call(p,q,new FleshTemplateBounds.Shift(0,0),
            (a,s)->{throw new AssertionError("safe translated");},(a,b)->{need(a==p&&b==q);calls.incrementAndGet();return value;});
        need(out==value&&calls.get()==1);
        out=FleshTemplateCall.call(p,q,new FleshTemplateBounds.Shift(5,-7),
            (a,s)->new P(a.x()+s.x(),a.z()+s.z()),(a,b)->{need(a.equals(new P(6,-5))&&b.equals(new P(8,-3)));calls.incrementAndGet();return value;});
        need(out==value&&calls.get()==2);
        need(!FleshTemplateCall.call(p,p,new FleshTemplateBounds.Shift(1,1),
            (a,s)->new P(a.x()+s.x(),a.z()+s.z()),(a,b)->{need(a==b);return false;}));
        RuntimeException exact=new RuntimeException("original");
        try { FleshTemplateCall.call(p,q,new FleshTemplateBounds.Shift(0,0),(a,s)->a,(a,b)->{throw exact;});
            throw new AssertionError("original exception swallowed"); }
        catch(RuntimeException ex){need(ex==exact);}
        AtomicInteger invalidCalls=new AtomicInteger();
        rejects(()->FleshTemplateCall.call(p,q,FleshTemplateBounds.fit(0,99,0,1,0,0,1),
            (a,s)->a,(a,b)->{invalidCalls.incrementAndGet();return true;}));
        need(invalidCalls.get()==0);
        System.out.println("PASS DeepVoid pure controls: "+checks+" assertions; "+cases+" coordinate cases; NOT native acceptance");
    }
}
