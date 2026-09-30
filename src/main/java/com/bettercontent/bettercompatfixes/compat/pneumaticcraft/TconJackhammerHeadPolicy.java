package com.bettercontent.bettercompatfixes.compat.pneumaticcraft;

import me.desht.pneumaticcraft.common.item.DrillBitItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.Optional;

/** Narrow bridge from TConstruct's pick head part into PneumaticCraft's Jackhammer bit slot. */
public final class TconJackhammerHeadPolicy {
    public static final ResourceLocation PICK_HEAD_ID = new ResourceLocation("tconstruct", "pick_head");
    public static final String JACKHAMMER_HEAD_TAG = "BetterContentTconPickHead";
    public static final String PART_WEAR_TAG = "BetterContentJackhammerWear";
    private static final MaterialStatsId HEAD_STATS_ID = HeadMaterialStats.ID;

    private TconJackhammerHeadPolicy() {
    }

    public static boolean isPickHeadIdentity(final ItemStack stack) {
        return !stack.isEmpty() && isPickHeadIdentity(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static boolean isPickHeadIdentity(final ResourceLocation id) {
        return PICK_HEAD_ID.equals(id);
    }

    public static boolean isSupportedHead(final ItemStack stack) {
        return stack.getCount() == 1
                && isPickHeadIdentity(stack)
                && stack.getItem() instanceof ToolPartItem part
                && HEAD_STATS_ID.equals(part.getStatType())
                && profile(stack).isPresent();
    }

    public static boolean supportsPersistedIdentity(final String itemId, final String statId,
                                                    final String materialId) {
        return PICK_HEAD_ID.toString().equals(itemId)
                && HEAD_STATS_ID.toString().equals(statId)
                && materialId != null && !materialId.isBlank();
    }

    public static Optional<HeadMaterialStats> profile(final ItemStack stack) {
        if (!isPickHeadIdentity(stack) || !(stack.getItem() instanceof ToolPartItem part)
                || !HEAD_STATS_ID.equals(part.getStatType())) {
            return Optional.empty();
        }
        final MaterialVariantId material = part.getMaterial(stack);
        if (material == null || material.getId() == null
                || !supportsPersistedIdentity(
                ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(),
                part.getStatType().toString(), material.getId().toString())) return Optional.empty();
        return MaterialRegistry.getInstance().getMaterialStats(material.getId(), HEAD_STATS_ID);
    }

    public static ItemStack readInstalledHead(final ItemStack jackhammer) {
        if (jackhammer.isEmpty() || !jackhammer.hasTag()) return ItemStack.EMPTY;
        final CompoundTag tag = jackhammer.getTag();
        if (tag == null || !tag.contains(JACKHAMMER_HEAD_TAG, CompoundTag.TAG_COMPOUND)) return ItemStack.EMPTY;
        final ItemStack head = ItemStack.of(tag.getCompound(JACKHAMMER_HEAD_TAG));
        return isSupportedHead(head) ? head : ItemStack.EMPTY;
    }

    public static void writeInstalledHead(final ItemStack jackhammer, final ItemStack head) {
        final CompoundTag root = jackhammer.getOrCreateTag();
        if (!isSupportedHead(head)) {
            root.remove(JACKHAMMER_HEAD_TAG);
            return;
        }
        root.put(JACKHAMMER_HEAD_TAG, head.save(new CompoundTag()));
        root.putString("DrillBit", drillBitType(head).name());
    }

    public static DrillBitItem.DrillBitType drillBitType(final ItemStack head) {
        final int level = profile(head).map(stats -> stats.tier().getLevel()).orElse(-1);
        return drillBitTypeForLevel(level);
    }

    public static DrillBitItem.DrillBitType drillBitTypeForLevel(final int level) {
        if (level < 0) return DrillBitItem.DrillBitType.NONE;
        if (level >= 4) return DrillBitItem.DrillBitType.NETHERITE;
        if (level == 3) return DrillBitItem.DrillBitType.DIAMOND;
        if (level == 2) return DrillBitItem.DrillBitType.COMPRESSED_IRON;
        return DrillBitItem.DrillBitType.IRON;
    }

    public static int maxWear(final ItemStack head) {
        return profile(head).map(HeadMaterialStats::durability).orElse(0);
    }

    public static int wear(final ItemStack head) {
        return readWear(head.getOrCreateTag());
    }

    public static boolean wornOut(final ItemStack head) {
        final int max = maxWear(head);
        return max <= 0 || wear(head) >= max;
    }

    /** One unit of wear per block actually broken; the counter travels with this physical part. */
    public static int applySuccessfulBreakWear(final ItemStack head) {
        final int max = maxWear(head);
        if (max <= 0) return 0;
        return advanceWear(head.getOrCreateTag(), max);
    }

    public static int nextWear(final int currentWear, final int maxWear) {
        if (maxWear <= 0) return 0;
        if (currentWear >= maxWear) return maxWear;
        return Math.max(0, currentWear) + 1;
    }

    public static int readWear(final CompoundTag partTag) {
        return Math.max(0, partTag.getInt(PART_WEAR_TAG));
    }

    public static int advanceWear(final CompoundTag partTag, final int maxWear) {
        final int updated = nextWear(readWear(partTag), maxWear);
        partTag.putInt(PART_WEAR_TAG, updated);
        return updated;
    }

    /** Keeps material speed within half to one-and-a-half times the native bit speed. */
    public static float miningSpeedScale(final float materialMiningSpeed) {
        if (!Float.isFinite(materialMiningSpeed) || materialMiningSpeed <= 0f) return 1f;
        return Math.max(0.5f, Math.min(1.5f, materialMiningSpeed / 6f));
    }

    /** Faster/harder heads preserve PneumaticCraft's per-block air preflight and charge. */
    public static float airCostScale(final float materialMiningSpeed) {
        return miningSpeedScale(materialMiningSpeed);
    }

    public static int scaledAirReservation(final int baseAir, final float materialMiningSpeed) {
        if (baseAir <= 0) return baseAir;
        return (int) Math.ceil(baseAir * (double) airCostScale(materialMiningSpeed));
    }
}
