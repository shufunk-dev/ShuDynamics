package net.enchantedwood.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
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

import java.util.List;

public class HydrogenJetpackItem extends Item {
    public static final int MAX_HYDROGEN = 5_000; // 5,000 mB = 5 Canisters

    public HydrogenJetpackItem(Settings settings) {
        super(settings.maxCount(1));
    }

    public static int getHydrogen(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            return nbtComponent.copyNbt().getInt("Hydrogen", 0);
        }
        return 0;
    }

    public static void setHydrogen(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(amount, MAX_HYDROGEN));
        NbtCompound nbt = new NbtCompound();
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            nbt = nbtComponent.copyNbt();
        }
        nbt.putInt("Hydrogen", clamped);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        ItemStack offhand = user.getOffHandStack();

        // Refuel with Hydrogen Canister
        if (offhand.isOf(ModItems.HYDROGEN_CANISTER)) {
            int current = getHydrogen(stack);
            if (current < MAX_HYDROGEN) {
                if (!world.isClient()) {
                    setHydrogen(stack, current + 1000);
                    offhand.decrement(1);
                    user.getInventory().offerOrDrop(new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.2f);
                    user.sendMessage(Text.literal("§6Jetpack refueled (+1000 mB Hydrogen)"), true);
                }
                return ActionResult.SUCCESS;
            }
        }
        return super.use(world, user, hand);
    }
}
