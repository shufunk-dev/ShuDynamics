package net.enchantedwood.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class BowlFoodItem extends TooltipItem {
    public BowlFoodItem(Settings settings, Text... tooltip) {
        super(settings.recipeRemainder(Items.BOWL).maxCount(16), tooltip);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (result.isEmpty()) {
                return bowl;
            }
            if (!player.getInventory().insertStack(bowl)) {
                player.dropItem(bowl, false);
            }
        }
        return result;
    }
}
