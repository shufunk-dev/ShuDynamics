package net.enchantedwood.mixin;

import net.enchantedwood.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void enforceAnomalyKeystoneCraftingLocks(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci
    ) {
        if (player == null) return;
        ItemStack result = resultSlots.getItem(0);
        if (result.isEmpty()) return;

        // 1. Keystone #2: Kinetic Anchor (Locked until ATV is constructed and driven)
        if (result.is(ModBlocks.KINETIC_ANCHOR.asItem())) {
            boolean hasUnlockedAtv = player.entityTags().contains("unlocked_kinetic_anchor")
                    || player.getInventory().contains(new ItemStack(net.enchantedwood.item.ModItems.ATV_ITEM));
            if (!hasUnlockedAtv) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                player.sendOverlayMessage(
                        Component.literal("§c🔒 Anomaly Keystone #2 is locked! Build and ride an ATV first.")
                );
            }
        }
        // 2. Keystone #1: Atmospheric Anchor (Locked until Scuba Gear ocean depth exploration)
        else if (result.is(ModBlocks.ATMOSPHERIC_ANCHOR.asItem())) {
            if (!player.entityTags().contains("unlocked_atmospheric_anchor")) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                player.sendOverlayMessage(
                        Component.literal("§c🔒 Anomaly Keystone #1 is locked! Explore ocean depths in Scuba Gear first.")
                );
            }
        }
    }
}
