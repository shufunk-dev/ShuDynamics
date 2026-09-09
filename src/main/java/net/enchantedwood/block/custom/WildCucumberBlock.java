package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class WildCucumberBlock extends PlantBlock {
    public static final MapCodec<WildCucumberBlock> CODEC = createCodec(WildCucumberBlock::new);
    private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 12.0, 15.0);

    public WildCucumberBlock(Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<WildCucumberBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return super.canPlantOnTop(floor, world, pos)
                || floor.isOf(net.minecraft.block.Blocks.FARMLAND)
                || floor.isOf(net.enchantedwood.block.ModBlocks.VOLCANIC_SOIL);
    }
}
