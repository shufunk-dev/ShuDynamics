package net.enchantedwood.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class BowlFoodItem extends TooltipItem {
    public BowlFoodItem(Properties settings, Component... tooltip) {
        super(settings.craftRemainder(Items.BOWL).stacksTo(16), tooltip);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, world, user);
        if (user instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (result.isEmpty()) {
                return bowl;
            }
            if (!player.getInventory().add(bowl)) {
                player.drop(bowl, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        return result;
    }
}
