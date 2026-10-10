package net.enchantedwood.item.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CopperBucketItem extends BucketItem {
    private final Fluid fluid;

    public CopperBucketItem(Fluid fluid, Properties settings) {
        super(fluid, settings);
        this.fluid = fluid;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(
            world, user, this.fluid == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE
        );

        if (hitResult.getType() == HitResult.Type.MISS || hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos();
        Direction direction = hitResult.getDirection();
        BlockPos targetPos = pos.relative(direction);

        if (this.fluid == Fluids.EMPTY) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.getBlock() instanceof BucketPickup fluidDrainable) {
                ItemStack drainedStack = fluidDrainable.pickupBlock(user, world, pos, blockState);
                if (!drainedStack.isEmpty()) {
                    user.awardStat(Stats.ITEM_USED.get(this));
                    fluidDrainable.getPickupSound().ifPresent(sound -> user.playSound(sound, 1.0F, 1.0F));
                    world.gameEvent(user, GameEvent.FLUID_PICKUP, pos);

                    ItemStack copperFilledStack = getCopperBucketVariant(drainedStack);
                    ItemStack finalStack = ItemUtils.createFilledResult(itemStack, user, copperFilledStack);

                    return InteractionResult.SUCCESS.heldItemTransformedTo(finalStack);
                }
            }
            return InteractionResult.FAIL;
        } else {
            BlockState state = world.getBlockState(pos);
            BlockPos placePos = state.getBlock() instanceof LiquidBlockContainer && this.fluid == Fluids.WATER ? pos : targetPos;
            
            if (this.emptyContents(user, world, placePos, hitResult)) {
                this.checkExtraContent(user, world, itemStack, placePos);
                if (user != null) {
                    user.awardStat(Stats.ITEM_USED.get(this));
                }

                ItemStack emptyBucket = (this == ModItems.ENCHANTED_LAVA_BUCKET) ? new ItemStack(net.minecraft.world.item.Items.BUCKET) : new ItemStack(ModItems.COPPER_BUCKET);
                ItemStack finalStack = ItemUtils.createFilledResult(itemStack, user, emptyBucket);
                return InteractionResult.SUCCESS.heldItemTransformedTo(finalStack);
            }
            return InteractionResult.FAIL;
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.is(ModItems.ENCHANTED_LAVA_BUCKET) || stack.is(ModItems.ENCHANTED_COPPER_LAVA_BUCKET);
    }

    private ItemStack getCopperBucketVariant(ItemStack drainedStack) {
        if (drainedStack.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
            return new ItemStack(ModItems.COPPER_WATER_BUCKET);
        } else if (drainedStack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
            return new ItemStack(ModItems.COPPER_LAVA_BUCKET);
        }
        return drainedStack;
    }
}
