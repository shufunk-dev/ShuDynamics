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

public class DimensionalSingularityBlock extends Block {
    public static final MapCodec<DimensionalSingularityBlock> CODEC = createCodec(DimensionalSingularityBlock::new);

    public DimensionalSingularityBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.entity.LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient() && placer instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            ResonanceFrameValidator.tryActivateGateway(world, pos, serverPlayer);
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player != null && !player.isCreative()) {
            ItemStack tool = player.getMainHandStack();

            if (AtmosphericAnchorBlock.isEnchantedPickaxe(tool)) {
                dropStack(world, pos, new ItemStack(this));
            } else {
                player.sendMessage(Text.literal("§c⚠ Dimensional Singularity destabilized! An enchanted pickaxe is required to harvest it."), true);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 1.0f, 0.8f);
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(6) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            
            world.addParticleClient(ParticleTypes.REVERSE_PORTAL, x, y, z, (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
            if (random.nextInt(2) == 0) {
                world.addParticleClient(ParticleTypes.PORTAL, x, y, z, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);
            }
            if (random.nextInt(4) == 0) {
                world.addParticleClient(ParticleTypes.END_ROD, x, y, z, 0.0, 0.02, 0.0);
            }
            if (random.nextInt(25) == 0) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 0.4f, 1.8f);
            }
        }
    }
}
