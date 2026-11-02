package com.bettercontent.bettercompatfixes.compat.emi;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.List;

/** Moonlight counts are NOT Forge millibuckets. Never present a bottle as a consumed ingredient. */
final class CauldronFluidStack extends EmiStack {
    private record FluidKey(ResourceLocation id) {}
    private final SoftFluidStack fluid;
    private final EmiStack icon;
    CauldronFluidStack(SoftFluidStack fluid) {
        this.fluid = fluid.copy();
        this.amount = fluid.getCount();
        this.comparison = Comparison.compareNbt();
        this.icon = fluid.toAllPossibleFilledItems().values().stream().findFirst()
                .map(EmiStack::of).orElseGet(() -> EmiStack.of(Items.CAULDRON));
    }
    @Override public EmiStack copy() {
        return new CauldronFluidStack(fluid).setAmount(amount).setChance(chance)
                .comparison(comparison).setRemainder(getRemainder());
    }
    @Override public boolean isEmpty() { return fluid.isEmpty() || amount <= 0; }
    @Override public CompoundTag getNbt() { return fluid.getTag(); }
    @Override public Object getKey() { return new FluidKey(getId()); }
    @Override public ResourceLocation getId() { return fluid.fluidKey().location(); }
    @Override public Component getName() { return fluid.fluid().getTranslatedName(); }
    @Override public List<Component> getTooltipText() {
        return List.of(getName(), Component.translatable("better_compat_fixes.emi.cauldron.fluid", amount),
                Component.translatable("better_compat_fixes.emi.cauldron.icon"));
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
        icon.copy().setAmount(amount).render(graphics, x, y, delta, flags);
    }
    static final class Serializer implements EmiIngredientSerializer<CauldronFluidStack> {
        @Override public String getType() { return "better_compat_fixes:cauldron_fluid"; }
        @Override public JsonElement serialize(CauldronFluidStack stack) {
            var copy = stack.fluid.copyWithCount(Math.toIntExact(stack.amount));
            var json = new com.google.gson.JsonObject();
            json.addProperty("type", getType());
            json.add("fluid", new JsonPrimitive(copy.save(new CompoundTag()).toString()));
            return json;
        }
        @Override public EmiIngredient deserialize(JsonElement json) {
            try {
                var fluid = SoftFluidStack.load(TagParser.parseTag(json.getAsJsonObject().get("fluid").getAsString()));
                return fluid.isEmpty() ? EmiStack.EMPTY : new CauldronFluidStack(fluid);
            } catch (Exception invalid) { return EmiStack.EMPTY; }
        }
    }
}
