package net.enchantedwood.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class BottleFoodItem extends TooltipItem {
    public BottleFoodItem(Properties settings, Component... tooltip) {
        super(settings.craftRemainder(Items.GLASS_BOTTLE).stacksTo(16), tooltip);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        if (!world.isClientSide()) {
            user.removeAllEffects();
        }
        ItemStack result = super.finishUsingItem(stack, world, user);
        if (user instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (result.isEmpty()) {
                return bottle;
            }
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        return result;
    }
}
