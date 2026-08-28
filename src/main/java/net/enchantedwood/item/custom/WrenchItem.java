package net.enchantedwood.item.custom;

import net.enchantedwood.util.Wrenchable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WrenchItem extends Item {
    public WrenchItem(Settings settings) {
        super(settings.maxCount(1));
    }

    public static Direction getTargetedDirection(BlockPos pos, Direction side, Vec3d hitPos) {
        double dx = hitPos.x - (pos.getX() + 0.5);
        double dy = hitPos.y - (pos.getY() + 0.5);
        double dz = hitPos.z - (pos.getZ() + 0.5);

        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);

        // If clicked on a protruding arm
        if (absX > 0.22 || absY > 0.22 || absZ > 0.22) {
            if (absX > absY && absX > absZ) {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            } else if (absY > absX && absY > absZ) {
                return dy > 0 ? Direction.UP : Direction.DOWN;
            } else {
                return dz > 0 ? Direction.SOUTH : Direction.NORTH;
            }
        }

        return side;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        Direction side = context.getSide();
        Vec3d hitPos = context.getHitPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (player == null) return ActionResult.PASS;

        Direction targetDir = getTargetedDirection(pos, side, hitPos);

        // 1. Check if block implements Wrenchable
        if (block instanceof Wrenchable wrenchable) {
            if (player.isSneaking()) {
                return wrenchable.onShiftWrenched(world, pos, player, targetDir);
            } else {
                return wrenchable.onWrenched(world, pos, player, targetDir);
            }
        }

        // 2. Shift + Right-Click Dismantle on mod blocks
        if (player.isSneaking()) {
            if (state.getBlock().asItem() != null && state.getHardness(world, pos) >= 0) {
                if (!world.isClient()) {
                    world.breakBlock(pos, true, player);
                    world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    player.sendMessage(Text.literal("§6[Wrench] §eDismantled " + block.getName().getString()), true);
                }
                return ActionResult.SUCCESS;
            }
        }

        // 3. Right-Click Machine Rotation
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            if (!world.isClient()) {
                Direction current = state.get(Properties.HORIZONTAL_FACING);
                Direction next = current.rotateYClockwise();
                world.setBlockState(pos, state.with(Properties.HORIZONTAL_FACING, next), Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.BLOCK_COPPER_GRATE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.2f);
                player.sendMessage(Text.literal("§6[Wrench] §aRotated " + next.asString().toUpperCase()), true);
            }
            return ActionResult.SUCCESS;
        } else if (state.contains(Properties.FACING)) {
            if (!world.isClient()) {
                Direction current = state.get(Properties.FACING);
                Direction[] all = Direction.values();
                Direction next = all[(current.ordinal() + 1) % all.length];
                world.setBlockState(pos, state.with(Properties.FACING, next), Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.BLOCK_COPPER_GRATE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.2f);
                player.sendMessage(Text.literal("§6[Wrench] §aFacing " + next.asString().toUpperCase()), true);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
