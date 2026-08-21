package net.enchantedwood.event;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.enchantedwood.item.ModItems;

public class WoodenShearsSheepHandler {
    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof SheepEntity sheep) {
                ItemStack stack = player.getStackInHand(hand);
                if (stack.isOf(ModItems.WOODEN_SHEARS)) {
                    if (sheep.isShearable()) {
                        if (!world.isClient()) {
                            sheep.setSheared(true);
                            int woolCount = 1 + world.getRandom().nextInt(2); // Drops 1-2 wool
                            Item woolItem = getWoolItem(sheep.getColor());
                            for (int i = 0; i < woolCount; i++) {
                                ItemEntity itemEntity = sheep.dropStack((net.minecraft.server.world.ServerWorld) world, new ItemStack(woolItem));
                                if (itemEntity != null) {
                                    itemEntity.setVelocity(itemEntity.getVelocity().add(
                                            (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.1F,
                                            world.getRandom().nextFloat() * 0.05F,
                                            (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.1F
                                    ));
                                }
                            }
                            EquipmentSlot slot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                            stack.damage(2, player, slot);
                            world.playSound(null, sheep.getBlockPos(), SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 1.0F, 1.0F);
                        }
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private static Item getWoolItem(net.minecraft.util.DyeColor color) {
        return switch (color) {
            case WHITE -> net.minecraft.item.Items.WHITE_WOOL;
            case ORANGE -> net.minecraft.item.Items.ORANGE_WOOL;
            case MAGENTA -> net.minecraft.item.Items.MAGENTA_WOOL;
            case LIGHT_BLUE -> net.minecraft.item.Items.LIGHT_BLUE_WOOL;
            case YELLOW -> net.minecraft.item.Items.YELLOW_WOOL;
            case LIME -> net.minecraft.item.Items.LIME_WOOL;
            case PINK -> net.minecraft.item.Items.PINK_WOOL;
            case GRAY -> net.minecraft.item.Items.GRAY_WOOL;
            case LIGHT_GRAY -> net.minecraft.item.Items.LIGHT_GRAY_WOOL;
            case CYAN -> net.minecraft.item.Items.CYAN_WOOL;
            case PURPLE -> net.minecraft.item.Items.PURPLE_WOOL;
            case BLUE -> net.minecraft.item.Items.BLUE_WOOL;
            case BROWN -> net.minecraft.item.Items.BROWN_WOOL;
            case GREEN -> net.minecraft.item.Items.GREEN_WOOL;
            case RED -> net.minecraft.item.Items.RED_WOOL;
            case BLACK -> net.minecraft.item.Items.BLACK_WOOL;
        };
    }
}
