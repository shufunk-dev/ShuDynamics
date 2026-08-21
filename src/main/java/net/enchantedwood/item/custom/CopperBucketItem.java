package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.FluidFillable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.enchantedwood.item.ModItems;

public class CopperBucketItem extends BucketItem {
    private final Fluid fluid;

    public CopperBucketItem(Fluid fluid, Settings settings) {
        super(fluid, settings);
        this.fluid = fluid;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        BlockHitResult hitResult = raycast(
            world, user, this.fluid == Fluids.EMPTY ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE
        );

        if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() != HitResult.Type.BLOCK) {
            return ActionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos();
        Direction direction = hitResult.getSide();
        BlockPos targetPos = pos.offset(direction);

        if (this.fluid == Fluids.EMPTY) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.getBlock() instanceof FluidDrainable fluidDrainable) {
                ItemStack drainedStack = fluidDrainable.tryDrainFluid(user, world, pos, blockState);
                if (!drainedStack.isEmpty()) {
                    user.incrementStat(Stats.USED.getOrCreateStat(this));
                    fluidDrainable.getBucketFillSound().ifPresent(sound -> user.playSound(sound, 1.0F, 1.0F));
                    world.emitGameEvent(user, GameEvent.FLUID_PICKUP, pos);

                    ItemStack copperFilledStack = getCopperBucketVariant(drainedStack);
                    ItemStack finalStack = ItemUsage.exchangeStack(itemStack, user, copperFilledStack);

                    return ActionResult.SUCCESS.withNewHandStack(finalStack);
                }
            }
            return ActionResult.FAIL;
        } else {
            BlockState state = world.getBlockState(pos);
            BlockPos placePos = state.getBlock() instanceof FluidFillable && this.fluid == Fluids.WATER ? pos : targetPos;
            
            if (this.placeFluid(user, world, placePos, hitResult)) {
                this.onEmptied(user, world, itemStack, placePos);
                if (user != null) {
                    user.incrementStat(Stats.USED.getOrCreateStat(this));
                }

                ItemStack emptyBucket = (this == ModItems.ENCHANTED_LAVA_BUCKET) ? new ItemStack(net.minecraft.item.Items.BUCKET) : new ItemStack(ModItems.COPPER_BUCKET);
                ItemStack finalStack = ItemUsage.exchangeStack(itemStack, user, emptyBucket);
                return ActionResult.SUCCESS.withNewHandStack(finalStack);
            }
            return ActionResult.FAIL;
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.isOf(ModItems.ENCHANTED_LAVA_BUCKET) || stack.isOf(ModItems.ENCHANTED_COPPER_LAVA_BUCKET);
    }

    private ItemStack getCopperBucketVariant(ItemStack drainedStack) {
        if (drainedStack.isOf(net.minecraft.item.Items.WATER_BUCKET)) {
            return new ItemStack(ModItems.COPPER_WATER_BUCKET);
        } else if (drainedStack.isOf(net.minecraft.item.Items.LAVA_BUCKET)) {
            return new ItemStack(ModItems.COPPER_LAVA_BUCKET);
        }
        return drainedStack;
    }
}
