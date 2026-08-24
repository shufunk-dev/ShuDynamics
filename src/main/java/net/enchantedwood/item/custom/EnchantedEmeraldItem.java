package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.enchantedwood.block.ModBlocks;
import net.enchantedwood.block.custom.MiningPortalBlock;
import net.enchantedwood.block.custom.MiningPortalFrameValidator;

public class EnchantedEmeraldItem extends Item {
    public EnchantedEmeraldItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState clickedState = world.getBlockState(pos);

        if (!clickedState.isOf(ModBlocks.ENCHANTED_COBBLESTONE)) {
            return ActionResult.PASS;
        }

        MiningPortalFrameValidator.FrameResult result =
                MiningPortalFrameValidator.tryFindFrame(world, pos, context.getSide());

        if (result.valid) {
            if (!world.isClient()) {
                for (BlockPos interiorPos : result.interiorPositions) {
                    world.setBlockState(interiorPos,
                            ModBlocks.MINING_PORTAL.getDefaultState().with(MiningPortalBlock.AXIS, result.axis),
                            3);
                }
            }

            world.playSound(context.getPlayer(), pos,
                    SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 1.0f, 1.2f);
            world.playSound(context.getPlayer(), pos,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.0f);

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
