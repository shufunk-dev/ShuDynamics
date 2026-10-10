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

public class DimensionalSingularityBlock extends Block {

    public DimensionalSingularityBlock(Properties settings) {
        super(settings);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide() && placer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ResonanceFrameValidator.tryActivateGateway(world, pos, serverPlayer);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandItem();

            if (AtmosphericAnchorBlock.isEnchantedPickaxe(tool)) {
                popResource(world, pos, new ItemStack(this));
            } else {
                player.sendOverlayMessage(Component.literal("§c⚠ Dimensional Singularity destabilized! An enchanted pickaxe is required to harvest it."));
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            
            world.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
            if (random.nextInt(2) == 0) {
                world.addParticle(ParticleTypes.PORTAL, x, y, z, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);
            }
            if (random.nextInt(4) == 0) {
                world.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.02, 0.0);
            }
            if (random.nextInt(25) == 0) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4f, 1.8f);
            }
        }
    }
}
