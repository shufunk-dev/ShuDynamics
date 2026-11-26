package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.item.ModItems;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class AvocadoLeavesBlock extends FruitLeavesBlock {
    public static final MapCodec<AvocadoLeavesBlock> CODEC = createCodec(settings -> new AvocadoLeavesBlock(0.01f, settings));

    public AvocadoLeavesBlock(float leafParticleChance, Settings settings) {
        super(leafParticleChance, settings, () -> ModItems.AVOCADO);
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
    }
}
