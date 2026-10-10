package net.enchantedwood.item.custom;

import net.enchantedwood.effect.ModStatusEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
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

    public HyposprayCartridgeItem(Properties settings, Type type) {
        super(settings);
        this.type = type;
    }

    public Type getCartridgeType() {
        return this.type;
    }

    public static boolean isPure(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) return false;
        return stack.get(DataComponents.CUSTOM_DATA).copyTag().getBooleanOr(NBT_PURE_KEY, false);
    }

    public static void setPure(ItemStack stack, boolean pure) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (pure) {
            nbt.putBoolean(NBT_PURE_KEY, true);
        } else {
            nbt.remove(NBT_PURE_KEY);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isPure(stack)) {
            return Component.literal("§d✦ Pure §r").append(super.getName(stack));
        }
        return super.getName(stack);
    }

    @Override
    public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level world, net.minecraft.world.entity.player.Player user, net.minecraft.world.InteractionHand hand) {
        ItemStack cartridgeStack = user.getItemInHand(hand);

        // Look for an empty Hypospray: check offhand first, then player inventory
        ItemStack offhand = user.getOffhandItem();
        ItemStack hyposprayStack = ItemStack.EMPTY;
        if (!offhand.isEmpty() && offhand.getItem() instanceof HyposprayItem && !HyposprayItem.isLoaded(offhand)) {
            hyposprayStack = offhand;
        } else {
            for (int i = 0; i < user.getInventory().getContainerSize(); i++) {
                ItemStack candidate = user.getInventory().getItem(i);
                if (!candidate.isEmpty() && candidate.getItem() instanceof HyposprayItem && !HyposprayItem.isLoaded(candidate)) {
                    hyposprayStack = candidate;
                    break;
                }
            }
        }

        if (!hyposprayStack.isEmpty()) {
            boolean pure = isPure(cartridgeStack);
            if (!world.isClientSide()) {
                HyposprayItem.setLoadedCartridge(hyposprayStack, this.type, pure);
                cartridgeStack.shrink(1);
                world.playSound(null, user.getX(), user.getY(), user.getZ(), net.minecraft.sounds.SoundEvents.CROSSBOW_LOADING_END.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.7f);
                user.sendOverlayMessage(Component.literal("§e✦ Loaded into Hypospray: " + (pure ? "§d✦ Pure " : "") + this.type.title + " ✦"));
            }
            user.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        if (!world.isClientSide()) {
            user.sendOverlayMessage(Component.literal("§e[Ampoule] No empty Hypospray in inventory to load into."));
        }
        return net.minecraft.world.InteractionResult.FAIL;
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
                target.addEffect(new MobEffectInstance(ModStatusEffects.ACID_PROTECTION, duration, 0));
                if (isPure) {
                    target.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 0));
                }
            }
            case HEAT_BUFFER -> {
                int duration = (int) (20 * 60 * (isPure ? 12 : 6) * factor);
                target.addEffect(new MobEffectInstance(ModStatusEffects.THERMAL_PROTECTION, duration, 0));
                if (isPure) {
                    target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0));
                }
            }
            case HYPER_OXYGENATION -> {
                int duration = (int) (20 * 60 * (isPure ? 12 : 6) * factor);
                target.addEffect(new MobEffectInstance(ModStatusEffects.ATMOSPHERIC_PROTECTION, duration, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0));
                if (isPure) {
                    target.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, duration, 0));
                }
            }
            case NANITE_TRAUMA -> {
                target.heal(isPure ? (diminished ? 10.0f : 20.0f) : (diminished ? 4.0f : 8.0f));
                int duration = (int) (20 * (isPure ? 40 : 20) * factor);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, isPure ? 2 : 1));
                if (isPure) {
                    target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 120, 1));
                }
                // Cleanse harmful negative effects
                target.removeEffect(MobEffects.POISON);
                target.removeEffect(MobEffects.WITHER);
                target.removeEffect(MobEffects.SLOWNESS);
                target.removeEffect(MobEffects.WEAKNESS);
                target.removeEffect(MobEffects.NAUSEA);
            }
            case ADRENALINE_STIM -> {
                int duration = (int) (20 * 60 * (isPure ? 6 : 3) * factor);
                target.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, isPure ? 2 : 1));
                target.addEffect(new MobEffectInstance(MobEffects.HASTE, duration, isPure ? 2 : 1));
                target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, isPure ? 1 : 0));
                if (isPure) {
                    target.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 0));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§9✦ Hypospray Medical Ampoule ✦"));
        textConsumer.accept(Component.literal(this.type.title));
        textConsumer.accept(Component.literal(this.type.description));
        if (isPure(stack)) {
            textConsumer.accept(Component.literal("§d✦ GRADE-A PURE CLEANROOM SYNTHESIS ✦"));
            textConsumer.accept(Component.literal("§a • 2x Duration (12 Minutes)"));
            textConsumer.accept(Component.literal("§a • Amplified Potency & Cleansing Buffs"));
        }
        textConsumer.accept(Component.literal("§e • Right-Click: §7Snap-load directly into an empty Hypospray"));
        textConsumer.accept(Component.literal("§b • Offhand: §7Hold in offhand & Right-Click with Hypospray to load"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
