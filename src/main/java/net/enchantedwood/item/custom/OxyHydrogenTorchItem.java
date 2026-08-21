package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.enchantedwood.item.ModItems;

import java.util.List;

public class OxyHydrogenTorchItem extends Item {
    public static final int MAX_FUEL = 2_000; // 2,000 mB

    public OxyHydrogenTorchItem(Settings settings) {
        super(settings.maxCount(1));
    }

    public static int getFuel(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            return nbtComponent.copyNbt().getInt("TorchFuel", 0);
        }
        return 0;
    }

    public static void setFuel(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(amount, MAX_FUEL));
        NbtCompound nbt = new NbtCompound();
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            nbt = nbtComponent.copyNbt();
        }
        nbt.putInt("TorchFuel", clamped);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        ItemStack offhand = user.getOffHandStack();

        // Refuel with Oxygen or Hydrogen
        if (offhand.isOf(ModItems.OXYGEN_CANISTER) || offhand.isOf(ModItems.HYDROGEN_CANISTER)) {
            int current = getFuel(stack);
            if (current < MAX_FUEL) {
                setFuel(stack, current + 500);
                offhand.decrement(1);
                user.getInventory().offerOrDrop(new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.5f);
                user.sendMessage(Text.literal("§bTorch refueled (+500 mB Oxy-Hydrogen)"), true);
                return ActionResult.SUCCESS;
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (player != null && player.isSneaking()) {
            int fuel = getFuel(stack);
            if (fuel >= 10 || (player.isCreative())) {
                // Instant dismantle modded machine / block
                if (!world.isClient()) {
                    if (!player.isCreative()) {
                        setFuel(stack, fuel - 10);
                    }
                    BlockEntity be = world.getBlockEntity(pos);
                    ItemStack drop = new ItemStack(state.getBlock().asItem());
                    world.breakBlock(pos, false);
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), drop);
                    world.playSound(null, pos, SoundEvents.BLOCK_NETHERITE_BLOCK_BREAK, SoundCategory.BLOCKS, 1.0f, 1.4f);
                    player.sendMessage(Text.literal("§aDismantled block with Oxy-Hydrogen Torch!"), true);
                }
                return ActionResult.SUCCESS;
            } else {
                if (world.isClient()) {
                    player.sendMessage(Text.literal("§cTorch is out of Oxy-Hydrogen fuel!"), true);
                }
                return ActionResult.FAIL;
            }
        }
        return super.useOnBlock(context);
    }
}
