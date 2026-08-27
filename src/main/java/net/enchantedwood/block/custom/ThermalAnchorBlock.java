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

public class ThermalAnchorBlock extends Block {
    public static final MapCodec<ThermalAnchorBlock> CODEC = createCodec(ThermalAnchorBlock::new);

    public ThermalAnchorBlock(Settings settings) {
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
                player.sendMessage(Text.literal("§c⚠ Thermal Keystone destabilized! An enchanted pickaxe is required to harvest it."), true);
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
            world.addParticleClient(ParticleTypes.FLAME, x, y, z, 0.0, 0.03, 0.0);
            if (random.nextInt(3) == 0) {
                world.addParticleClient(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
            }
            if (random.nextInt(30) == 0) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 0.3f, 1.2f);
            }
        }
    }
}
