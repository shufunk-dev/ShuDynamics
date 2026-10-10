package net.enchantedwood.item.custom;

import net.enchantedwood.event.PlayerEquipmentState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EnchantedCapeItem extends Item {
    public EnchantedCapeItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!world.isClientSide() && user instanceof ServerPlayer serverPlayer) {
            ItemStack singleCape = stack.copy();
            singleCape.setCount(1);

            ItemStack previousCape = PlayerEquipmentState.equipCape(serverPlayer, singleCape);

            if (!previousCape.isEmpty()) {
                if (!user.getInventory().add(previousCape)) {
                    user.drop(previousCape, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }

            stack.shrink(1);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 1.0f);

            user.sendOverlayMessage(Component.literal("§aEquipped Enchanted Cape to Back Slot!"));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }
}
