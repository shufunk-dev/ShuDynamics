package net.enchantedwood.item.custom;

import net.enchantedwood.effect.ModStatusEffects;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class HyposprayCartridgeItem extends Item {

    public enum Type {
        ACID_NEUTRALIZING("§aAcid-Neutralizing", "§7Neutralizes ambient caustic fumes & acid pools.", 0x52B788),
        HEAT_BUFFER("§6Endothermic Heat-Buffer", "§7Thermal insulation against extreme volcanic heat & lava.", 0xFF6B35),
        HYPER_OXYGENATION("§bHyper-Oxygenation", "§7Oxygenates bloodstream for vacuum & underwater respiration.", 0x00B4D8),
        NANITE_TRAUMA("§dNanite Trauma Inoculant", "§7Emergency critical care: +8 HP, Regen II & cleanses debuffs.", 0xE0AAFF),
        ADRENALINE_STIM("§eAdrenaline Combat Stim", "§7High-performance combat booster: Speed II, Haste II & Resistance I.", 0xFFD166);

        public final String title;
        public final String description;
        public final int color;

        Type(String title, String description, int color) {
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }

    private final Type type;

    public HyposprayCartridgeItem(Settings settings, Type type) {
        super(settings);
        this.type = type;
    }

    public Type getCartridgeType() {
        return this.type;
    }

    /**
     * Applies this cartridge's effects to the target living entity.
     * @param target The entity being injected.
     * @param diminished If true (due to overdose), duration/potency is reduced.
     */
    public void applyEffects(LivingEntity target, boolean diminished) {
        float factor = diminished ? 0.5f : 1.0f;

        switch (this.type) {
            case ACID_NEUTRALIZING -> {
                int duration = (int) (20 * 60 * 6 * factor); // 6 mins base
                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.ACID_PROTECTION, duration, 0));
            }
            case HEAT_BUFFER -> {
                int duration = (int) (20 * 60 * 6 * factor); // 6 mins base
                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.THERMAL_PROTECTION, duration, 0));
            }
            case HYPER_OXYGENATION -> {
                int duration = (int) (20 * 60 * 6 * factor); // 6 mins base
                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.ATMOSPHERIC_PROTECTION, duration, 0));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration, 0));
            }
            case NANITE_TRAUMA -> {
                target.heal(diminished ? 4.0f : 8.0f); // 4 or 8 HP
                int duration = (int) (20 * 20 * factor); // 20s base
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 1));
                // Cleanse harmful negative effects
                target.removeStatusEffect(StatusEffects.POISON);
                target.removeStatusEffect(StatusEffects.WITHER);
                target.removeStatusEffect(StatusEffects.SLOWNESS);
                target.removeStatusEffect(StatusEffects.WEAKNESS);
                target.removeStatusEffect(StatusEffects.NAUSEA);
            }
            case ADRENALINE_STIM -> {
                int duration = (int) (20 * 60 * 3 * factor); // 3 mins base
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0));
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§9✦ Hypospray Medical Ampoule ✦"));
        textConsumer.accept(Text.literal(this.type.title));
        textConsumer.accept(Text.literal(this.type.description));
        textConsumer.accept(Text.literal("§8 • Load into Hypospray (or place in offhand) to inject."));
        textConsumer.accept(Text.literal("§c • Pacing: §7Wait 5s between injections to prevent overdose sickness."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
