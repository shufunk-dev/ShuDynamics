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

public class EnchantedHeartItem extends Item {
    private final float absorptionAmount;

    public EnchantedHeartItem(float absorptionAmount, Properties settings) {
        super(settings);
        this.absorptionAmount = absorptionAmount;
    }

    public float getAbsorptionAmount() {
        return absorptionAmount;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!world.isClientSide() && user instanceof ServerPlayer serverPlayer) {
            ItemStack singleHeart = stack.copy();
            singleHeart.setCount(1);

            ItemStack previousHeart = PlayerEquipmentState.equipHeart(serverPlayer, singleHeart);

            // Give previous equipped heart back to inventory or drop
            if (!previousHeart.isEmpty()) {
                if (!user.getInventory().add(previousHeart)) {
                    user.drop(previousHeart, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }

            stack.shrink(1);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0f, 1.2f);

            user.sendOverlayMessage(Component.literal("§eEquipped " + this.getName(this.getDefaultInstance()).getString() + " to Heart Container Slot!"));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }
}
