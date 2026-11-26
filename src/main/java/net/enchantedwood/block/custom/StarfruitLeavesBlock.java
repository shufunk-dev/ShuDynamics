package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.enchantedwood.item.ModItems;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class StarfruitLeavesBlock extends FruitLeavesBlock {
    public static final MapCodec<StarfruitLeavesBlock> CODEC = createCodec(settings -> new StarfruitLeavesBlock(0.01f, settings));

    public StarfruitLeavesBlock(float leafParticleChance, Settings settings) {
        super(leafParticleChance, settings, () -> ModItems.STARFRUIT);
    }

    public StarfruitLeavesBlock(Settings settings) {
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
