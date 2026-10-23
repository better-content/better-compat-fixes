package com.bettercontent.bettercompatfixes.compat.caravan;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.Vec3;

/** NONSHIPPING API/ordinary-NBT experiment. No registry bootstrap, world, or entity creation. */
final class CaravanOriginalVectors {
    static int checks;
    static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    static BlockPos independent(BlockPos p, Mirror m, Rotation r, BlockPos pivot, BlockPos origin) {
        int x=p.getX(), z=p.getZ(), px=pivot.getX(), pz=pivot.getZ();
        if(m==Mirror.LEFT_RIGHT) z=-z;
        if(m==Mirror.FRONT_BACK) x=-x;
        int a=x,b=z;
        switch(r) {
            case CLOCKWISE_90 -> { a=px+pz-z; b=pz-px+x; }
            case CLOCKWISE_180 -> { a=2*px-x; b=2*pz-z; }
            case COUNTERCLOCKWISE_90 -> { a=px-pz+z; b=px+pz-x; }
            default -> {}
        }
        return new BlockPos(a,p.getY(),b).offset(origin);
    }
    static int verify() throws Exception {
        checks = 0;
        CompoundTag resource;
        try(var in=CaravanOriginalVectors.class.getResourceAsStream("/caravan/original-clown-caravan.nbt")) {
            if (in == null) throw new AssertionError("Pinned original template fixture missing");
            resource=NbtIo.readCompressed(in);
        }
        CompoundTag resourceBefore=resource.copy();
        ListTag entities=resource.getList("entities", Tag.TAG_COMPOUND);
        CompoundTag wrapper=entities.getCompound(0), original=wrapper.getCompound("nbt");
        check(original.getString("id").equals("minecraft:item_frame"), "actual first entity");
        check(original.getInt("TileX")==-22 && original.getInt("TileY")==-57 && original.getInt("TileZ")==11, "actual stale TileXYZ");
        ListTag b=wrapper.getList("blockPos",Tag.TAG_INT), v=wrapper.getList("pos",Tag.TAG_DOUBLE);
        BlockPos local=new BlockPos(b.getInt(0),b.getInt(1),b.getInt(2));
        Vec3 localVec=new Vec3(v.getDouble(0),v.getDouble(1),v.getDouble(2));
        CaravanAnchorProcessor processor=CaravanAnchorProcessor.INSTANCE;
        int cases=0;
        for(Mirror mirror:Mirror.values()) for(Rotation rotation:Rotation.values())
        for(BlockPos origin:List.of(BlockPos.ZERO,new BlockPos(123,64,-987),new BlockPos(-1000000,-64,1000000),new BlockPos(25000000,300,-25000000)))
        for(BlockPos pivot:List.of(BlockPos.ZERO,new BlockPos(9,3,14))) {
            cases++;
            StructurePlaceSettings settings=new StructurePlaceSettings().setMirror(mirror).setRotation(rotation).setRotationPivot(pivot);
            BlockPos world=StructureTemplate.transform(local,mirror,rotation,pivot).offset(origin);
            check(world.equals(independent(local,mirror,rotation,pivot,origin)), "independent WORLD transform");
            Vec3 worldVec=StructureTemplate.transform(localVec,mirror,rotation,pivot).add(origin.getX(),origin.getY(),origin.getZ());
            var raw=new StructureTemplate.StructureEntityInfo(localVec,local,original);
            var current=new StructureTemplate.StructureEntityInfo(worldVec,world,original);
            var fixed=processor.processEntity(null,origin,raw,current,settings,null);
            check(fixed!=current && fixed.nbt!=original, "copy, not shared mutation");
            check(fixed.blockPos.equals(world) && fixed.pos.equals(worldVec), "SDK transformed wrapper unchanged");
            for(String key:original.getAllKeys()) {
                if(!Set.of("TileX","TileY","TileZ").contains(key)) check(original.get(key).equals(fixed.nbt.get(key)), "unchanged typed NBT "+key);
            }
            check(original.getAllKeys().equals(fixed.nbt.getAllKeys()), "no key additions/deletions");
            check(fixed.nbt.getInt("TileX")==world.getX() && fixed.nbt.getInt("TileY")==world.getY() && fixed.nbt.getInt("TileZ")==world.getZ(), "three WORLD ints");
            for(String key:List.of("TileX","TileY","TileZ")) check(fixed.nbt.contains(key,Tag.TAG_INT), "integer type "+key);
            check(fixed.nbt.get("Pos").equals(original.get("Pos")), "processor leaves Pos for SDK authoritative overwrite");
            check(processor.processEntity(null,origin,raw,fixed,settings,null).nbt.equals(fixed.nbt), "idempotent");
        }
        for(int i=1;i<entities.size();i++) {
            CompoundTag nbt=entities.getCompound(i).getCompound("nbt");
            var other=new StructureTemplate.StructureEntityInfo(Vec3.ZERO,BlockPos.ZERO,nbt);
            check(processor.processEntity(null,BlockPos.ZERO,other,other,new StructurePlaceSettings(),null)==other, "other authored entity unchanged");
        }
        CompoundTag otherFrame=original.copy(); otherFrame.putString("id","minecraft:glow_item_frame");
        var other=new StructureTemplate.StructureEntityInfo(Vec3.ZERO,BlockPos.ZERO,otherFrame);
        check(processor.processEntity(null,BlockPos.ZERO,other,other,new StructurePlaceSettings(),null)==other,"outside exact entity kind unchanged");
        CompoundTag missing=original.copy(); missing.remove("TileX");
        var malformed=new StructureTemplate.StructureEntityInfo(Vec3.ZERO,BlockPos.ZERO,missing);
        check(processor.processEntity(null,BlockPos.ZERO,malformed,malformed,new StructurePlaceSettings(),null)==malformed,"missing typed anchor not silently repaired");
        CompoundTag wrongType=original.copy(); wrongType.putByte("TileX",(byte)2);
        malformed=new StructureTemplate.StructureEntityInfo(Vec3.ZERO,BlockPos.ZERO,wrongType);
        check(processor.processEntity(null,BlockPos.ZERO,malformed,malformed,new StructurePlaceSettings(),null)==malformed,"wrong typed anchor not silently repaired");
        var empty=new StructureTemplate.StructureEntityInfo(Vec3.ZERO,BlockPos.ZERO,null);
        check(processor.processEntity(null,BlockPos.ZERO,empty,empty,new StructurePlaceSettings(),null)==empty,"null NBT unchanged");
        RandomSource rng=RandomSource.create(12345);
        StructurePlaceSettings settings=new StructurePlaceSettings().setMirror(Mirror.FRONT_BACK).setRotation(Rotation.CLOCKWISE_90)
            .setRotationPivot(new BlockPos(3,4,5)).setIgnoreEntities(false).setKnownShape(true).setFinalizeEntities(true)
            .setKeepLiquids(false).setRandom(rng).setBoundingBox(new BoundingBox(-10,-20,-30,40,50,60)).addProcessor(processor);
        StructurePlaceSettings copy=settings.copy().addProcessor(processor);
        check(copy.getMirror()==settings.getMirror() && copy.getRotation()==settings.getRotation(),"mirror rotation copy");
        check(copy.getRotationPivot().equals(settings.getRotationPivot()) && copy.getBoundingBox()==settings.getBoundingBox(),"pivot bounds copy");
        check(copy.isIgnoreEntities()==settings.isIgnoreEntities() && copy.getKnownShape()==settings.getKnownShape(),"entity knownshape flags copy");
        check(copy.shouldFinalizeEntities()==settings.shouldFinalizeEntities() && copy.shouldKeepLiquids()==settings.shouldKeepLiquids(),"finalize liquids flags copy");
        check(copy.getRandom(null)==rng && settings.getRandom(null)==rng,"same RNG without a draw");
        check(rng.nextLong()==RandomSource.create(12345).nextLong(),"copy/addProcessor consumed no RNG");
        check(settings.getProcessors().size()==1 && copy.getProcessors().size()==2 && copy.getProcessors().get(0)==processor,"original processor order and settings unmodified");
        check(resource.equals(resourceBefore),"entire original typed resource unchanged");
        if (cases != 96) throw new AssertionError("Original matrix changed");
        return checks;
    }
}
