package net.enchantedwood.item.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.enchantedwood.event.PlayerEquipmentState;

public class EnchantedCapeItem extends Item {
    public EnchantedCapeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            ItemStack singleCape = stack.copy();
            singleCape.setCount(1);

            ItemStack previousCape = PlayerEquipmentState.equipCape(serverPlayer, singleCape);

            if (!previousCape.isEmpty()) {
                if (!user.getInventory().insertStack(previousCape)) {
                    user.dropItem(previousCape, false);
                }
            }

            stack.decrement(1);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 1.0f, 1.0f);

            user.sendMessage(Text.literal("§aEquipped Enchanted Cape to Back Slot!"), true);
            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS;
    }
}
