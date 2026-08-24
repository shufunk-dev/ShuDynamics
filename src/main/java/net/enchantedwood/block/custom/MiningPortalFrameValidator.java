package net.enchantedwood.block.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.enchantedwood.block.ModBlocks;

import java.util.ArrayList;
import java.util.List;

public class MiningPortalFrameValidator {

    public static class FrameResult {
        public final boolean valid;
        public final Direction.Axis axis;
        public final List<BlockPos> interiorPositions;

        public FrameResult(boolean valid, Direction.Axis axis, List<BlockPos> interiorPositions) {
            this.valid = valid;
            this.axis = axis;
            this.interiorPositions = interiorPositions;
        }
    }

    public static FrameResult tryFindFrame(World world, BlockPos clickedPos, Direction clickedFace) {
        // Search in adjacent positions to the clicked block
        for (Direction dir : Direction.values()) {
            BlockPos airPos = clickedPos.offset(dir);
            if (world.isAir(airPos)) {
                // Try X axis (runs along North-South)
                FrameResult xResult = checkFrameOnAxis(world, airPos, Direction.Axis.X);
                if (xResult.valid) return xResult;

                // Try Z axis (runs along East-West)
                FrameResult zResult = checkFrameOnAxis(world, airPos, Direction.Axis.Z);
                if (zResult.valid) return zResult;
            }
        }
        return new FrameResult(false, Direction.Axis.X, List.of());
    }

    private static FrameResult checkFrameOnAxis(World world, BlockPos startAirPos, Direction.Axis axis) {
        Direction widthDir = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;

        // Find bottom-left of air region
        BlockPos.Mutable current = startAirPos.mutableCopy();
        while (world.isAir(current.down()) && current.getY() > world.getBottomY()) {
            current.move(Direction.DOWN);
        }
        while (world.isAir(current.offset(widthDir.getOpposite()))) {
            current.move(widthDir.getOpposite());
        }

        BlockPos bottomLeftAir = current.toImmutable();

        // Measure width of air
        int width = 0;
        BlockPos.Mutable wCheck = bottomLeftAir.mutableCopy();
        while (world.isAir(wCheck) && width <= 21) {
            width++;
            wCheck.move(widthDir);
        }

        if (width < 2 || width > 21) {
            return new FrameResult(false, axis, List.of());
        }

        // Measure height of air
        int height = 0;
        BlockPos.Mutable hCheck = bottomLeftAir.mutableCopy();
        while (world.isAir(hCheck) && height <= 21) {
            height++;
            hCheck.move(Direction.UP);
        }

        if (height < 3 || height > 21) {
            return new FrameResult(false, axis, List.of());
        }

        List<BlockPos> interior = new ArrayList<>();

        // Validate complete rectangle interior & borders
        for (int w = -1; w <= width; w++) {
            for (int h = -1; h <= height; h++) {
                BlockPos pos = bottomLeftAir.offset(widthDir, w).up(h);
                boolean isBorder = (w == -1 || w == width || h == -1 || h == height);

                if (isBorder) {
                    // Border corners are optional; non-corners MUST be Enchanted Cobblestone
                    boolean isCorner = (w == -1 || w == width) && (h == -1 || h == height);
                    if (!isCorner) {
                        BlockState state = world.getBlockState(pos);
                        if (!state.isOf(ModBlocks.ENCHANTED_COBBLESTONE)) {
                            return new FrameResult(false, axis, List.of());
                        }
                    }
                } else {
                    // Interior must be Air
                    if (!world.isAir(pos) && !world.getBlockState(pos).isOf(ModBlocks.MINING_PORTAL)) {
                        return new FrameResult(false, axis, List.of());
                    }
                    interior.add(pos);
                }
            }
        }

        return new FrameResult(true, axis, interior);
    }
}
