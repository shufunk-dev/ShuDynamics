package net.enchantedwood.item.custom;

import net.enchantedwood.effect.ModStatusEffects;
import net.enchantedwood.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

public class HyposprayItem extends Item {
    public static final String NBT_LOADED_KEY = "LoadedCartridge";
    public static final String NBT_PURE_KEY = "LoadedPure";

    public HyposprayItem(Properties settings) {
        super(settings);
    }

    public static boolean isLoaded(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) return false;
        CompoundTag nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return nbt.contains(NBT_LOADED_KEY) && !nbt.getStringOr(NBT_LOADED_KEY, "").isEmpty();
    }

    public static boolean isLoadedPure(ItemStack stack) {
        if (!isLoaded(stack)) return false;
        CompoundTag nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return nbt.getBooleanOr(NBT_PURE_KEY, false);
    }

    public static HyposprayCartridgeItem.Type getLoadedType(ItemStack stack) {
        if (!isLoaded(stack)) return null;
        CompoundTag nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        String name = nbt.getStringOr(NBT_LOADED_KEY, "");
        try {
            return HyposprayCartridgeItem.Type.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setLoadedCartridge(ItemStack stack, HyposprayCartridgeItem.Type type) {
        setLoadedCartridge(stack, type, false);
    }

    public static void setLoadedCartridge(ItemStack stack, HyposprayCartridgeItem.Type type, boolean isPure) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        nbt.putString(NBT_LOADED_KEY, type.name());
        nbt.putBoolean(NBT_PURE_KEY, isPure);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    public static void clearLoadedCartridge(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            nbt.remove(NBT_LOADED_KEY);
            nbt.remove(NBT_PURE_KEY);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    public static Item getCartridgeItem(HyposprayCartridgeItem.Type type) {
        return switch (type) {
            case ACID_NEUTRALIZING -> ModItems.ACID_NEUTRALIZING_CARTRIDGE;
            case HEAT_BUFFER -> ModItems.HEAT_BUFFER_CARTRIDGE;
            case HYPER_OXYGENATION -> ModItems.HYPER_OXYGENATION_CARTRIDGE;
            case NANITE_TRAUMA -> ModItems.NANITE_TRAUMA_CARTRIDGE;
            case ADRENALINE_STIM -> ModItems.ADRENALINE_STIM_CARTRIDGE;
        };
    }

    public static void registerEntityInteraction() {
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.HYPOSPRAY) && entity instanceof LivingEntity livingEntity) {
                HyposprayItem hypospray = (HyposprayItem) stack.getItem();

                // If empty, check if offhand has a cartridge to load
                if (!isLoaded(stack)) {
                    ItemStack offhand = player.getOffhandItem();
                    if (!offhand.isEmpty() && offhand.getItem() instanceof HyposprayCartridgeItem cartridgeItem) {
                        boolean pure = HyposprayCartridgeItem.isPure(offhand);
                        if (!world.isClientSide()) {
                            setLoadedCartridge(stack, cartridgeItem.getCartridgeType(), pure);
                            offhand.shrink(1);
                            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.9f, 1.7f);
                            player.sendOverlayMessage(Component.literal("§e✦ Loaded Hypospray with " + (pure ? "§d✦ Pure " : "") + cartridgeItem.getCartridgeType().title + " ✦"));
                        }
                        player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                        return InteractionResult.SUCCESS;
                    }
                    if (!world.isClientSide()) {
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.8f);
                        player.sendOverlayMessage(Component.literal("§e[Hypospray] Chamber empty! Load an ampoule to inoculate " + livingEntity.getName().getString() + "."));
                    }
                    return InteractionResult.FAIL;
                }

                InteractionResult result = hypospray.executeInoculation(world, player, livingEntity, stack, hand);
                if (result == InteractionResult.SUCCESS) {
                    player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack hyposprayStack = user.getItemInHand(hand);

        // 1. Unload cartridge on Sneak + Right-Click
        if (user.isShiftKeyDown() && isLoaded(hyposprayStack)) {
            if (!world.isClientSide()) {
                HyposprayCartridgeItem.Type loadedType = getLoadedType(hyposprayStack);
                boolean wasPure = isLoadedPure(hyposprayStack);
                clearLoadedCartridge(hyposprayStack);
                if (loadedType != null) {
                    ItemStack ejected = new ItemStack(getCartridgeItem(loadedType));
                    if (wasPure) {
                        HyposprayCartridgeItem.setPure(ejected, true);
                    }
                    user.addItem(ejected);
                }
                world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
                user.sendOverlayMessage(Component.literal("§7✦ Ejected cartridge from Hypospray ✦"));
            }
            user.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
            return InteractionResult.SUCCESS;
        }

        // 2. If empty, check if offhand has a cartridge to load
        if (!isLoaded(hyposprayStack)) {
            ItemStack offhand = user.getOffhandItem();
            if (!offhand.isEmpty() && offhand.getItem() instanceof HyposprayCartridgeItem cartridgeItem) {
                boolean pure = HyposprayCartridgeItem.isPure(offhand);
                if (!world.isClientSide()) {
                    setLoadedCartridge(hyposprayStack, cartridgeItem.getCartridgeType(), pure);
                    offhand.shrink(1);
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.9f, 1.7f);
                    user.sendOverlayMessage(Component.literal("§e✦ Loaded Hypospray with " + (pure ? "§d✦ Pure " : "") + cartridgeItem.getCartridgeType().title + " ✦"));
                }
                user.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
                return InteractionResult.SUCCESS;
            } else {
                if (!world.isClientSide()) {
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.8f);
                    user.sendOverlayMessage(Component.literal("§e[Hypospray] Chamber empty! Right-click an ampoule or hold in offhand to load."));
                }
                return InteractionResult.FAIL;
            }
        }

        // 3. Loaded: Inoculate Self!
        return executeInoculation(world, user, user, hyposprayStack, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        return executeInoculation(entity.level(), user, entity, stack, hand);
    }

    public InteractionResult executeInoculation(Level world, Player user, LivingEntity target, ItemStack hyposprayStack, InteractionHand hand) {
        if (!isLoaded(hyposprayStack)) {
            return InteractionResult.FAIL;
        }

        HyposprayCartridgeItem.Type loadedType = getLoadedType(hyposprayStack);
        if (loadedType == null) return InteractionResult.FAIL;
        boolean isPure = isLoadedPure(hyposprayStack);

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            boolean isOverdose = target.hasEffect(ModStatusEffects.METABOLIC_SATURATION);

            if (isOverdose) {
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * 10, 1));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 10, 1));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 10, 1));
                target.hurtServer(serverWorld, serverWorld.damageSources().magic(), 4.0f); // 2 hearts rejection damage

                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.6f, 1.6f);
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8f, 0.8f);
                serverWorld.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.4, 0.3, 0.05);

                user.sendOverlayMessage(Component.literal("§c⚠ WARNING: Metabolic Inoculant Overload! Chemical sickness induced! ⚠"));
                applyEffects(loadedType, target, true, isPure);
            } else {
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.9f, 1.8f);
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.7f);

                serverWorld.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.2, 0.3, 0.2, 0.03);
                serverWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.2, target.getZ(), 4, 0.2, 0.2, 0.2, 0.02);

                if (isPure) {
                    serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.5f);
                    serverWorld.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 18, 0.3, 0.4, 0.3, 0.06);
                }

                target.addEffect(new MobEffectInstance(ModStatusEffects.METABOLIC_SATURATION, 100, 0));
                applyEffects(loadedType, target, false, isPure);

                String targetName = (target == user) ? "Self" : target.getName().getString();
                if (isPure) {
                    user.sendOverlayMessage(Component.literal("§d✦ PURE Inoculation administered (" + targetName + "): " + loadedType.title + " §e(12m Duration / Grade-A Potency) ✦"));
                } else {
                    user.sendOverlayMessage(Component.literal("§a✔ Inoculation administered (" + targetName + "): " + loadedType.title + " §7(Metabolic rest: 5s)"));
                }
            }

            // Chamber is now empty!
            clearLoadedCartridge(hyposprayStack);
            // Refund empty cartridge casing
            user.addItem(new ItemStack(ModItems.EMPTY_CARTRIDGE));
        }

        return InteractionResult.SUCCESS;
    }

    public static void applyEffects(HyposprayCartridgeItem.Type type, LivingEntity target, boolean diminished) {
        applyEffects(type, target, diminished, false);
    }

    public static void applyEffects(HyposprayCartridgeItem.Type type, LivingEntity target, boolean diminished, boolean isPure) {
        float factor = diminished ? 0.5f : 1.0f;
        switch (type) {
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
    public Component getName(ItemStack stack) {
        if (isLoaded(stack)) {
            HyposprayCartridgeItem.Type type = getLoadedType(stack);
            if (type != null) {
                boolean isPure = isLoadedPure(stack);
                return Component.literal("Hypospray [" + (isPure ? "✦ Pure " : "") + type.title.replaceAll("§[0-9a-fk-or]", "") + "]");
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§6✦ Hypospray ✦"));
        textConsumer.accept(Component.literal("§7High-pressure pneumatic needleless drug delivery device."));

        if (isLoaded(stack)) {
            HyposprayCartridgeItem.Type loadedType = getLoadedType(stack);
            boolean isPure = isLoadedPure(stack);
            if (loadedType != null) {
                textConsumer.accept(Component.literal("§a✦ Chamber: §f" + (isPure ? "§d✦ Pure " : "") + loadedType.title + " §7(Ready to fire)"));
                if (isPure) {
                    textConsumer.accept(Component.literal("§d✦ Grade-A Pure Cleanroom Potency (12m Duration)"));
                }
                textConsumer.accept(Component.literal("§e✦ Right-Click: §fInoculate Self or Ally"));
                textConsumer.accept(Component.literal("§8✦ Shift + Right-Click: §7Eject cartridge back to inventory"));
            }
        } else {
            textConsumer.accept(Component.literal("§c✦ Chamber: §7Empty"));
            textConsumer.accept(Component.literal("§e✦ Right-Click with ampoule in hand to load"));
            textConsumer.accept(Component.literal("§e✦ Or place ampoule in offhand & Right-Click Hypospray"));
        }

        textConsumer.accept(Component.literal("§a✔ Recycles: §7Returns an Empty Cartridge after each injection."));
        textConsumer.accept(Component.literal("§c⚠ Medical Notice: §7Wait 5s between injections to avoid Inoculant Sickness."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
