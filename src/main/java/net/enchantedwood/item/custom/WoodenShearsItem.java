package net.enchantedwood.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WoodenShearsItem extends ShearsItem {
    public WoodenShearsItem(Properties settings) {
        super(settings.component(net.minecraft.core.component.DataComponents.TOOL, ShearsItem.createToolProperties()));
    }


    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide() && miner instanceof net.minecraft.world.entity.player.Player player) {
            if (!player.isCreative() && state.getDestroySpeed(world, pos) != 0.0F) {
                stack.hurtAndBreak(2, miner, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }
}
