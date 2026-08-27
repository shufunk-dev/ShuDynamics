package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class MetallurgicalAnchorBlock extends Block {
    public static final MapCodec<MetallurgicalAnchorBlock> CODEC = createCodec(MetallurgicalAnchorBlock::new);

    public MetallurgicalAnchorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandStack();

            if (AtmosphericAnchorBlock.isEnchantedPickaxe(tool)) {
                dropStack(world, pos, new ItemStack(this));
            } else {
                player.sendMessage(Text.literal("§c⚠ Metallurgical Keystone destabilized! An enchanted pickaxe is required to harvest it."), true);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 1.0f, 0.8f);
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(15) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            world.addParticleClient(ParticleTypes.PORTAL, x, y, z, (random.nextDouble() - 0.5) * 0.2, 0.1, (random.nextDouble() - 0.5) * 0.2);
            if (random.nextInt(3) == 0) {
                world.addParticleClient(ParticleTypes.ENCHANT, x, y, z, (random.nextDouble() - 0.5) * 0.2, 0.2, (random.nextDouble() - 0.5) * 0.2);
            }
            if (random.nextInt(35) == 0) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.4f, 0.9f);
            }
        }
    }
}
