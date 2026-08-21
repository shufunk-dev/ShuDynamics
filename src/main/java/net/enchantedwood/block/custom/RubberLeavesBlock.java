package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class RubberLeavesBlock extends LeavesBlock {
    public static final MapCodec<RubberLeavesBlock> CODEC = createCodec(settings -> new RubberLeavesBlock(0.01f, settings));

    public RubberLeavesBlock(float leafParticleChance, Settings settings) {
        super(leafParticleChance, settings);
    }

    public RubberLeavesBlock(Settings settings) {
        this(0.01f, settings);
    }

    @Override
    public MapCodec<? extends LeavesBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void spawnLeafParticle(World world, BlockPos pos, Random random) {
        // Default subtle leaves particle behavior
    }
}
