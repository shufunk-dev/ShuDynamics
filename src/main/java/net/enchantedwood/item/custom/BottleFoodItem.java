package net.enchantedwood.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class BottleFoodItem extends TooltipItem {
    public BottleFoodItem(Settings settings, Text... tooltip) {
        super(settings.recipeRemainder(Items.GLASS_BOTTLE).maxCount(16), tooltip);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient()) {
            user.clearStatusEffects();
        }
        ItemStack result = super.finishUsing(stack, world, user);
        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (result.isEmpty()) {
                return bottle;
            }
            if (!player.getInventory().insertStack(bottle)) {
                player.dropItem(bottle, false);
            }
        }
        return result;
    }
}
