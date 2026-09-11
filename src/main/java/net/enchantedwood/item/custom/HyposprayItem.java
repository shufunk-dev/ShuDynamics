package net.enchantedwood.item.custom;

import net.enchantedwood.effect.ModStatusEffects;
import net.enchantedwood.item.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class HyposprayItem extends Item {
    public static final String NBT_LOADED_KEY = "LoadedCartridge";
    public static final String NBT_PURE_KEY = "LoadedPure";

    public HyposprayItem(Settings settings) {
        super(settings);
    }

    public static boolean isLoaded(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.contains(DataComponentTypes.CUSTOM_DATA)) return false;
        NbtCompound nbt = stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        return nbt.contains(NBT_LOADED_KEY) && !nbt.getString(NBT_LOADED_KEY, "").isEmpty();
    }

    public static boolean isLoadedPure(ItemStack stack) {
        if (!isLoaded(stack)) return false;
        NbtCompound nbt = stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        return nbt.getBoolean(NBT_PURE_KEY, false);
    }

    public static HyposprayCartridgeItem.Type getLoadedType(ItemStack stack) {
        if (!isLoaded(stack)) return null;
        NbtCompound nbt = stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        String name = nbt.getString(NBT_LOADED_KEY, "");
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
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putString(NBT_LOADED_KEY, type.name());
        nbt.putBoolean(NBT_PURE_KEY, isPure);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static void clearLoadedCartridge(ItemStack stack) {
        if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
            NbtCompound nbt = stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
            nbt.remove(NBT_LOADED_KEY);
            nbt.remove(NBT_PURE_KEY);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
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
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(ModItems.HYPOSPRAY) && entity instanceof LivingEntity livingEntity) {
                HyposprayItem hypospray = (HyposprayItem) stack.getItem();

                // If empty, check if offhand has a cartridge to load
                if (!isLoaded(stack)) {
                    ItemStack offhand = player.getOffHandStack();
                    if (!offhand.isEmpty() && offhand.getItem() instanceof HyposprayCartridgeItem cartridgeItem) {
                        boolean pure = HyposprayCartridgeItem.isPure(offhand);
                        if (!world.isClient()) {
                            setLoadedCartridge(stack, cartridgeItem.getCartridgeType(), pure);
                            offhand.decrement(1);
                            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_CROSSBOW_LOADING_END.value(), SoundCategory.PLAYERS, 0.9f, 1.7f);
                            player.sendMessage(Text.literal("§e✦ Loaded Hypospray with " + (pure ? "§d✦ Pure " : "") + cartridgeItem.getCartridgeType().title + " ✦"), true);
                        }
                        player.swingHand(hand);
                        return ActionResult.SUCCESS;
                    }
                    if (!world.isClient()) {
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.6f, 1.8f);
                        player.sendMessage(Text.literal("§e[Hypospray] Chamber empty! Load an ampoule to inoculate " + livingEntity.getName().getString() + "."), true);
                    }
                    return ActionResult.FAIL;
                }

                ActionResult result = hypospray.executeInoculation(world, player, livingEntity, stack, hand);
                if (result == ActionResult.SUCCESS) {
                    player.swingHand(hand);
                    return ActionResult.SUCCESS;
                }
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack hyposprayStack = user.getStackInHand(hand);

        // 1. Unload cartridge on Sneak + Right-Click
        if (user.isSneaking() && isLoaded(hyposprayStack)) {
            if (!world.isClient()) {
                HyposprayCartridgeItem.Type loadedType = getLoadedType(hyposprayStack);
                boolean wasPure = isLoadedPure(hyposprayStack);
                clearLoadedCartridge(hyposprayStack);
                if (loadedType != null) {
                    ItemStack ejected = new ItemStack(getCartridgeItem(loadedType));
                    if (wasPure) {
                        HyposprayCartridgeItem.setPure(ejected, true);
                    }
                    user.giveItemStack(ejected);
                }
                world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_CROSSBOW_LOADING_START.value(), SoundCategory.PLAYERS, 0.8f, 1.4f);
                user.sendMessage(Text.literal("§7✦ Ejected cartridge from Hypospray ✦"), true);
            }
            user.swingHand(hand);
            return ActionResult.SUCCESS;
        }

        // 2. If empty, check if offhand has a cartridge to load
        if (!isLoaded(hyposprayStack)) {
            ItemStack offhand = user.getOffHandStack();
            if (!offhand.isEmpty() && offhand.getItem() instanceof HyposprayCartridgeItem cartridgeItem) {
                boolean pure = HyposprayCartridgeItem.isPure(offhand);
                if (!world.isClient()) {
                    setLoadedCartridge(hyposprayStack, cartridgeItem.getCartridgeType(), pure);
                    offhand.decrement(1);
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_CROSSBOW_LOADING_END.value(), SoundCategory.PLAYERS, 0.9f, 1.7f);
                    user.sendMessage(Text.literal("§e✦ Loaded Hypospray with " + (pure ? "§d✦ Pure " : "") + cartridgeItem.getCartridgeType().title + " ✦"), true);
                }
                user.swingHand(hand);
                return ActionResult.SUCCESS;
            } else {
                if (!world.isClient()) {
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.6f, 1.8f);
                    user.sendMessage(Text.literal("§e[Hypospray] Chamber empty! Right-click an ampoule or hold in offhand to load."), true);
                }
                return ActionResult.FAIL;
            }
        }

        // 3. Loaded: Inoculate Self!
        return executeInoculation(world, user, user, hyposprayStack, hand);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        return executeInoculation(entity.getEntityWorld(), user, entity, stack, hand);
    }

    public ActionResult executeInoculation(World world, PlayerEntity user, LivingEntity target, ItemStack hyposprayStack, Hand hand) {
        if (!isLoaded(hyposprayStack)) {
            return ActionResult.FAIL;
        }

        HyposprayCartridgeItem.Type loadedType = getLoadedType(hyposprayStack);
        if (loadedType == null) return ActionResult.FAIL;
        boolean isPure = isLoadedPure(hyposprayStack);

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            boolean isOverdose = target.hasStatusEffect(ModStatusEffects.METABOLIC_SATURATION);

            if (isOverdose) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 10, 1));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 10, 1));
                target.damage(serverWorld, serverWorld.getDamageSources().magic(), 4.0f); // 2 hearts rejection damage

                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.6f, 1.6f);
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.8f, 0.8f);
                serverWorld.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.4, 0.3, 0.05);

                user.sendMessage(Text.literal("§c⚠ WARNING: Metabolic Inoculant Overload! Chemical sickness induced! ⚠"), true);
                applyEffects(loadedType, target, true, isPure);
            } else {
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.9f, 1.8f);
                serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.6f, 1.7f);

                serverWorld.spawnParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.2, 0.3, 0.2, 0.03);
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.2, target.getZ(), 4, 0.2, 0.2, 0.2, 0.02);

                if (isPure) {
                    serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1.5f);
                    serverWorld.spawnParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 18, 0.3, 0.4, 0.3, 0.06);
                }

                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.METABOLIC_SATURATION, 100, 0));
                applyEffects(loadedType, target, false, isPure);

                String targetName = (target == user) ? "Self" : target.getName().getString();
                if (isPure) {
                    user.sendMessage(Text.literal("§d✦ PURE Inoculation administered (" + targetName + "): " + loadedType.title + " §e(12m Duration / Grade-A Potency) ✦"), true);
                } else {
                    user.sendMessage(Text.literal("§a✔ Inoculation administered (" + targetName + "): " + loadedType.title + " §7(Metabolic rest: 5s)"), true);
                }
            }

            // Chamber is now empty!
            clearLoadedCartridge(hyposprayStack);
            // Refund empty cartridge casing
            user.giveItemStack(new ItemStack(ModItems.EMPTY_CARTRIDGE));
        }

        return ActionResult.SUCCESS;
    }

    public static void applyEffects(HyposprayCartridgeItem.Type type, LivingEntity target, boolean diminished) {
        applyEffects(type, target, diminished, false);
    }

    public static void applyEffects(HyposprayCartridgeItem.Type type, LivingEntity target, boolean diminished, boolean isPure) {
        float factor = diminished ? 0.5f : 1.0f;
        switch (type) {
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
    public Text getName(ItemStack stack) {
        if (isLoaded(stack)) {
            HyposprayCartridgeItem.Type type = getLoadedType(stack);
            if (type != null) {
                boolean isPure = isLoadedPure(stack);
                return Text.literal("Hypospray [" + (isPure ? "✦ Pure " : "") + type.title.replaceAll("§[0-9a-fk-or]", "") + "]");
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§6✦ Hypospray ✦"));
        textConsumer.accept(Text.literal("§7High-pressure pneumatic needleless drug delivery device."));

        if (isLoaded(stack)) {
            HyposprayCartridgeItem.Type loadedType = getLoadedType(stack);
            boolean isPure = isLoadedPure(stack);
            if (loadedType != null) {
                textConsumer.accept(Text.literal("§a✦ Chamber: §f" + (isPure ? "§d✦ Pure " : "") + loadedType.title + " §7(Ready to fire)"));
                if (isPure) {
                    textConsumer.accept(Text.literal("§d✦ Grade-A Pure Cleanroom Potency (12m Duration)"));
                }
                textConsumer.accept(Text.literal("§e✦ Right-Click: §fInoculate Self or Ally"));
                textConsumer.accept(Text.literal("§8✦ Shift + Right-Click: §7Eject cartridge back to inventory"));
            }
        } else {
            textConsumer.accept(Text.literal("§c✦ Chamber: §7Empty"));
            textConsumer.accept(Text.literal("§e✦ Right-Click with ampoule in hand to load"));
            textConsumer.accept(Text.literal("§e✦ Or place ampoule in offhand & Right-Click Hypospray"));
        }

        textConsumer.accept(Text.literal("§a✔ Recycles: §7Returns an Empty Cartridge after each injection."));
        textConsumer.accept(Text.literal("§c⚠ Medical Notice: §7Wait 5s between injections to avoid Inoculant Sickness."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
