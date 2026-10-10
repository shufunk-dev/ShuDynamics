package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.enchantedwood.item.ModItems;

public class WoodenShearsSheepHandler {
    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof Sheep sheep) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.is(ModItems.WOODEN_SHEARS)) {
                    if (sheep.readyForShearing()) {
                        if (!world.isClientSide()) {
                            sheep.setSheared(true);
                            int woolCount = 1 + world.getRandom().nextInt(2); // Drops 1-2 wool
                            Item woolItem = getWoolItem(sheep.getColor());
                            for (int i = 0; i < woolCount; i++) {
                                ItemEntity itemEntity = sheep.spawnAtLocation((net.minecraft.server.level.ServerLevel) world, new ItemStack(woolItem));
                                if (itemEntity != null) {
                                    itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add(
                                            (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.1F,
                                            world.getRandom().nextFloat() * 0.05F,
                                            (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.1F
                                    ));
                                }
                            }
                            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                            stack.hurtAndBreak(2, player, slot);
                            world.playSound(null, sheep.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }

    private static Item getWoolItem(net.minecraft.world.item.DyeColor color) {
        return net.minecraft.world.item.Items.WOOL.pick(color);
    }
}
