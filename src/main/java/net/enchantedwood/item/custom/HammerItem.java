package net.enchantedwood.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class HammerItem extends Item {
    private static final ThreadLocal<Boolean> IS_MINING_AREA = ThreadLocal.withInitial(() -> false);

    public HammerItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay displayComponent, java.util.function.Consumer<net.minecraft.network.chat.Component> textConsumer, net.minecraft.world.item.TooltipFlag type) {
        textConsumer.accept(net.minecraft.network.chat.Component.literal("§7Mines a §e3×3 area §7centered on targeted block."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }


    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide() && miner instanceof ServerPlayer player && !IS_MINING_AREA.get()) {
            if (state.getDestroySpeed(world, pos) > 0.0f) {
                IS_MINING_AREA.set(true);
                try {
                    mine3x3Area(stack, (ServerLevel) world, pos, player);
                } finally {
                    IS_MINING_AREA.set(false);
                }
            }
        }
        return super.mineBlock(stack, world, state, pos, miner);
    }

    private void mine3x3Area(ItemStack stack, ServerLevel world, BlockPos origin, ServerPlayer player) {
        Direction side = getTargetedSide(player, origin);

        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;

        switch (side.getAxis()) {
            case Y:
                minX = -1; maxX = 1;
                minZ = -1; maxZ = 1;
                break;
            case X:
                minY = -1; maxY = 1;
                minZ = -1; maxZ = 1;
                break;
            case Z:
                minX = -1; maxX = 1;
                minY = -1; maxY = 1;
                break;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos targetPos = origin.offset(x, y, z);
                    BlockState targetState = world.getBlockState(targetPos);

                    if (canHarvestBlock(targetState, world, targetPos)) {
                        player.gameMode.destroyBlock(targetPos);
                        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                        if (stack.isEmpty()) return;
                    }
                }
            }
        }
    }

    private boolean canHarvestBlock(BlockState state, Level world, BlockPos pos) {
        if (state.isAir() || state.getDestroySpeed(world, pos) < 0) return false;
        // Do not mine machines / BlockEntities in 3x3 area so machines are picked up 1 at a time!
        if (state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock || world.getBlockEntity(pos) != null) return false;

        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.NEEDS_STONE_TOOL)
                || state.is(BlockTags.NEEDS_IRON_TOOL)
                || state.is(BlockTags.NEEDS_DIAMOND_TOOL)
                || !state.requiresCorrectToolForDrops();
    }

    private Direction getTargetedSide(Player player, BlockPos pos) {
        if (player.getXRot() > 40.0f) {
            return Direction.UP;
        } else if (player.getXRot() < -40.0f) {
            return Direction.DOWN;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 rotation = player.getViewVector(1.0f);
        Vec3 reachVec = eyePos.add(rotation.x * 5.0, rotation.y * 5.0, rotation.z * 5.0);

        BlockHitResult hit = player.level().clip(new ClipContext(
                eyePos,
                reachVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getDirection();
        }
        return player.getDirection().getOpposite();
    }
}
