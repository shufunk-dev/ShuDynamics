package net.enchantedwood.item.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

public class HydrogenJetpackItem extends Item {
    public static final int MAX_HYDROGEN = 5_000; // 5,000 mB = 5 Canisters

    public HydrogenJetpackItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    public static int getHydrogen(ItemStack stack) {
        CustomData nbtComponent = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComponent != null) {
            CompoundTag nbt = nbtComponent.copyTag();
            if (nbt.contains("Hydrogen")) {
                return nbt.getIntOr("Hydrogen", 0);
            }
        }
        // Newly crafted/spawned jetpacks come pre-fueled with 2,000 mB from the 2 crafting canisters
        return 2_000;
    }

    public static void setHydrogen(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(amount, MAX_HYDROGEN));
        CompoundTag nbt = new CompoundTag();
        CustomData nbtComponent = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComponent != null) {
            nbt = nbtComponent.copyTag();
        }
        nbt.putInt("Hydrogen", clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((float) getHydrogen(stack) * 13.0f / (float) MAX_HYDROGEN);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00E5FF; // Electric Cyan
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        ItemStack offhand = user.getOffhandItem();

        // Creative mode instant top-off
        if (user.isCreative() && user.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                setHydrogen(stack, MAX_HYDROGEN);
                world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0f, 1.5f);
                user.sendOverlayMessage(Component.literal("§b⚡ Jetpack fully charged (5,000 mB Hydrogen)!"));
            }
            return InteractionResult.SUCCESS;
        }

        // 1. Refuel from offhand canister
        if (offhand.is(ModItems.HYDROGEN_CANISTER)) {
            int current = getHydrogen(stack);
            if (current < MAX_HYDROGEN) {
                if (!world.isClientSide()) {
                    int next = Math.min(current + 1000, MAX_HYDROGEN);
                    setHydrogen(stack, next);
                    offhand.shrink(1);
                    user.getInventory().placeItemBackInInventory(new ItemStack(ModItems.EMPTY_GAS_CANISTER), net.minecraft.util.Prediction.SERVER_ONLY);
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.2f);
                    user.sendOverlayMessage(Component.literal(String.format("§6⚡ Jetpack refueled (+1,000 mB) [%,d / %,d mB]", next, MAX_HYDROGEN)));
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!world.isClientSide()) {
                    user.sendOverlayMessage(Component.literal("§a✔ Jetpack is already full on Hydrogen!"));
                }
                return InteractionResult.CONSUME;
            }
        }

        // 2. Refuel from canister anywhere in inventory
        for (int i = 0; i < user.getInventory().getContainerSize(); i++) {
            ItemStack invStack = user.getInventory().getItem(i);
            if (invStack.is(ModItems.HYDROGEN_CANISTER)) {
                int current = getHydrogen(stack);
                if (current < MAX_HYDROGEN) {
                    if (!world.isClientSide()) {
                        int next = Math.min(current + 1000, MAX_HYDROGEN);
                        setHydrogen(stack, next);
                        invStack.shrink(1);
                        user.getInventory().placeItemBackInInventory(new ItemStack(ModItems.EMPTY_GAS_CANISTER), net.minecraft.util.Prediction.SERVER_ONLY);
                        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.2f);
                        user.sendOverlayMessage(Component.literal(String.format("§6⚡ Jetpack refueled (+1,000 mB) [%,d / %,d mB]", next, MAX_HYDROGEN)));
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        int fuel = getHydrogen(stack);
        textConsumer.accept(Component.literal(String.format("§b⚡ Hydrogen Fuel: §f%,d / %,d mB", fuel, MAX_HYDROGEN)));
        if (fuel > 0) {
            textConsumer.accept(Component.literal("§a● Flight Propulsion: ONLINE"));
            textConsumer.accept(Component.literal("§7Equip in Chest slot & double-tap Space to fly!"));
        } else {
            textConsumer.accept(Component.literal("§c○ Flight Propulsion: OFFLINE (Empty)"));
            textConsumer.accept(Component.literal("§eRefuel: §7Right-Click with Hydrogen Canister."));
        }
        textConsumer.accept(Component.literal("§d✨ Built-in parachute fall dampener"));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
