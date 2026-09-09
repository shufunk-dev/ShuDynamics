package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class AvocadoLeavesBlock extends LeavesBlock {
    public static final MapCodec<AvocadoLeavesBlock> CODEC = createCodec(settings -> new AvocadoLeavesBlock(0.01f, settings));

    public AvocadoLeavesBlock(float leafParticleChance, Settings settings) {
        super(leafParticleChance, settings);
    }

    public AvocadoLeavesBlock(Settings settings) {
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
