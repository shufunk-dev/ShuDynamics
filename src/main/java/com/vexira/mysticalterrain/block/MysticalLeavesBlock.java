package com.vexira.mysticalterrain.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class MysticalLeavesBlock extends LeavesBlock {
    public static final MapCodec<MysticalLeavesBlock> CODEC = createCodec(settings -> new MysticalLeavesBlock(0.01f, settings));

    public MysticalLeavesBlock(float leafParticleChance, Settings settings) {
        super(leafParticleChance, settings);
    }

    public MysticalLeavesBlock(Settings settings) {
        this(0.01f, settings);
    }

    @Override
    public MapCodec<? extends LeavesBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void spawnLeafParticle(World world, BlockPos pos, Random random) {
    }
}
