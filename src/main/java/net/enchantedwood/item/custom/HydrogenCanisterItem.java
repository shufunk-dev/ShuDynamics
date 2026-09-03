package net.enchantedwood.item.custom;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.enchantedwood.item.ModItems;

import java.util.function.Consumer;

public class HydrogenCanisterItem extends Item {
    public HydrogenCanisterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack canister = user.getStackInHand(hand);
        ItemStack chest = user.getEquippedStack(EquipmentSlot.CHEST);

        // Refuel equipped Jetpack on right-click
        if (chest.isOf(ModItems.HYDROGEN_JETPACK)) {
            int current = HydrogenJetpackItem.getHydrogen(chest);
            if (current < HydrogenJetpackItem.MAX_HYDROGEN) {
                if (!world.isClient()) {
                    int next = Math.min(current + 1000, HydrogenJetpackItem.MAX_HYDROGEN);
                    HydrogenJetpackItem.setHydrogen(chest, next);
                    canister.decrement(1);
                    user.getInventory().offerOrDrop(new ItemStack(ModItems.EMPTY_GAS_CANISTER));
                    world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.2f);
                    user.sendMessage(Text.literal(String.format("§6⚡ Refueled equipped Jetpack (+1,000 mB) [%,d / %,d mB]", next, HydrogenJetpackItem.MAX_HYDROGEN)), true);
                }
                return ActionResult.SUCCESS;
            } else {
                if (!world.isClient()) {
                    user.sendMessage(Text.literal("§a✔ Equipped Jetpack is already fully fueled!"), true);
                }
                return ActionResult.CONSUME;
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("§bContains: §f1,000 mB Compressed Hydrogen Gas"));
        textConsumer.accept(Text.literal("§7Industrial fuel for Jetpacks & Steel Blast Furnaces."));
        textConsumer.accept(Text.literal("§eRight-Click: §7Directly refuels equipped Jetpack."));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}
