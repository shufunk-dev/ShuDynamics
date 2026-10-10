package net.enchantedwood.item.custom;

import net.enchantedwood.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

public class HydrogenCanisterItem extends Item {
    public HydrogenCanisterItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack canister = user.getItemInHand(hand);
        ItemStack chest = user.getItemBySlot(EquipmentSlot.CHEST);

        // Refuel equipped Jetpack on right-click
        if (chest.is(ModItems.HYDROGEN_JETPACK)) {
            int current = HydrogenJetpackItem.getHydrogen(chest);
            if (current < HydrogenJetpackItem.MAX_HYDROGEN) {
                if (!world.isClientSide()) {
                    int next = Math.min(current + 1000, HydrogenJetpackItem.MAX_HYDROGEN);
                    HydrogenJetpackItem.setHydrogen(chest, next);
                    canister.shrink(1);
                    user.getInventory().placeItemBackInInventory(new ItemStack(ModItems.EMPTY_GAS_CANISTER), net.minecraft.util.Prediction.SERVER_ONLY);
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 1.2f);
                    user.sendOverlayMessage(Component.literal(String.format("§6⚡ Refueled equipped Jetpack (+1,000 mB) [%,d / %,d mB]", next, HydrogenJetpackItem.MAX_HYDROGEN)));
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!world.isClientSide()) {
                    user.sendOverlayMessage(Component.literal("§a✔ Equipped Jetpack is already fully fueled!"));
                }
                return InteractionResult.CONSUME;
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.literal("§bContains: §f1,000 mB Compressed Hydrogen Gas"));
        textConsumer.accept(Component.literal("§7Industrial fuel for Jetpacks & Steel Blast Furnaces."));
        textConsumer.accept(Component.literal("§eRight-Click: §7Directly refuels equipped Jetpack."));
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}
