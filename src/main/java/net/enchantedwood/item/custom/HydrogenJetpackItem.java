package net.enchantedwood.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.enchantedwood.item.ModItems;

import java.util.function.Consumer;

public class HydrogenJetpackItem extends Item {
    public static final int MAX_HYDROGEN = 5_000; // 5,000 mB = 5 Canisters

    public HydrogenJetpackItem(Settings settings) {
        super(settings.maxCount(1));
    }

    public static int getHydrogen(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            NbtCompound nbt = nbtComponent.copyNbt();
            if (nbt.contains("Hydrogen")) {
                return nbt.getInt("Hydrogen", 0);
            }
        }
        // Newly crafted/spawned jetpacks come pre-fueled with 2,000 mB from the 2 crafting canisters
        return 2_000;
    }

    public static void setHydrogen(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(amount, MAX_HYDROGEN));
        NbtCompound nbt = new NbtCompound();
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            nbt = nbtComponent.copyNbt();
        }
        nbt.putInt("Hydrogen", clamped);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round((float) getHydrogen(stack) * 13.0f / (float) MAX_HYDROGEN);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x00E5FF; // Electric Cyan
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        ItemStack offhand = user.getOffHandStack();

        // Creative mode instant top-off
        if (user.isCreative() && user.isSneaking()) {
            if (!world.isClient()) {
                setHydrogen(stack, MAX_HYDROGEN);
                world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 1.5f);
                user.sendMessage(Text.literal("§b⚡ Jetpack fully charged (5,000 mB Hydrogen)!"), true);
            }
            return ActionResult.SUCCESS;
        }

        // 1. Refuel from offhand canister
        if (offhand.isOf(ModItems.HYDROGEN_CANISTER)) {
            int current = getHydrogen(stack);
            if (current < MAX_HYDROGEN) {
                if (!world.isClient()) {
                    int next = Math.min(current + 1000, MAX_HYDROGEN);
                    setHydrogen(stack, next);
                    offhand.decrement(1);
                    user.getInventory().offerOrDrop(new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.2f);
                    user.sendMessage(Text.literal(String.format("§6⚡ Jetpack refueled (+1,000 mB) [%,d / %,d mB]", next, MAX_HYDROGEN)), true);
                }
                return ActionResult.SUCCESS;
            } else {
                if (!world.isClient()) {
                    user.sendMessage(Text.literal("§a✔ Jetpack is already full on Hydrogen!"), true);
                }
                return ActionResult.CONSUME;
            }
        }

        // 2. Refuel from canister anywhere in inventory
        for (int i = 0; i < user.getInventory().size(); i++) {
            ItemStack invStack = user.getInventory().getStack(i);
            if (invStack.isOf(ModItems.HYDROGEN_CANISTER)) {
                int current = getHydrogen(stack);
                if (current < MAX_HYDROGEN) {
                    if (!world.isClient()) {
                        int next = Math.min(current + 1000, MAX_HYDROGEN);
                        setHydrogen(stack, next);
                        invStack.decrement(1);
                        user.getInventory().offerOrDrop(new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.2f);
                        user.sendMessage(Text.literal(String.format("§6⚡ Jetpack refueled (+1,000 mB) [%,d / %,d mB]", next, MAX_HYDROGEN)), true);
                    }
                    return ActionResult.SUCCESS;
                }
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        int fuel = getHydrogen(stack);
        textConsumer.accept(Text.literal(String.format("§b⚡ Hydrogen Fuel: §f%,d / %,d mB", fuel, MAX_HYDROGEN)));
        if (fuel > 0) {
            textConsumer.accept(Text.literal("§a● Flight Propulsion: ONLINE"));
            textConsumer.accept(Text.literal("§7Equip in Chest slot & double-tap Space to fly!"));
        } else {
            textConsumer.accept(Text.literal("§c○ Flight Propulsion: OFFLINE (Empty)"));
            textConsumer.accept(Text.literal("§eRefuel: §7Right-Click with Hydrogen Canister."));
        }
        textConsumer.accept(Text.literal("§d✨ Built-in parachute fall dampener"));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
