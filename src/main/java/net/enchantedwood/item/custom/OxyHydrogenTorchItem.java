package net.enchantedwood.item.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public class OxyHydrogenTorchItem extends Item {
    public static final int MAX_FUEL = 2_000; // 2,000 mB

    public OxyHydrogenTorchItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    public static int getFuel(ItemStack stack) {
        CustomData nbtComponent = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComponent != null) {
            return nbtComponent.copyTag().getIntOr("TorchFuel", 0);
        }
        return 0;
    }

    public static void setFuel(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(amount, MAX_FUEL));
        CompoundTag nbt = new CompoundTag();
        CustomData nbtComponent = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComponent != null) {
            nbt = nbtComponent.copyTag();
        }
        nbt.putInt("TorchFuel", clamped);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, nbt);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        ItemStack offhand = user.getOffhandItem();

        // Refuel with Oxygen or Hydrogen
        if (offhand.is(ModItems.OXYGEN_CANISTER) || offhand.is(ModItems.HYDROGEN_CANISTER)) {
            int current = getFuel(stack);
            if (current < MAX_FUEL) {
                setFuel(stack, current + 500);
                offhand.shrink(1);
                user.getInventory().placeItemBackInInventory(new ItemStack(ModItems.EMPTY_GAS_CANISTER), net.minecraft.util.Prediction.SERVER_ONLY);
                world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.5f);
                user.sendOverlayMessage(Component.literal("§bTorch refueled (+500 mB Oxy-Hydrogen)"));
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && player.isShiftKeyDown()) {
            int fuel = getFuel(stack);
            if (fuel >= 10 || (player.isCreative())) {
                // Instant dismantle modded machine / block
                if (!world.isClientSide()) {
                    if (!player.isCreative()) {
                        setFuel(stack, fuel - 10);
                    }
                    BlockEntity be = world.getBlockEntity(pos);
                    ItemStack drop = new ItemStack(state.getBlock().asItem());
                    world.destroyBlock(pos, false);
                    Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), drop);
                    world.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 1.4f);
                    player.sendOverlayMessage(Component.literal("§aDismantled block with Oxy-Hydrogen Torch!"));
                }
                return InteractionResult.SUCCESS;
            } else {
                if (world.isClientSide()) {
                    player.sendOverlayMessage(Component.literal("§cTorch is out of Oxy-Hydrogen fuel!"));
                }
                return InteractionResult.FAIL;
            }
        }
        return super.useOn(context);
    }
}
