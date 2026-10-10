package net.enchantedwood.item.custom;

import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.MiningPortalBlock;
import net.enchantedwood.block.custom.MiningPortalFrameValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantedEmeraldItem extends Item {
    public EnchantedEmeraldItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clickedState = world.getBlockState(pos);

        if (!clickedState.is(ModBlocks.ENCHANTED_COBBLESTONE)) {
            return InteractionResult.PASS;
        }

        MiningPortalFrameValidator.FrameResult result =
                MiningPortalFrameValidator.tryFindFrame(world, pos, context.getClickedFace());

        if (result.valid) {
            if (!world.isClientSide()) {
                for (BlockPos interiorPos : result.interiorPositions) {
                    world.setBlock(interiorPos,
                            ModBlocks.MINING_PORTAL.defaultBlockState().setValue(MiningPortalBlock.AXIS, result.axis),
                            3);
                }
            }

            world.playSound(context.getPlayer(), pos,
                    SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0f, 1.2f);
            world.playSound(context.getPlayer(), pos,
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.0f);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
