package com.bettercontent.bettercompatfixes.client.tooltip;

import com.bettercontent.bettercompatfixes.compat.tconstruct.ToolCapabilities;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.ToolActions;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.impl.ComposableModifier;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.capability.ToolEnergyCapability;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import static com.bettercontent.bettercompatfixes.client.tooltip.ToolTooltipRenderer.*;

/** The input is already detached by the owner. No player-aware tooltip/stat hooks are invoked. */
public final class ToolTooltipSummaryProvider {
    private ToolTooltipSummaryProvider() {}

    public static boolean isAssembled(ItemStack stack) {
        return stack.getItem() instanceof IModifiable;
    }

    public static ToolTooltipSnapshot snapshot(ItemStack detached) {
        IModifiable item = (IModifiable)detached.getItem();
        var name = detached.getHoverName().copy();
        if (name.getStyle().getColor() == null) name.withStyle(detached.getRarity().getStyleModifier());
        if (detached.hasCustomHoverName()) name.withStyle(name.getStyle().applyTo(Style.EMPTY.withItalic(true)));
        var lore = lore(detached);
        if (item.getToolDefinition() == null || !item.getToolDefinition().isDataLoaded()) {
            return unavailable(name, lore, "definition_pending", "Tool definition not available yet");
        }
        if (!ToolStack.isInitialized(detached)) {
            return unavailable(name, lore, "uninitialized", "Tool not initialized; assembled statistics unavailable");
        }
        ToolStack tool = ToolStack.copyFrom(detached);
        if (tool.getStats().getContainedStats().isEmpty()) {
            return unavailable(name, lore, "invalid_stats", "Assembled statistics unavailable");
        }
        var capabilities = new HashSet<String>();
        if (tool.getModifierLevel(new ModifierId("tconstruct", "silky")) > 0) capabilities.add("silky");
        if (ToolCapabilities.hasMiningDefinition(item.getToolDefinition())) capabilities.add("mining");
        if (detached.is(TinkerTags.Items.MELEE)) capabilities.add("melee");
        if (detached.is(TinkerTags.Items.MELEE_WEAPON)) capabilities.add("melee_weapon");
        if (detached.is(TinkerTags.Items.ARMOR)) capabilities.add("defense");
        if (detached.is(TinkerTags.Items.LAUNCHERS)) capabilities.add("launcher");
        if (detached.is(TinkerTags.Items.AMMO)) capabilities.add("ammo");
        if (detached.is(TinkerTags.Items.RANGED) || capabilities.contains("ammo")) capabilities.add("projectile");
        if (capabilities.contains("melee") || capabilities.contains("projectile")) capabilities.add("attack");
        if (detached.getItem() instanceof ArmorItem) capabilities.add("armor");
        else capabilities.add("held");
        // Query actions on an unbroken detached view: condition must not change capability classification.
        ItemStack baseline = detached.copy();
        baseline.getOrCreateTag().putBoolean(ToolStack.TAG_BROKEN, false);
        if (baseline.canPerformAction(ToolActions.SHIELD_BLOCK)) capabilities.add("blocking");
        if (capabilities.stream().anyMatch(c -> Set.of("melee", "launcher", "ammo", "armor", "blocking").contains(c)))
            capabilities.add("combat");
        var state = new ArrayList<Component>();
        var stats = new ArrayList<Component>();
        var actions = new ArrayList<Component>();
        var effects = new ArrayList<ToolTooltipEffect>();
        if (tool.isBroken()) state.add(Component.translatableWithFallback("tooltip.better_compat_fixes.broken", "Broken — repair before use")
                .withStyle(ChatFormatting.RED));
        else if (tool.isUnbreakable() || tool.getModifierLevel(new ModifierId("tconstruct", "unbreakable")) > 0)
            state.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.GRAY));
        else if (detached.is(TinkerTags.Items.DURABILITY)) state.add(fact("durability", "Durability", amount(tool.getCurrentDurability())
                + "/" + amount(tool.getStats().getInt(ToolStats.DURABILITY))));
        collectState(detached, tool, capabilities, state);
        EquipmentSlot slot = detached.getItem() instanceof ArmorItem armor ? armor.getEquipmentSlot() : EquipmentSlot.MAINHAND;
        var attributes = baseline.getAttributeModifiers(slot);
        if (capabilities.contains("melee_weapon")) {
            stats.add(row(fact("attack_damage", "Damage", number(attribute(attributes, Attributes.ATTACK_DAMAGE, 1))),
                    fact("attack_speed", "Attack speed", number(attribute(attributes, Attributes.ATTACK_SPEED, 4)))));
        }
        if (detached.is(TinkerTags.Items.UNARMED) && tool.getStats().get(ToolStats.ATTACK_DAMAGE) != 0) {
            stats.add(fact("unarmed_damage", "Unarmed damage", number(1 + tool.getStats().get(ToolStats.ATTACK_DAMAGE))));
        }
        if (capabilities.contains("mining")) {
            stats.add(row(fact("mining_speed", "Mining speed", number(tool.getStats().get(ToolStats.MINING_SPEED))),
                    ToolStats.HARVEST_TIER.formatValue(tool.getStats().get(ToolStats.HARVEST_TIER))));
        }
        if (capabilities.contains("launcher") || capabilities.contains("ammo")) {
            var facts = new ArrayList<Component>();
            facts.add(fact("projectile_power", "Projectile power", number(tool.getStats().get(ToolStats.PROJECTILE_DAMAGE))));
            if (capabilities.contains("launcher")) facts.add(fact("draw_speed", "Draw speed", number(tool.getStats().get(ToolStats.DRAW_SPEED))));
            stats.add(row(facts.toArray(Component[]::new)));
            stats.add(row(fact("velocity", "Velocity", number(tool.getStats().get(ToolStats.VELOCITY))),
                    fact("accuracy", "Accuracy", number(tool.getStats().get(ToolStats.ACCURACY) * 100) + "%")));
        }
        if (capabilities.contains("blocking")) stats.add(row(
                fact("block_amount", "Block", number(tool.getStats().get(ToolStats.BLOCK_AMOUNT))),
                fact("block_angle", "Angle", number(tool.getStats().get(ToolStats.BLOCK_ANGLE)) + "°")));
        if (capabilities.contains("armor")) {
            var facts = new ArrayList<Component>();
            facts.add(fact("armor", "Armor", number(attribute(attributes, Attributes.ARMOR, 0))));
            double toughness = attribute(attributes, Attributes.ARMOR_TOUGHNESS, 0);
            if (toughness != 0) facts.add(fact("toughness", "Toughness", number(toughness)));
            double resistance = attribute(attributes, Attributes.KNOCKBACK_RESISTANCE, 0);
            if (resistance != 0) facts.add(fact("knockback_resistance", "Knockback resistance", number(resistance * 100) + "%"));
            stats.add(row(facts.toArray(Component[]::new)));
        }
        // Other real item attributes (movement, reach, experience, etc.) are not lost when their
        // modifier's numerical fragment is folded. Do not assume every attribute uses a vanilla base.
        var additional = new ArrayList<Component>();
        Set<Attribute> shown = new HashSet<>();
        if (capabilities.contains("melee_weapon")) shown.addAll(Set.of(Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED));
        if (capabilities.contains("armor")) shown.addAll(Set.of(Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE));
        attributes.keySet().stream().filter(key -> !shown.contains(key))
                .filter(key -> capabilities.contains("mining") || !new ResourceLocation("tconstruct", "player.mining_speed_multiplier")
                        .equals(BuiltInRegistries.ATTRIBUTE.getKey(key)))
                .sorted(java.util.Comparator.comparing(Attribute::getDescriptionId)).forEach(key -> {
                    boolean percent = attributes.get(key).stream().noneMatch(m -> m.getOperation() == AttributeModifier.Operation.ADDITION);
                    double fixedBase = key == Attributes.MOVEMENT_SPEED ? 0.1 : key.getDefaultValue();
                    double value = percent ? (IntrinsicAttributeMath.apply(1, attributes.get(key).stream()
                            .map(m -> new IntrinsicAttributeMath.Contribution(m.getId(), m.getAmount(), m.getOperation().toValue())).toList()) - 1) * 100
                            : attribute(attributes, key, fixedBase);
                    if ((percent && Math.abs(value) < 0.0005) || (!percent && value == fixedBase)) return;
                    additional.add(Component.translatableWithFallback("tooltip.better_compat_fixes.attribute", "%s %s",
                            Component.translatable(key.getDescriptionId()), (percent && value >= 0 ? "+" : "")
                            + number(value) + (percent ? "%" : "")));
                });
        if (!additional.isEmpty()) stats.add(row(additional.toArray(Component[]::new)));
        if (ToolTooltipOwner.supported("epicfight", "20.14.17")) {
            try {
                EpicFightTooltipSummary.contribute(baseline, stats, actions, effects);
            } catch (RuntimeException | LinkageError failure) {
                ToolTooltipCatalogue.warnOnce("Epic Fight profile " + detached.getItem().getDescriptionId(), failure);
                state.add(Component.translatableWithFallback("tooltip.better_compat_fixes.combat_profile_unavailable", "Combat profile unavailable"));
            }
        }
        for (ModifierEntry modifier : tool.getModifierList()) collectModifier(modifier, capabilities, effects);
        String id = BuiltInRegistries.ITEM.getKey(detached.getItem()).toString();
        for (var action : ToolTooltipCatalogue.current().actions().getOrDefault(id, List.of())) {
            if (capabilities.containsAll(action.requires())) actions.add(fragmentText(action, 1));
        }
        ExternalToolTooltipSummary.contribute(detached, capabilities, effects, state);
        return new ToolTooltipSnapshot(name, capabilities, state, stats, actions, effects, lore);
    }

    private static ToolTooltipSnapshot unavailable(Component name, List<Component> lore, String key, String text) {
        return new ToolTooltipSnapshot(name, Set.of(), List.of(Component.translatableWithFallback("tooltip.better_compat_fixes." + key, text)),
                List.of(), List.of(), List.of(), lore);
    }

    public static double attribute(Multimap<Attribute, AttributeModifier> attributes, Attribute key, double baseline) {
        var contributions = attributes.get(key).stream().map(m -> new IntrinsicAttributeMath.Contribution(
                m.getId(), m.getAmount(), m.getOperation().toValue())).toList();
        return key.sanitizeValue(IntrinsicAttributeMath.apply(baseline, contributions));
    }

    private static void collectModifier(ModifierEntry modifier, Set<String> capabilities, List<ToolTooltipEffect> effects) {
        String id = modifier.getId().toString();
        var catalogue = ToolTooltipCatalogue.current();
        var fragments = catalogue.modifiers().get(id);
        // Re-read the actual synced module shape. A datapack adding a behavioral module to a
        // previously stat-only ID must not silently inherit its old omission classification.
        if (fragments != null && modifier.getModifier() instanceof ComposableModifier composed) {
            var json = ComposableModifier.LOADER.serialize(composed).getAsJsonObject();
            var actualTypes = new ArrayList<String>();
            if (json.has("modules")) json.getAsJsonArray("modules").forEach(module ->
                    actualTypes.add(module.getAsJsonObject().get("type").getAsString()));
            actualTypes.sort(String::compareTo);
            if (!actualTypes.equals(catalogue.moduleTypes().get(id))) {
                ToolTooltipCatalogue.warnOnce("changed module shape " + id, null);
                fragments = null;
            }
        }
        if (fragments == null) {
            ToolTooltipCatalogue.warnOnce("modifier " + id, null);
            Component text = modifierName(modifier);
            // Unclassified prose is not safe for a mining-disabled item: it may mix mining and combat.
            // Keep the identity/level; never silently discard the unknown effect itself.
            String key = modifier.getModifier().getTranslationKey() + ".description";
            if (capabilities.contains("mining") && Language.getInstance().has(key)) {
                text = text.copy().append(": ").append(Component.translatable(key));
            }
            effects.add(new ToolTooltipEffect(id + "@" + modifier.getEffectiveLevel(), Set.of(),
                    ToolTooltipEffect.Category.BEHAVIOR, text, false));
            return;
        }
        for (var fragment : fragments) {
            Component text = fragmentText(fragment, modifier.getEffectiveLevel());
            if (fragment.named()) text = modifierName(modifier).copy().append(": ").append(text);
            effects.add(new ToolTooltipEffect(fragment.key() + "@" + modifier.getEffectiveLevel(), fragment.requires(),
                    ToolTooltipEffect.Category.valueOf(fragment.kind().toUpperCase(java.util.Locale.ROOT)), text, false));
        }
    }

    private static Component modifierName(ModifierEntry modifier) {
        String key = modifier.getModifier().getTranslationKey();
        if (Language.getInstance().has(key)) return modifier.getDisplayName();
        // Keep unknown effects identifiable without dumping a technical translation key into hover.
        String path = modifier.getId().getPath().replace('_', ' ').replace('/', ' ');
        String label = path.isEmpty() ? "Unknown effect" : Character.toUpperCase(path.charAt(0)) + path.substring(1);
        return Component.translatableWithFallback(key, label).append(" " + number(modifier.getEffectiveLevel()));
    }

    static Component fragmentText(ToolTooltipCatalogue.Fragment fragment, double level) {
        var args = fragment.per_level().stream().map(scale -> number(scale * level)).toArray();
        return Component.translatableWithFallback(fragment.key(), fragment.text(), args);
    }

    private static void collectState(ItemStack stack, ToolStack tool, Set<String> capabilities, List<Component> state) {
        var data = tool.getPersistentData();
        var charged = data.getCompound(new ResourceLocation("tconstruct", "crossbow_ammo"));
        if (!charged.isEmpty()) {
            ItemStack ammo = ItemStack.of(charged);
            if (!ammo.isEmpty()) state.add(fact("loaded", "Loaded", ammo.getHoverName().copy()
                    .append(ammo.getCount() > 1 ? " ×" + ammo.getCount() : "")));
        }
        var fluid = ToolTankHelper.TANK_HELPER.getFluid(tool);
        int capacity = ToolTankHelper.TANK_HELPER.getCapacity(tool);
        if (capacity > 0) state.add(fact("fluid", "Fluid", fluid.isEmpty() ? "0/" + amount(capacity) + " mB"
                : Component.empty().append(fluid.getDisplayName()).append(" " + amount(fluid.getAmount()) + "/" + amount(capacity) + " mB")));
        int energy = ToolEnergyCapability.getMaxEnergy(tool);
        if (energy > 0) state.add(fact("energy", "Energy", amount(ToolEnergyCapability.getEnergy(tool)) + "/" + amount(energy) + " FE"));
        if (energy == 0) stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
            if (storage.getMaxEnergyStored() > 0) state.add(fact("energy", "Energy", amount(storage.getEnergyStored())
                    + "/" + amount(storage.getMaxEnergyStored()) + " FE"));
        });
        if (capacity == 0) stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(storage -> {
            for (int tank = 0; tank < storage.getTanks(); tank++) {
                var content = storage.getFluidInTank(tank);
                if (storage.getTankCapacity(tank) <= 0) continue;
                state.add(fact("fluid", "Fluid", (content.isEmpty() ? Component.empty() : content.getDisplayName().copy().append(" "))
                        .append(amount(content.getAmount()) + "/" + amount(storage.getTankCapacity(tank)) + " mB")));
            }
        });
        for (var entry : tool.getModifierList()) {
            var bar = entry.getHook(ModifierHooks.CAPACITY_BAR);
            int maximum = bar.getCapacity(tool, entry);
            if (maximum > 0) state.add(row(modifierName(entry), Component.literal(amount(bar.getAmount(tool)) + "/" + amount(maximum))));
        }
        for (String key : List.of("etstlib:static_electricity", "etstlib:air")) {
            var location = new ResourceLocation(key);
            if (data.contains(location, Tag.TAG_ANY_NUMERIC)) state.add(fact(key.replace(':', '.'),
                    key.equals("etstlib:air") ? "Stored air" : "Static charge", amount(data.getInt(location))));
        }
        // Potion payloads are item-local; do not recursively call the ammunition tooltip.
        if (capabilities.contains("ammo")) {
            var key = new ModifierId("tconstruct", "tipped");
            ResourceLocation potion = ResourceLocation.tryParse(data.getString(key));
            int level = tool.getModifierLevel(key);
            if (potion != null && level > 0) BuiltInRegistries.POTION.getOptional(potion).ifPresent(payload -> {
                int divisor = 1 << Math.max(4 - level, 0);
                for (var effect : payload.getEffects()) {
                    Component time = effect.getEffect().isInstantenous() ? Component.empty()
                            : Component.literal(" · " + number(effect.getDuration() / (20.0 * divisor)) + " s");
                    state.add(row(effect.getEffect().getDisplayName().copy().append(" ")
                            .append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1))).append(time)));
                }
            });
        }
    }

    private static List<Component> lore(ItemStack stack) {
        var result = new ArrayList<Component>();
        var tag = stack.getTagElement("display");
        if (tag != null && tag.contains("Lore", Tag.TAG_LIST)) {
            var list = tag.getList("Lore", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                try {
                    var line = Component.Serializer.fromJson(list.getString(i));
                    if (line != null) result.add(line.withStyle(line.getStyle().applyTo(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true))));
                } catch (RuntimeException e) { ToolTooltipCatalogue.warnOnce("invalid custom lore", e); }
            }
        }
        return result;
    }
}
