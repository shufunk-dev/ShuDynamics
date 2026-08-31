package net.enchantedwood.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WoodenShearsItem extends ShearsItem {
    public WoodenShearsItem(Settings settings) {
        super(settings.component(net.minecraft.component.DataComponentTypes.TOOL, ShearsItem.createToolComponent()));
    }


    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient() && miner instanceof net.minecraft.entity.player.PlayerEntity player) {
            if (!player.isCreative() && state.getHardness(world, pos) != 0.0F) {
                stack.damage(2, miner, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }
}
