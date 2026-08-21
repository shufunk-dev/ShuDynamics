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

public class EnchantedHeartItem extends Item {
    private final float absorptionAmount;

    public EnchantedHeartItem(float absorptionAmount, Settings settings) {
        super(settings);
        this.absorptionAmount = absorptionAmount;
    }

    public float getAbsorptionAmount() {
        return absorptionAmount;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            ItemStack singleHeart = stack.copy();
            singleHeart.setCount(1);

            ItemStack previousHeart = PlayerEquipmentState.equipHeart(serverPlayer, singleHeart);

            // Give previous equipped heart back to inventory or drop
            if (!previousHeart.isEmpty()) {
                if (!user.getInventory().insertStack(previousHeart)) {
                    user.dropItem(previousHeart, false);
                }
            }

            stack.decrement(1);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_ARMOR_EQUIP_GOLD, SoundCategory.PLAYERS, 1.0f, 1.2f);

            user.sendMessage(Text.literal("§eEquipped " + this.getName().getString() + " to Heart Container Slot!"), true);
            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS;
    }
}
