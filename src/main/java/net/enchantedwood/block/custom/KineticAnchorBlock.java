package net.enchantedwood.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class KineticAnchorBlock extends Block {
    public static final MapCodec<KineticAnchorBlock> CODEC = createCodec(KineticAnchorBlock::new);

    public KineticAnchorBlock(Settings settings) {
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
            boolean isTool = tool.isIn(ItemTags.PICKAXES) || tool.isIn(ItemTags.AXES) || tool.isIn(ItemTags.HOES) || tool.isIn(ItemTags.SHOVELS);
            boolean isEnchanted = tool.hasEnchantments()
                    || (tool.get(DataComponentTypes.ENCHANTMENTS) != null && !tool.get(DataComponentTypes.ENCHANTMENTS).isEmpty())
                    || (tool.get(DataComponentTypes.STORED_ENCHANTMENTS) != null && !tool.get(DataComponentTypes.STORED_ENCHANTMENTS).isEmpty());

            if (isTool && isEnchanted) {
                dropStack(world, pos, new ItemStack(this));
            } else {
                player.sendMessage(Text.literal("§c⚠ Anomaly Keystone destabilized! An enchanted pickaxe or axe is required to harvest it."), true);
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

            world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, x, y, z, (random.nextDouble() - 0.5) * 0.05, 0.05, (random.nextDouble() - 0.5) * 0.05);
            world.addParticleClient(ParticleTypes.SMOKE, x, y, z, 0.0, 0.02, 0.0);

            if (random.nextInt(35) == 0) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_AMBIENT, SoundCategory.BLOCKS, 0.3f, 1.6f);
            }
        }
    }
}
