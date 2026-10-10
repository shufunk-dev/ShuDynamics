package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ThermalAnchorBlock extends Block {


    public ThermalAnchorBlock(Properties settings) {
        super(settings);
    }


    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandItem();

            if (AtmosphericAnchorBlock.isEnchantedPickaxe(tool)) {
                popResource(world, pos, new ItemStack(this));
            } else {
                player.sendOverlayMessage(Component.literal("§c⚠ Thermal Keystone destabilized! An enchanted pickaxe is required to harvest it."));
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(15) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.03, 0.0);
            if (random.nextInt(3) == 0) {
                world.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
            }
            if (random.nextInt(30) == 0) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.3f, 1.2f);
            }
        }
    }
}
