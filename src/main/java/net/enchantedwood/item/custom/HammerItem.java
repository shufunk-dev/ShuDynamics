package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class HammerItem extends Item {
    private static final ThreadLocal<Boolean> IS_MINING_AREA = ThreadLocal.withInitial(() -> false);

    public HammerItem(Settings settings) {
        super(settings);
    }


    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient() && miner instanceof ServerPlayerEntity player && !IS_MINING_AREA.get()) {
            if (state.getHardness(world, pos) > 0.0f) {
                IS_MINING_AREA.set(true);
                try {
                    mine3x3Area(stack, (ServerWorld) world, pos, player);
                } finally {
                    IS_MINING_AREA.set(false);
                }
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    private void mine3x3Area(ItemStack stack, ServerWorld world, BlockPos origin, ServerPlayerEntity player) {
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

                    BlockPos targetPos = origin.add(x, y, z);
                    BlockState targetState = world.getBlockState(targetPos);

                    if (canHarvestBlock(targetState, world, targetPos)) {
                        player.interactionManager.tryBreakBlock(targetPos);
                        stack.damage(1, player, EquipmentSlot.MAINHAND);
                        if (stack.isEmpty()) return;
                    }
                }
            }
        }
    }

    private boolean canHarvestBlock(BlockState state, World world, BlockPos pos) {
        if (state.isAir() || state.getHardness(world, pos) < 0) return false;
        // Do not mine machines / BlockEntities in 3x3 area so machines are picked up 1 at a time!
        if (state.getBlock() instanceof net.minecraft.block.BlockEntityProvider || world.getBlockEntity(pos) != null) return false;

        return state.isIn(BlockTags.PICKAXE_MINEABLE)
                || state.isIn(BlockTags.SHOVEL_MINEABLE)
                || state.isIn(BlockTags.NEEDS_STONE_TOOL)
                || state.isIn(BlockTags.NEEDS_IRON_TOOL)
                || state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)
                || !state.isToolRequired();
    }

    private Direction getTargetedSide(PlayerEntity player, BlockPos pos) {
        if (player.getPitch() > 40.0f) {
            return Direction.UP;
        } else if (player.getPitch() < -40.0f) {
            return Direction.DOWN;
        }

        Vec3d eyePos = player.getEyePos();
        Vec3d rotation = player.getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(rotation.x * 5.0, rotation.y * 5.0, rotation.z * 5.0);

        BlockHitResult hit = player.getEntityWorld().raycast(new RaycastContext(
                eyePos,
                reachVec,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getSide();
        }
        return player.getHorizontalFacing().getOpposite();
    }
}
