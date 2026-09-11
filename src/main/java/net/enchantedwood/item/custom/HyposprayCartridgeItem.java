package net.enchantedwood.item.custom;

import net.enchantedwood.effect.ModStatusEffects;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class HyposprayCartridgeItem extends Item {

    public static final String NBT_PURE_KEY = "Pure";

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

    public static boolean isPure(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.contains(DataComponentTypes.CUSTOM_DATA)) return false;
        return stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt().getBoolean(NBT_PURE_KEY, false);
    }

    public static void setPure(ItemStack stack, boolean pure) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (pure) {
            nbt.putBoolean(NBT_PURE_KEY, true);
        } else {
            nbt.remove(NBT_PURE_KEY);
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public Text getName(ItemStack stack) {
        if (isPure(stack)) {
            return Text.literal("§d✦ Pure §r").append(super.getName(stack));
        }
        return super.getName(stack);
    }

    @Override
    public net.minecraft.util.ActionResult use(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity user, net.minecraft.util.Hand hand) {
        ItemStack cartridgeStack = user.getStackInHand(hand);

        // Look for an empty Hypospray: check offhand first, then player inventory
        ItemStack offhand = user.getOffHandStack();
        ItemStack hyposprayStack = ItemStack.EMPTY;
        if (!offhand.isEmpty() && offhand.getItem() instanceof HyposprayItem && !HyposprayItem.isLoaded(offhand)) {
            hyposprayStack = offhand;
        } else {
            for (int i = 0; i < user.getInventory().size(); i++) {
                ItemStack candidate = user.getInventory().getStack(i);
                if (!candidate.isEmpty() && candidate.getItem() instanceof HyposprayItem && !HyposprayItem.isLoaded(candidate)) {
                    hyposprayStack = candidate;
                    break;
                }
            }
        }

        if (!hyposprayStack.isEmpty()) {
            boolean pure = isPure(cartridgeStack);
            if (!world.isClient()) {
                HyposprayItem.setLoadedCartridge(hyposprayStack, this.type, pure);
                cartridgeStack.decrement(1);
                world.playSound(null, user.getX(), user.getY(), user.getZ(), net.minecraft.sound.SoundEvents.ITEM_CROSSBOW_LOADING_END.value(), net.minecraft.sound.SoundCategory.PLAYERS, 0.9f, 1.7f);
                user.sendMessage(Text.literal("§e✦ Loaded into Hypospray: " + (pure ? "§d✦ Pure " : "") + this.type.title + " ✦"), true);
            }
            user.swingHand(hand);
            return net.minecraft.util.ActionResult.SUCCESS;
        }

        if (!world.isClient()) {
            user.sendMessage(Text.literal("§e[Ampoule] No empty Hypospray in inventory to load into."), true);
        }
        return net.minecraft.util.ActionResult.FAIL;
    }

    /**
     * Applies this cartridge's effects to the target living entity.
     * @param target The entity being injected.
     * @param diminished If true (due to overdose), duration/potency is reduced.
     */
    public void applyEffects(LivingEntity target, boolean diminished) {
        applyEffects(target, diminished, false);
    }

    public void applyEffects(LivingEntity target, boolean diminished, boolean isPure) {
        float factor = diminished ? 0.5f : 1.0f;

        switch (this.type) {
            case ACID_NEUTRALIZING -> {
                int duration = (int) (20 * 60 * (isPure ? 12 : 6) * factor);
                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.ACID_PROTECTION, duration, 0));
                if (isPure) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 100, 0));
                }
            }
            case HEAT_BUFFER -> {
                int duration = (int) (20 * 60 * (isPure ? 12 : 6) * factor);
                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.THERMAL_PROTECTION, duration, 0));
                if (isPure) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0));
                }
            }
            case HYPER_OXYGENATION -> {
                int duration = (int) (20 * 60 * (isPure ? 12 : 6) * factor);
                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.ATMOSPHERIC_PROTECTION, duration, 0));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, duration, 0));
                if (isPure) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, duration, 0));
                }
            }
            case NANITE_TRAUMA -> {
                target.heal(isPure ? (diminished ? 10.0f : 20.0f) : (diminished ? 4.0f : 8.0f));
                int duration = (int) (20 * (isPure ? 40 : 20) * factor);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, isPure ? 2 : 1));
                if (isPure) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 120, 1));
                }
                // Cleanse harmful negative effects
                target.removeStatusEffect(StatusEffects.POISON);
                target.removeStatusEffect(StatusEffects.WITHER);
                target.removeStatusEffect(StatusEffects.SLOWNESS);
                target.removeStatusEffect(StatusEffects.WEAKNESS);
                target.removeStatusEffect(StatusEffects.NAUSEA);
            }
            case ADRENALINE_STIM -> {
                int duration = (int) (20 * 60 * (isPure ? 6 : 3) * factor);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, isPure ? 2 : 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, isPure ? 2 : 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, isPure ? 1 : 0));
                if (isPure) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 0));
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§9✦ Hypospray Medical Ampoule ✦"));
        textConsumer.accept(Text.literal(this.type.title));
        textConsumer.accept(Text.literal(this.type.description));
        if (isPure(stack)) {
            textConsumer.accept(Text.literal("§d✦ GRADE-A PURE CLEANROOM SYNTHESIS ✦"));
            textConsumer.accept(Text.literal("§a • 2x Duration (12 Minutes)"));
            textConsumer.accept(Text.literal("§a • Amplified Potency & Cleansing Buffs"));
        }
        textConsumer.accept(Text.literal("§e • Right-Click: §7Snap-load directly into an empty Hypospray"));
        textConsumer.accept(Text.literal("§b • Offhand: §7Hold in offhand & Right-Click with Hypospray to load"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
